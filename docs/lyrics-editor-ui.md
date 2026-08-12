# Lyrics Editor - Chi Tiết Cách Nhập Timestamps

## 3 Cách Nhập Timestamp

### Cách 1: Tap While Playing (Đề xuất - Dễ nhất)

```
┌────────────────────────────────────────────────────────────────┐
│  LYRICS EDITOR                                                 │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ▶️ 00:15.3 / 03:45          [🔊 Volume]                      │
│  ═══════════════════●═══════════════════════════════════        │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  [ 0:00.0 ]  🎵 Instrumental intro 🎵                    │  │
│  │                                                          │  │
│  │  [ 0:04.5 ]  Walking down the street today    ← CURRENT  │  │
│  │                                                          │  │
│  │  [ 0:08.2 ]  Sun is shining all the way                  │  │
│  │                                                          │  │
│  │  [ 0:12.0 ]  Got my headphones on my ears                │  │
│  │                                                          │  │
│  │  [ 0:15.5 ]  Dancing through my fears                    │  │
│  │                                                          │  │
│  │  [ 0:19.0 ]  ________________________________            │  │
│  │                   ↑ Chưa nhập                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [🔴 START]  ← Nhấn để bắt đầu phát nhạc + capture time      │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Workflow:**
1. Artist gõ lyrics vào các dòng
2. Click **[🔴 START]** → nhạc bắt đầu play
3. Khi muốn set thời gian cho dòng nào → **click vào dòng đó**
4. Thời gian hiện tại của nhạc → tự động fill vào ô timestamp
5. Tiếp tục click các dòng tiếp theo khi nhạc chạy

---

### Cách 2: Manual Input - Nhập Tay

```
┌────────────────────────────────────────────────────────────────┐
│  LYRICS EDITOR - MANUAL MODE                                   │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  Start Time  │  End Time  │  Lyrics Text                │  │
│  │  ─────────────────────────────────────────────────────   │  │
│  │  00:00.0     │  00:04.5   │  🎵 Instrumental intro 🎵   │  │
│  │  00:04.5     │  00:08.2   │  Walking down the street    │  │
│  │  00:08.2     │  00:12.0   │  Sun is shining all the way  │  │
│  │  00:12.0     │  00:15.5   │  Got my headphones on ears  │  │
│  │  + Add Line                                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  💡 Tip: Dùng format MM:SS.ss (ví dụ: 01:23.45)              │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Workflow:**
1. Artist nhập thủ công từng dòng
2. Fill `Start Time`, `End Time`, `Lyrics Text`
3. Click `+ Add Line` để thêm dòng mới

---

### Cách 3: Import Lyrics + Auto-Sync (Nâng cao)

```
┌────────────────────────────────────────────────────────────────┐
│  IMPORT LYRICS                                                 │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Paste your plain lyrics here:                          │  │
│  │                                                          │  │
│  │  Walking down the street today                          │  │
│  │  Sun is shining all the way                             │  │
│  │  Got my headphones on my ears                           │  │
│  │  Dancing through my fears                              │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [📋 IMPORT & AUTO-ARRANGE]                                    │
│                                                                │
│  ↓ After Import ↓                                             │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  System sẽ tự chia lyrics thành các dòng                │  │
│  │  Artist chỉ cần set timestamps cho từng dòng            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## UI Component Design

### LyricsLineInput Component

```
┌─────────────────────────────────────────────────────────────────┐
│  ☐  [00:04.5] [00:08.2]  Walking down the street today    🗑️  │
└─────────────────────────────────────────────────────────────────┘
  │        │            │              │                        │
  │        │            │              └── Text input (lyrics)    │
  │        │            └── End time input (MM:SS.ss)           │
  │        └── Start time input (MM:SS.ss)                      │
  └── Checkbox để select/delete nhiều dòng
```

### Interactions:

| Action | Kết quả |
|--------|---------|
| Click vào dòng khi đang play | Fill timestamp = current time |
| Double-click timestamp | Edit thủ công |
| Drag dòng | Reorder lines |
| Checkbox + Delete | Xóa dòng đã chọn |
| Tab key | Di chuyển sang ô tiếp theo |

---

## Auto-continue Mode

```
┌────────────────────────────────────────────────────────────────┐
│  MODE: [●] Auto-continue    [ ] Manual                        │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Khi Auto-continue ON:                                        │
│  - Sau khi fill timestamp cho dòng 1 → con trỏ tự nhảy       │
│    xuống dòng 2 và auto-fill startTime = endTime của dòng 1  │
│                                                                │
│  Ví dụ:                                                       │
│  Dòng 1: [00:04.5] [00:08.2] "Hello"  ← Artist set          │
│                                    ↓                          │
│  Dòng 2: [00:08.2] [__:__] "World"   ← Tự động fill start   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Preview Mode

```
┌────────────────────────────────────────────────────────────────┐
│  LYRICS PREVIEW                                    [✏️ Edit]   │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ▶️ 01:23.4 / 03:45          [🔊 Volume]  [🔄 Loop]           │
│  ════════════════●══════════════════════════════════════       │
│                                                                │
│         ┌────────────────────────────────────────┐              │
│         │                                        │              │
│         │    🎵 Instrumental intro 🎵            │              │
│         │                                        │              │
│         │    Walking down the street today       │ ← faded      │
│         │                                        │              │
│         │    ████ Sun is shining all the way ████│ ← glowing    │
│         │                                        │              │
│         │    Got my headphones on my ears        │ ← normal     │
│         │                                        │              │
│         │    Dancing through my fears            │ ← faded      │
│         │                                        │              │
│         └────────────────────────────────────────┘              │
│                                                                │
│  [◀️ Prev Line]  [🔄 Restart]  [Next Line ▶️]                   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Summary: Artist Workflow

```
1. Artist nhập lyrics text (mỗi dòng = 1 line)
            ↓
2. Artist click [▶️ PLAY] để nghe nhạc
            ↓
3. Khi đến đoạn cần set time:
   - Click vào dòng lyrics tương ứng
   - Start time tự động capture
            ↓
4. Tiếp tục click các dòng tiếp theo
   (hoặc dùng keyboard shortcut)
            ↓
5. [💾 Save] → Lyrics được lưu với timestamps
            ↓
6. Preview để xem kết quả
```
