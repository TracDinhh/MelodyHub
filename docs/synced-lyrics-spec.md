# Spec: Synced Lyrics (Lyrics Chạy Theo Nhạc)

## 1. Tổng Quan

Cho phép artist đăng bài hát kèm **synced lyrics** - lyrics tự động highlight dòng đang hát theo thời gian phát nhạc.

---

## 2. Data Model

### 2.1 Database Schema

**Option A: Mở rộng bảng `songs`**

```sql
-- Thêm cột lyrics_type vào bảng songs
ALTER TABLE songs ADD COLUMN lyrics_type ENUM('PLAIN', 'SYNCED') DEFAULT 'PLAIN';
ALTER TABLE songs MODIFY COLUMN lyrics LONGTEXT; -- hỗ trợ cả plain và JSON
```

**Option B: Tabel riêng `song_lyrics` (đề xuất - có sẵn trong schema)**

```sql
-- Bảng song_lyrics đã có trong schema
-- Cấu trúc:
-- id, song_id, content (JSON synced lyrics), type ('PLAIN' | 'SYNCED'), 
-- language_code, is_primary, created_at, updated_at
```

### 2.2 Lyrics Format

```json
{
  "lines": [
    { "startTime": 0.0, "endTime": 3.5, "text": "Verse 1 line 1" },
    { "startTime": 3.5, "endTime": 7.2, "text": "Verse 1 line 2" },
    { "startTime": 7.2, "endTime": 11.0, "text": "Chorus line 1" }
  ],
  "language": "en"
}
```

---

## 3. Artist Workflow

### 3.1 Upload Song Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ARTIST UPLOAD SONG                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  Fill Song Info  │
                    │  (title, audio,  │
                    │   cover, etc.)   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Add Lyrics?   │
                    │   (optional)    │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                              │
              ▼                              ▼
    ┌─────────────────┐           ┌─────────────────┐
    │  PLAIN LYRICS   │           │  SYNCED LYRICS  │
    │  (textarea)     │           │  (lyrics editor) │
    └─────────────────┘           └────────┬────────┘
                                           │
                                           ▼
                                 ┌─────────────────┐
                                 │  Input Lines +   │
                                 │  Set Timestamps  │
                                 └────────┬────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │  Preview Sync   │
                                 │  ▶️ Play Song   │
                                 └────────┬────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │    Save Song    │
                                 └────────┬────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │  Send to API    │
                                 │  (lyrics as     │
                                 │   JSON string)  │
                                 └─────────────────┘
```

### 3.2 Lyrics Editor Interface

```
┌────────────────────────────────────────────────────────────────┐
│  LYRICS EDITOR                                    [Preview ▶️] │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Line 1:  [0:00.0] [0:04.5] ________________________    │  │
│  │  Line 2:  [0:04.5] [0:08.2] ________________________    │  │
│  │  Line 3:  [0:08.2] [0:12.0] ________________________    │  │
│  │  Line 4:  [0:12.0] [0:15.5] ________________________    │  │
│  │  ...                                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [+ Add Line]  [🗑️ Remove Selected]  [Auto-timestamp]         │
│                                                                │
│  ▶️ Current: Line 2 at 0:04.5                                  │
└────────────────────────────────────────────────────────────────┘
```

### 3.3 Timestamp Input Methods

1. **Manual Input**: Artist nhập thời gian bằng tay (MM:SS.ms)
2. **Tap to Set**: Click nút khi đang play nhạc → tự động capture thời gian hiện tại
3. **Auto Lyrics Lookup (LRCLIB)**: Tự tìm synced lyrics từ LRCLIB và fill vào editor

### 3.4 Auto Lyrics Lookup (LRCLIB Integration)

**LRCLIB does NOT generate lyrics.** It retrieves lyrics that already exist in its database.

#### Flow

```
Artist nhập thông tin bài hát (title, artist, duration)
        │
        ▼
Click "✨ Find Lyrics Automatically"
        │
        ▼
Frontend → GET /api/artist/lyrics/search
        │
        ▼
