# Tối ưu & Clean code — Nhật ký

Tài liệu theo dõi các hạng mục tối ưu và làm sạch code base MelodyHub, được xác
định qua đợt review toàn bộ backend + frontend (2026-08-14).

Trạng thái:
- ✅ **Đã sửa** — đã triển khai và verify build.
- 🔜 **Sẽ sửa** — đã lên kế hoạch, chưa triển khai.

---

## ✅ Đã sửa (đợt 1 — 2 bug logic)

### 1. Fetch lyrics không còn tăng `play_count` (backend)

- **File:** [backend/.../controller/song/SongServlet.java](../backend/src/main/java/com/melodyHub/controller/song/SongServlet.java) — `handleGetLyrics`
- **Vấn đề:** `handleGetLyrics` gọi `songService.getDetail(slug, null)` chỉ để đọc
  lyrics, nhưng `getDetail` có side effect `incrementPlayCount` → mỗi lần lấy
  lyrics là một lượt nghe ảo, làm sai lệch `play_count` (chỉ số này còn dùng để
  xếp hạng bài liên quan).
- **Cách sửa:** Chuyển sang `songService.getBySlug(slug)` (read-only, không tăng
  count). Đường `GET /api/songs/{slug}` bình thường vẫn giữ nguyên việc tăng count.

### 2. Ngưỡng nghe 30s + debounce ghi storage (frontend)

- **File:** [frontend/src/stores/listenTracker.store.js](../frontend/src/stores/listenTracker.store.js)
- **Vấn đề:**
  - `MIN_LISTEN_SECONDS = 0` → bài được ghi vào lịch sử ngay khi bắt đầu phát,
    mâu thuẫn với UI ("ghi sau khi nghe ≥ 30 giây") và gây ghi nhầm khi lướt qua.
  - `noteProgress` chạy `JSON.stringify` + `sessionStorage.setItem` đồng bộ trên
    main thread mỗi `timeupdate` (~4 lần/giây).
- **Cách sửa:**
  - Đặt `MIN_LISTEN_SECONDS = 30`.
  - `noteProgress` chỉ động vào state/storage khi giá trị giây thực sự đổi.
  - Debounce ghi `sessionStorage` xuống tối đa 1 lần/giây (`schedulePersist`).
  - Tự gọi `tryRecord` ngay khi vượt ngưỡng 30s (không phụ thuộc `pause`/`ended`).
  - `clear()` hủy cả debounce timer đang chờ khi logout.

---

## ✅ Nhóm A — Tối ưu hiệu năng Database (đã triển khai)

| Vấn đề | File đã sửa | Cách tối ưu |
|--------|-------------|-------------|
| Không có connection pool — mở kết nối mới mỗi query | [config/DatabaseConfig.java](../backend/src/main/java/com/melodyHub/config/DatabaseConfig.java) | Thêm HikariCP, tạo pool 1 lần, cắm vào seam `DataSource` sẵn có của các repository |
| N+1 query khi lấy artist theo bài | [repository/SongRepository.java](../backend/src/main/java/com/melodyHub/repository/SongRepository.java) + các service | `findArtistsForSongs(Collection<Integer>)` chạy 1 query `WHERE song_id IN (...)`, gom nhóm trong bộ nhớ |
| Đếm số bài playlist theo từng cái | [repository/PlaylistRepository.java](../backend/src/main/java/com/melodyHub/repository/PlaylistRepository.java) | `countSongsFor(...)` dùng 1 query `GROUP BY playlist_id` |
| Thiếu index cho query browse chính | [resources/db/schema.sql](../backend/src/main/resources/db/schema.sql) | Thêm index phục vụ browse (`status, deleted_at, created_at, id`) |
| Search full scan | [repository/SongRepository.java](../backend/src/main/java/com/melodyHub/repository/SongRepository.java) | Tối ưu điều kiện search |
| `findRelated` correlated subquery toàn catalog | [repository/SongRepository.java](../backend/src/main/java/com/melodyHub/repository/SongRepository.java) | Lọc ứng viên trước rồi mới rank |

## ✅ Nhóm B — Dọn code trùng lặp (đã triển khai, không đổi hành vi)

