# Phase 1 — Song Detail Page

## Goal

Khi user click vào một bài hát (từ Home, Artist page, hay bất kỳ đâu), hiển thị
trang chi tiết với đầy đủ thông tin: cover lớn, nghệ sĩ, album, action bar
(Play, Like, Add to playlist), lyrics, related songs.

## chi tiết với đầy đủ thông tin: cover lớn, nghệ sĩ, album, action bar

(Play, Like, Add to playlist), lyrics, related songs.

Scope

### Trong scope (Phase 1)

- Backend enrich `GET /api/songs/{slug}` trả về: artist array, album object,
likeCount, isLikedByCurrentUser, view increment.
- Backend endpoint mới `GET /api/songs/{slug}/related` trả về tối đa 8 bài
liên quan (ưu tiên cùng artist MAIN, sau đó cùng album, rồi random).
- Frontend route `/songs/:slug` + component `SongDetailView.vue`.
- Wire click từ `HomeView.vue` (new releases), `ArtistView.vue` (TrackRow),
`BottomPlayer` (mini title) sang detail page.



### Ngoài scope (Phase 2+)

- Like / Follow toggle → Phase 2.
- Add to playlist → Phase 3.
- Karaoke-style lyrics sync → để sau.
- Related algorithm phức tạp → Phase 4.



## API changes



### `GET /api/songs/{slug}` — enrich response

**Response shape (mới):**

```json
{
  "id": 1,
  "title": "Velvet Hours",
  "slug": "velvet-hours",
  "albumId": 1,
  "album": {
    "id": 1,
    "title": "Velvet Hours",
    "slug": "velvet-hours",
    "coverUrl": "https://..."
  },
  "trackNumber": 1,
  "durationSec": 238,
  "coverUrl": "https://...",
  "audioUrl": "https://...",
  "lyrics": "...",
  "status": "PUBLISHED",
  "playCount": 84291,
  "createdAt": "...",
  "updatedAt": "...",
  "artists": [
    { "id": 1, "name": "Lena Rivers", "slug": "lena-rivers", "imageUrl": "..." },
    { "id": 2, "name": "Eli Vale", "slug": "eli-vale", "imageUrl": "..." }
  ],
  "likeCount": 142,
  "isLiked": true
}
```

**Backward compatible:** Phase 1 vẫn trả về `lyrics` và `audioUrl` trong
response, dù có ghi chú "chưa dùng" trong CLAUDE.md. Sẽ cải thiện ở phase
sau. Phase 1 giữ để frontend dev nhanh.

**Auth behavior:**

- Có token hợp lệ → `isLiked` check từ `song_likes`. Không có token → `isLiked: false`.
- KHÔNG 401 nếu không login, vẫn trả response bình thường.

**Side effect:** Mỗi lần gọi → tăng `songs.play_count` lên 1.
Dùng `UPDATE ... SET play_count = play_count + 1` (atomic).
Không cần tracking IP/user, chỉ đếm lượt view.

### `GET /api/songs/{slug}/related` — endpoint mới

**Query params:**

- `size` (optional, default 8, max 12).

**Logic ưu tiên:**

1. Lấy các bài cùng `artists.id` (qua `song_artists`) mà `status = PUBLISHED`,
  loại trừ bài hiện tại.
2. Nếu chưa đủ → thêm bài cùng `album_id`.
3. Nếu vẫn chưa đủ → random bài PUBLISHED khác.
4. Sắp xếp: cùng artist trước → cùng album → còn lại.
5. Limit theo `size`.

**Response:**

```json
{
  "items": [
    { "id": 2, "title": "...", "slug": "...", "coverUrl": "...",
      "durationSec": 200, "playCount": 100, "audioUrl": "...",
      "artists": [{ "id": 1, "name": "Lena Rivers", "slug": "lena-rivers" }] }
  ]
}
```

Lưu ý: dùng shape **tối giản** cho list (không trả lyrics, status, v.v.) để
giảm payload và tránh phải xử lý nhiều field null.

**Error:**

- 404 `SONG_NOT_FOUND` nếu `slug` không tồn tại.
- 200 với `items: []` nếu bài đó tồn tại nhưng không có related.



## Backend implementation notes



### Files sẽ thay đổi / tạo mới

1. `dto/response/SongResponse.java` — thêm field `artists`, `album`,
  `likeCount`, `isLiked`. Thêm static factory `fromEntity(Song, List<Artist>,  Album, long likeCount, boolean isLiked)` giữ nguyên `fromEntity(Song)` cho
   list endpoint cũ.
2. `dto/response/ArtistSummaryResponse.java` (NEW) — bản nhẹ:
  `id, name, slug, imageUrl`.
3. `dto/response/AlbumSummaryResponse.java` (NEW) — bản nhẹ:
  `id, title, slug, coverUrl`.
4. `dto/response/SongSummaryResponse.java` (NEW) — bản nhẹ cho related:
  `id, title, slug, coverUrl, durationSec, playCount, audioUrl, artists[]`.
5. `repository/SongRepository.java` — thêm:
  - `List<Artist> findArtistsForSong(int songId)`
  - `Optional<Album> findAlbumForSong(int songId)` (cần `AlbumRepository` mới
  hoặc query inline)
  - `long countLikes(int songId)`
  - `boolean isLikedBy(int songId, int userId)`
  - `void incrementPlayCount(int songId)`
  - `List<Song> findRelated(int songId, int artistId, int albumId, int limit)`