MelodyHub Backend → LyricsLookupService
        │
        ▼
LrclibLyricsProvider
        │
        ├── GET /api/get (exact match)
        │   └── 404? → fallback to search
        │
        └── GET /api/search (broader)
        │
        ▼
LrcParser (parse LRC → MelodyHub format)
        │
        ▼
MatchScorer (score candidates)
        │
        ▼
LyricsLookupResponse (normalized DTO)
        │
        ▼
Frontend receives candidates
        │
        ├── Single high-confidence → auto-select
        └── Multiple → artist chọn
        │
        ▼
Artist clicks "Use selected lyrics"
        │
        ▼
LyricsEditor populated (SYNCED or PLAIN)
        │
        ▼
Artist review/edit → Save → existing ArtistSongService → song_lyrics
```

#### Architecture

```
backend/src/main/java/com/melodyHub/lyrics/
├── LyricsProvider.java           # Interface cho providers
├── LyricsProviderException.java  # Exception type
├── LyricsSearchResult.java       # Raw result from provider
├── LyricsLookupService.java      # Orchestration + validation
├── LyricsLookupResponse.java     # Normalized DTO for frontend
├── LrcParser.java                # LRC format → structured lines
├── MatchScorer.java              # Candidate scoring
└── provider/
    └── LrclibLyricsProvider.java # LRCLIB API implementation
```

#### Scoring

| Criterion            | Points |
|----------------------|--------|
| Title exact match    | 40     |
| Artist exact match   | 30     |
| Album exact match    | 10     |
| Duration ≤ 2 sec     | 20     |
| Duration ≤ 5 sec     | 15     |
| Duration ≤ 10 sec    | 5      |
| Duration > 10 sec    | 0      |

Score ≥ 90 → auto-select. Score < 30 → filtered out.

#### Fallback Priority

1. Synced lyrics (LRC format → parsed timestamps)
2. Plain lyrics (text only)
3. Manual editor (no auto-fill)

#### Limitations

- LRCLIB may not contain newly released or original songs
- AI transcription is not implemented
- Word-by-word synchronization is not implemented
- Only retrieves existing lyrics, does not generate them

---

## 4. API Design

### 4.1 Create Song with Lyrics

**POST `/api/artist/songs`**

```json
// Request
{
  "title": "My Song",
  "audioUrl": "https://...",
  "coverUrl": "https://...",
  "durationSec": 210,
  "lyrics": "[{\"startTime\":0,\"endTime\":3.5,\"text\":\"Line 1\"},...]",
  "lyricsType": "SYNCED"
}
```

### 4.2 Get Song with Lyrics

**GET `/api/songs/{slug}`**

```json
// Response
{
  "id": 1,
  "title": "My Song",
  "slug": "my-song",
  "audioUrl": "https://...",
  "coverUrl": "https://...",
  "durationSec": 210,
  "lyrics": "[{\"startTime\":0,\"endTime\":3.5,\"text\":\"Line 1\"},...]",
  "lyricsType": "SYNCED"
}
```

---

## 5. Frontend Player Integration

### 5.1 Display Logic

```
┌─────────────────────────────────────────────────────────────┐
│                      NOW PLAYING                            │
│                    🎵 My Song - Artist                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    [Cover Art / Visualizer]                 │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│              ┌─────────────────────────────┐                │
│              │                             │                │
│              │    [ Lyrics Display ]       │                │
│              │                             │                │
│              │   ───────────────────────   │                │
│              │   Line 1 (already sung)     │  ← faded       │
│              │   Line 2 (current)         │  ← highlighted  │
│              │   Line 3 (next)            │  ← normal       │
│              │   Line 4 (upcoming)        │  ← faded        │
│              │                             │                │
│              └─────────────────────────────┘                │
│                                                             │
│  ▶️ ──●──────────────────── 2:34 / 3:45                    │
│  🔊 ────────●                                                     │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Sync Algorithm

```javascript
function getCurrentLyricIndex(currentTime, lyrics) {
  for (let i = lyrics.length - 1; i >= 0; i--) {
    if (currentTime >= lyrics[i].startTime) {
      return i;
    }
  }
  return -1; // No match yet
}
```