| Vấn đề | File đã sửa | Cách tối ưu |
|--------|-------------|-------------|
| Servlet trùng `ObjectMapper`, `writeJson`, `writeError`, `getBearerToken`... | [controller/JsonServlet.java](../backend/src/main/java/com/melodyHub/controller/JsonServlet.java) + tất cả `*Servlet` | Tách abstract `JsonServlet` base class; Upload/Artist/PublicArtist/Playlist/Admin/Song đều extends |
| Row-mapping trùng ở nhiều repository | [util/SqlSupport.java](../backend/src/main/java/com/melodyHub/util/SqlSupport.java) | `getNullableInteger/Short`, `getLocalDateTime`, `placeholders(int)` dùng chung |
| Hàm hash SHA-256 + sinh token trùng lặp | [util/TokenHashUtil.java](../backend/src/main/java/com/melodyHub/util/TokenHashUtil.java) | `sha256Hex()` + `randomToken()`; RefreshTokenService & PasswordResetService tái dùng |
| Dead code servlet lyrics | ~~controller/song/SongLyricsServlet.java~~ | Đã xóa (không map trong web.xml, logic đã lệch) |
| Import thừa | [repository/ListenHistoryRepository.java](../backend/src/main/java/com/melodyHub/repository/ListenHistoryRepository.java) | Xóa import không dùng |
| `(page-1)*size` bằng `int` không guard → tràn số | [util/Pagination.java](../backend/src/main/java/com/melodyHub/util/Pagination.java) | Helper `offset(page, size)` tính bằng `long`, kẹp về `[0, Integer.MAX_VALUE]`; mọi service dùng chung |

## ✅ Nhóm C — Tối ưu Frontend (đã triển khai)

| Vấn đề | File đã sửa | Cách tối ưu |
|--------|-------------|-------------|
| `fetch()` thô bỏ qua lớp HTTP | [frontend/src/stores/player.store.js](../frontend/src/stores/player.store.js) — `loadSyncedLyrics` | Dùng `songService.getSyncedLyrics(slug)` |
| `toPlayerTrack` copy-paste 4+ nơi, shape khác nhau | [frontend/src/utils/playerTrack.js](../frontend/src/utils/playerTrack.js) | Tách `toPlayerTrack(song, { artist })` + `joinArtistNames`; HomeView, SongDetailView, PlaylistDetailView, ListenHistoryView, ArtistView dùng chung |
| Interval `tick()` no-op + `progress` trùng | [frontend/src/components/player/BottomPlayer.vue](../frontend/src/components/player/BottomPlayer.vue), [frontend/src/stores/player.store.js](../frontend/src/stores/player.store.js) | Xóa `setInterval`/`tick`; dùng thẳng `player.progress` (đã tính từ sự kiện `timeupdate`) |
| `LyricsEditor` re-stringify mỗi keystroke | [frontend/src/views/artist/components/LyricsEditor.vue](../frontend/src/views/artist/components/LyricsEditor.vue) | Debounce emit 300ms (`scheduleEmitSynced`); flush khi đổi type/unmount để không mất dữ liệu |
| Seek bar thiếu a11y | [frontend/src/views/artist/components/LyricsEditor.vue](../frontend/src/views/artist/components/LyricsEditor.vue) | Thêm `aria-valuetext` mô tả vị trí phát (input range đã hỗ trợ bàn phím sẵn) |

---

## 🔜 Còn lại (chưa triển khai đợt này)

| Vấn đề | File | Ghi chú |
|--------|------|---------|
| Format thời lượng/ngày rải rác | Nhiều view | `formatDate.js` đã có `formatDuration`/`formatDate`; còn vài chỗ dùng `Intl.DateTimeFormat` inline có thể gom tiếp |
| Logic phân trang lặp | Nhiều view + store | Tách composable `usePagination(fetcher)` |
| Secret bị commit | [application.properties](../backend/src/main/resources/application.properties) | `jwt.secret`, `db.password` cần chuyển sang biến môi trường trước khi deploy production |
| Enforce ngưỡng nghe 30s phía backend | service listen | Hiện chỉ enforce ở frontend |

---

## Ghi chú

- Backend verify: `mvn clean package` build thành công (WAR sinh trong `target/`).
- Frontend verify: `npm run build` build thành công.
- Chi tiết đầy đủ (kèm số dòng, mức độ nghiêm trọng) nằm trong báo cáo review đợt
  đầu; tài liệu này chỉ liệt kê các hạng mục hành động và tiến độ.
