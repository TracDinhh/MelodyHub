# Luồng Become Artist

## Tổng quan

Khi user muốn trở thành Artist, họ cần submit request. Request này sẽ được **Admin review** và approve/reject. User sẽ không tự động trở thành Artist - cần có Admin xử lý.

## Các bước thực hiện

### Bước 1: User submit request

```
Frontend: User vào /become-artist → Điền form → Submit
    ↓
POST /api/artist-requests
Body: { artistName, bio?, imageUrl? }
```

**Backend xử lý:**
1. `ArtistServlet.doPost()` nhận request
2. Gọi `ArtistRegistrationService.submitRequest(token, request)`
3. Kiểm tra:
   - User đã là Artist chưa? → Ném lỗi `ARTIST_ALREADY_EXISTS`
   - Đã có request PENDING chưa? → Ném lỗi `ARTIST_REQUEST_PENDING_EXISTS`
4. Tạo slug từ artist name (tự động tạo unique slug)
5. Lưu vào database với `status = 'PENDING'`
6. Trả về response cho frontend

### Bước 2: Request chờ Admin review

```
Database: artist_requests
┌────┬─────────┬──────────────┬────────┬─────────────┐
│ id │ user_id │ artist_name  │ status │ created_at  │
├────┼─────────┼──────────────┼────────┼─────────────┤
│ 1  │ 5       │ MyArtistName │ PENDING│ 2026-08-10  │
└────┴─────────┴──────────────┴────────┴─────────────┘
```

**Trạng thái PENDING** có nghĩa là request đang chờ được xử lý. **Đây là lý do request 5 phút chưa xong** - vì không có Admin nào approve nó!

### Bước 3: Admin approve/reject

Admin vào dashboard → thấy danh sách pending requests → approve hoặc reject.

#### Approve Flow:
```
Admin: POST /api/admin/artist-requests/{id}/approve
    ↓
AdminArtistRequestService.approve(token, requestId)
    ↓
1. Cập nhật user role: USER → ARTIST
2. Tạo artist profile mới
3. Cập nhật request status: PENDING → APPROVED
    ↓
Frontend: User thấy thông báo "Request approved" → Đăng nhập lại
```

#### Reject Flow:
```
Admin: POST /api/admin/artist-requests/{id}/reject
Body: { reviewNote?: "Lý do từ chối" }
    ↓
AdminArtistRequestService.reject(token, requestId, reviewNote)
    ↓
Cập nhật request: status → REJECTED, lưu reviewNote
    ↓
Frontend: User thấy thông báo bị từ chối + lý do
```

## Sơ đồ luồng

```
┌──────────┐         ┌─────────────┐         ┌──────────┐
│  User    │         │   Backend   │         │  Admin   │
└────┬─────┘         └──────┬──────┘         └────┬─────┘
     │                       │                     │
     │  1. Submit Request    │                     │
     │──────────────────────>│                     │
     │                       │                     │
     │  2. Save PENDING     │                     │
     │                       │                     │
     │  3. Show "Pending"   │                     │
     │<──────────────────────│                     │
     │                       │                     │
     │                       │  4. List Requests   │
     │                       │<────────────────────│
     │                       │                     │
     │                       │  5. GET /admin/     │
     │                       │      artist-req     │
     │                       │───────────────────>│
     │                       │                     │
     │                       │  6. Review &       │
     │                       │    Approve/Reject  │
     │                       │<───────────────────│
     │                       │                     │
     │  7. Status Updated   │                     │
     │  (if logged in)      │                     │
     │<──────────────────────│                     │
```

## Tại sao request 5 phút chưa xong?

**Lý do:** Request của bạn đang ở trạng thái `PENDING` - nghĩa là đang chờ Admin review. 

### Các nguyên nhân có thể:
1. **Không có Admin nào online** - Cần có user có role `ADMIN` để approve
2. **Admin chưa vào dashboard** - Admin cần vào trang quản lý artist requests
3. **Bug backend** - Kiểm tra backend logs

### Cách kiểm tra:

**1. Kiểm tra database:**
```sql
SELECT * FROM artist_requests WHERE user_id = <your_user_id>;
-- Xem status hiện tại là gì
```

**2. Kiểm tra backend logs:**
- Xem có lỗi gì không khi Admin approve
- Kiểm tra `AdminArtistRequestService` có được gọi không

**3. Kiểm tra user role:**
```sql
SELECT id, username, role FROM users WHERE id = <your_user_id>;
-- Phải là 'USER' mới có thể submit request thành công
-- Nếu đã là 'ARTIST' thì không thể submit nữa
```

## Frontend States

```
┌─────────────────┐
│   isLoading     │ ──> Hiển thị loading spinner
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌──────────┐
│  status ===     │────>│ PENDING  │ ──> "Request pending" message
│  'PENDING'      │     └──────────┘
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌──────────┐
│  status ===     │────>│ APPROVED │ ──> "Approved!" + Sign in again button
│  'APPROVED'     │     └──────────┘
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌──────────┐
│  status ===     │────>│ REJECTED │ ──> Rejection reason + Form để resubmit
│  'REJECTED'     │     └──────────┘
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  status ===     │ ──> Hiển thị form để submit
│  'NONE'         │
└─────────────────┘
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/artist-requests` | User submit become artist request |
| GET | `/api/artist-requests/me` | User xem request của mình |
| GET | `/api/admin/artist-requests` | Admin list requests (filter by status) |
| POST | `/api/admin/artist-requests/{id}/approve` | Admin approve |
| POST | `/api/admin/artist-requests/{id}/reject` | Admin reject |

## Database Tables

### `artist_requests`
| Column | Type | Mô tả |
|--------|------|-------|
| id | INT | Primary key |
| user_id | INT | FK → users.id |
| artist_name | VARCHAR(200) | Tên artist |
| slug | VARCHAR(220) | URL-friendly name |
| bio | TEXT | Tiểu sử |
| image_url | VARCHAR(500) | Avatar URL |
| status | ENUM | PENDING, APPROVED, REJECTED |
| review_note | TEXT | Lý do reject (nếu có) |
| reviewed_by | INT | FK → users.id (Admin) |
| reviewed_at | DATETIME | Thời gian review |
| created_at | DATETIME | Thời gian submit |

### `users`
| Column | Type | Mô tả |
|--------|------|-------|
| role | ENUM | USER, ARTIST, ADMIN |