### 5.3 Visual States

| State | Style |
|-------|-------|
| Past (đã hát) | opacity: 0.3, color: gray |
| Current (đang hát) | opacity: 1, color: white, scale: 1.05, glow effect |
| Future (chưa hát) | opacity: 0.6, color: light gray |

### 5.4 Auto-scroll

- Active line luôn ở giữa view
- Smooth scroll animation khi chuyển line
- User có thể scroll tự do, auto-scroll resume khi click vào lyrics area

---

## 6. Implementation Phases

### Phase 1: Core (MVP)
- [ ] Backend: Thêm `lyrics_type` vào song creation
- [ ] Frontend: Lyrics textarea cho plain lyrics (giữ nguyên)
- [ ] Player: Hiển thị plain lyrics khi không có synced

### Phase 2: Synced Lyrics Editor
- [ ] Frontend: Lyrics editor với timestamp input
- [ ] Frontend: Preview synced lyrics
- [ ] Backend: Validate & store synced lyrics JSON

### Phase 3: Player Enhancement
- [ ] Frontend: Synced lyrics display với highlight
- [ ] Frontend: Auto-scroll theo thời gian
- [ ] Frontend: Toggle plain/synced mode

### Phase 4: Polish
- [ ] Animation effects cho lyric transitions
- [ ] Karaoke-style highlight (word by word)
- [ ] Mobile responsive lyrics view

---

## 7. Questions for Decision

1. **Lyrics editor UX**: 
   - Mỗi dòng 1 input timestamp riêng?
   - Hay dùng "tap while playing" mode?

2. **Backward compatibility**:
   - Songs đã có plain lyrics → giữ nguyên?
   - Hoặc convert sang synced format (null timestamps)?

3. **Multiple languages**:
   - 1 bài hát có nhiều lyrics versions (English, Vietnamese)?
   - Artist chọn primary version?

---

## 8. File Structure

```
frontend/src/
├── views/
│   └── artist/
│       ├── ArtistSongUploadView.vue    # Thêm lyrics editor
│       └── components/
│           └── LyricsEditor.vue        # Synced lyrics editor component
├── components/
│   └── player/
│       ├── PlayerBar.vue               # Mở rộng để show lyrics
│       └── LyricsDisplay.vue           # Synced lyrics component
└── services/
    └── songService.js                  # Cập nhật create/update

backend/src/main/java/com/melodyHub/
├── dto/request/
│   └── SongCreateRequest.java          # Thêm lyricsType
├── entity/
│   └── Song.java                       # Thêm lyricsType enum
├── service/artist/
│   └── ArtistSongService.java          # Xử lý lyrics validation
└── repository/
    └── SongRepository.java             # Query với lyrics
```

---

## 9. Sample JSON

### Synced Lyrics Example (English Song)

```json
{
  "lines": [
    { "startTime": 0.0, "endTime": 4.5, "text": "🎵 Instrumental intro 🎵" },
    { "startTime": 4.5, "endTime": 8.2, "text": "Verse 1:" },
    { "startTime": 8.2, "endTime": 12.0, "text": "Walking down the street today" },
    { "startTime": 12.0, "endTime": 15.5, "text": "Sun is shining all the way" },
    { "startTime": 15.5, "endTime": 19.0, "text": "Got my headphones on my ears" },
    { "startTime": 19.0, "endTime": 23.0, "text": "Dancing through my fears" },
    { "startTime": 23.0, "endTime": 26.5, "text": "Chorus:" },
    { "startTime": 26.5, "endTime": 32.0, "text": "This is my kind of day" },
    { "startTime": 32.0, "endTime": 37.5, "text": "Everything's gonna be okay" }
  ],
  "language": "en"
}
```

### Plain Lyrics (Fallback)

```json
{
  "text": "Walking down the street today\nSun is shining all the way\nGot my headphones on my ears\nDancing through my fears",
  "language": "en"
}
```