6. `repository/AlbumRepository.java` (NEW) — query album theo id và theo slug.
7. `service/song/SongService.java` — refactor:
  - `getBySlug(String slug, Optional<Integer> userId)` trả về
   `Optional<SongDetailResponse>` (DTO mới) thay vì `Optional<SongResponse>`.
  - `getRelated(String slug, int size)` trả `List<SongSummaryResponse>`.
  - Giữ `getPage(...)` nguyên cho list endpoint cũ.
8. `dto/response/SongDetailResponse.java` (NEW) — extends `SongResponse`, thêm
  `artists`, `album`, `likeCount`, `isLiked`. Hoặc đơn giản hơn: tạo DTO mới
   riêng, không extends.
9. `controller/song/SongServlet.java` — thêm handler cho `/related`.
10. `controller/song/SongServlet.java` — đọc `Authorization` header để detect
  user hiện tại. **Không** require auth, chỉ dùng nếu có.



### Token parsing nhẹ

Vì `JWTUtil` parse được token, cần một helper nhỏ trong `SongServlet`:

```java
private Optional<Integer> currentUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
    String token = header.substring(7).trim();
    if (token.isEmpty()) return Optional.empty();
    try {
        return jwtUtil.extractUserId(token); // giả định method này tồn tại
    } catch (Exception e) {
        return Optional.empty();
    }
}
```

Sẽ kiểm tra method có sẵn của `JwtUtil` trước khi dùng.

### Cẩn trọng

- Query `artists` của một bài JOIN `song_artists` + `artists`. Nếu artist đã
soft-deleted → loại bỏ (WHERE `artists.deleted_at IS NULL`).
- Query `album` cũng filter `deleted_at IS NULL`. Nếu album bị xoá mềm →
trả `album: null` (vẫn có `albumId` để tương thích).
- `incrementPlayCount` chạy **sau khi** đã check song tồn tại, để tránh tăng
count cho slug không tồn tại.



## Frontend implementation notes



### Files sẽ thay đổi / tạo mới

1. `router/index.js` — thêm route `/songs/:slug` → `SongDetailView`.
2. `views/SongDetailView.vue` (NEW) — layout:
  ```
   ┌─────────────────────────────────────────────┐
   │ [Back]                                       │
   │                                              │
   │  ┌────────┐   Title (h1)                     │
   │  │ Cover  │   Artist chip(s) · Album chip    │
   │  │ 240x   │   84291 plays · 3:58             │
   │  └────────┘   [▶ Play] [♥ Like] [+ Add]     │
   │                                              │
   ├─────────────────────────────────────────────┤
   │ Lyrics panel (collapsible)                   │
   ├─────────────────────────────────────────────┤
   │ Related songs (grid 4 cols)                  │
   └─────────────────────────────────────────────┘
  ```
3. `services/songService.js` — thêm:
  - `getDetail(slug)` → GET `/api/songs/{slug}`.
  - `getRelated(slug)` → GET `/api/songs/{slug}/related`.
4. `views/HomeView.vue` — đổi button "New releases" thành `<RouterLink>`
  thay vì gọi `playNewRelease` trực tiếp. Click cover/title → navigate
   detail page. Riêng nút play → vẫn chơi nhạc.
5. `views/ArtistView.vue` — `TrackRow` giữ nguyên. Title của track link tới
  detail page. Play button vẫn chơi nhạc.
6. `components/music/TrackRow.vue` — thêm `@click` lên title (nhưng tránh
  trigger khi click vào nút play/menu/heart).



### Reusable bits

- Format duration đã có `formatDuration`.
- Hiển thị cover placeholder đã có pattern trong `HomeView.vue` / `ArtistView.vue`.
- `usePlayerStore` đã có sẵn `playTrack(track, list)`. `SongDetailView` sẽ
build list = `[thisSong, ...related.map(toPlayerTrack)]` rồi gọi `playTrack`.



### Cẩn trọng

- `Related` trả `SongSummary` (không có artist) → cần map thêm field `artists`
vào player track để hiển thị tên artist trong queue. Phase 1 sẽ join lại
trong summary response (đã có `artists[]`).
- Không block UI khi gọi `getRelated` → chạy song song với `getDetail` qua
`Promise.all`.
- Loading skeleton giống pattern `HomeView.vue`.
- 404 → render "Song not found" với link về Home.



## Test plan (manual)


| Test                                     | Expected                                                              |
| ---------------------------------------- | --------------------------------------------------------------------- |
| Vào `/songs/velvet-hours`                | Hiện cover, artist "Lena Rivers", album, play count, related bên dưới |
| Click play ở SongDetail                  | Bài bắt đầu chơi, queue = bài này + related                           |
| Vào `/songs/khong-ton-tai`               | 404 view "Song not found"                                             |
| Login, vào detail bài đã like            | `isLiked = true` (Phase 1 chưa có UI toggle, chỉ check API)           |
| Logout, vào detail                       | `isLiked = false`, vẫn load được                                      |
| Refresh detail page 5 lần                | `playCount` tăng 5 (check DB hoặc re-fetch)                           |
| Từ HomeView click cover "Velvet Hours"   | Navigate sang `/songs/velvet-hours`                                   |
| Từ ArtistView click title "Velvet Hours" | Navigate sang detail                                                  |




## Out of scope (chừa cho Phase 2+)

- Like button toggle (Phase 2).
- Follow button (Phase 2).
- Add to playlist (Phase 3).
- Share / Copy link (Phase 4).
- Karaoke-style lyrics (Phase 5).
- Comment / Discussion (chưa có trong schema → skip).

