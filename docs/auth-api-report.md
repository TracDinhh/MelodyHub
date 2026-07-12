# MelodyHub Auth API Report

## 1. Tổng quan luồng auth

Phần đăng ký/đăng nhập hiện tại là backend API trong Java Servlet.

Luồng chung:

```text
Client/Postman/Frontend
-> AuthServlet
-> AuthService
-> UserRepository / PasswordUtil / JwtUtil
-> MySQL
```

Vai trò từng lớp:

- `AuthServlet`: nhận HTTP request, đọc JSON, gọi service, trả JSON response.
- `AuthService`: xử lý logic đăng ký, đăng nhập, kiểm tra token.
- `UserRepository`: thao tác bảng `users` bằng JDBC.
- `PasswordUtil`: hash và verify password bằng BCrypt.
- `JwtUtil`: tạo và kiểm tra JWT token.
- `AuthException`: lỗi auth có `code` và `message` để trả về frontend.

Servlet được map trong `web.xml`:

```text
/api/auth/*
```

Nếu chạy WAR mặc định trên Tomcat theo README, URL thường là:

```text
/melodyhub-backend/api/auth/...
```

Nếu app được deploy ở root context thì URL là:

```text
/api/auth/...
```

## 2. API đăng ký

```http
POST /api/auth/register
Content-Type: application/json
```

API này dùng để tạo tài khoản mới.

Request body:

```json
{
  "username": "trac",
  "email": "trac@example.com",
  "password": "123456",
  "displayName": "Trac Dinh"
}
```

Code chạy như sau:

```text
AuthServlet.doPost()
-> path /register
-> handleRegister()
-> đọc JSON thành RegisterRequest
-> authService.register()
```

Trong `AuthService.register()`:

```text
Kiểm tra request không null
-> kiểm tra username
-> kiểm tra email
-> kiểm tra password
-> kiểm tra displayName
-> check username đã tồn tại chưa
-> check email đã tồn tại chưa
-> hash password bằng PasswordUtil.hash()
-> tạo User role USER, status ACTIVE
-> lưu vào DB bằng UserRepository.create()
-> tạo JWT bằng JwtUtil.generateToken()
-> trả AuthResponse
```

Rule hiện tại:

- `username` bắt buộc, dài 3-50 ký tự.
- `email` bắt buộc, đúng format cơ bản, tối đa 255 ký tự.
- `password` bắt buộc, tối thiểu 6 ký tự.
- `displayName` không bắt buộc, tối đa 100 ký tự.
- Client không được tự chọn `role` hoặc `status`.
- User mới luôn có:

```text
role = USER
status = ACTIVE
```

Success response:

```http
201 Created
```

```json
{
  "token": "jwt-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "user": {
    "id": 1,
    "username": "trac",
    "email": "trac@example.com",
    "displayName": "Trac Dinh",
    "avatarUrl": null,
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-07-07T10:00:00",
    "updatedAt": "2026-07-07T10:00:00"
  }
}
```

Lỗi có thể gặp:

| HTTP | Code | Khi nào xảy ra |
| --- | --- | --- |
| 400 | `INVALID_JSON` | Body JSON sai format |
| 400 | `INVALID_REQUEST` | Request bị null |
| 400 | `INVALID_USERNAME` | Thiếu username hoặc username sai độ dài |
| 400 | `INVALID_EMAIL` | Thiếu email hoặc email sai format |
| 400 | `INVALID_PASSWORD` | Thiếu password hoặc password dưới 6 ký tự |
| 400 | `INVALID_DISPLAY_NAME` | Display name dài hơn 100 ký tự |
| 409 | `USERNAME_EXISTS` | Username đã tồn tại |
| 409 | `EMAIL_EXISTS` | Email đã tồn tại |
| 500 | `DATABASE_ERROR` | Lỗi kết nối/truy vấn DB |

Ví dụ lỗi:

```json
{
  "code": "USERNAME_EXISTS",
  "message": "Username already exists"
}
```

## 3. API đăng nhập

```http
POST /api/auth/login
Content-Type: application/json
```

API này dùng để đăng nhập bằng username hoặc email.

Request body:

```json
{
  "usernameOrEmail": "trac",
  "password": "123456"
}
```

Hoặc:

```json
{
  "usernameOrEmail": "trac@example.com",
  "password": "123456"
}
```

Code chạy như sau:

```text
AuthServlet.doPost()
-> path /login
-> handleLogin()
-> đọc JSON thành LoginRequest
-> authService.login()
```

Trong `AuthService.login()`:

```text
Kiểm tra usernameOrEmail và password
-> tìm user theo username
-> nếu không có thì tìm theo email
-> nếu không tìm thấy thì báo INVALID_CREDENTIALS
-> nếu user bị BANNED thì báo USER_BANNED
-> verify password bằng PasswordUtil.verify()
-> nếu password sai thì báo INVALID_CREDENTIALS
-> tạo JWT bằng JwtUtil.generateToken()
-> trả AuthResponse
```

Success response:

```http
200 OK
```

```json
{
  "token": "jwt-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "user": {
    "id": 1,
    "username": "trac",
    "email": "trac@example.com",
    "displayName": "Trac Dinh",
    "avatarUrl": null,
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-07-07T10:00:00",
    "updatedAt": "2026-07-07T10:00:00"
  }
}
```

Lỗi có thể gặp:

| HTTP | Code | Khi nào xảy ra |
| --- | --- | --- |
| 400 | `INVALID_JSON` | Body JSON sai format |
| 400 | `INVALID_REQUEST` | Request bị null |
| 401 | `INVALID_CREDENTIALS` | Thiếu username/email, thiếu password, user không tồn tại, hoặc password sai |
| 403 | `USER_BANNED` | Tài khoản bị khóa |
| 500 | `DATABASE_ERROR` | Lỗi kết nối/truy vấn DB |

Lưu ý: user không tồn tại và sai password đều trả `INVALID_CREDENTIALS`. Làm vậy để không tiết lộ tài khoản nào đang tồn tại trong hệ thống.

## 4. API lấy user hiện tại

```http
GET /api/auth/me
Authorization: Bearer <token>
```

API này dùng để kiểm tra token hiện tại thuộc user nào.

Header bắt buộc:

```text
Authorization: Bearer jwt-token
```

Code chạy như sau:

```text
AuthServlet.doGet()
-> path /me
-> handleMe()
-> lấy token từ Authorization header
-> authService.getCurrentUser(token)
```

Trong `AuthService.getCurrentUser()`:

```text
Kiểm tra token có tồn tại không
-> verify token bằng JwtUtil.getUserIdFromToken()
-> lấy userId trong token
-> tìm user theo id trong DB
-> nếu user bị BANNED thì báo USER_BANNED
-> trả UserResponse
```

Success response:

```http
200 OK
```

```json
{
  "id": 1,
  "username": "trac",
  "email": "trac@example.com",
  "displayName": "Trac Dinh",
  "avatarUrl": null,
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-07-07T10:00:00",
  "updatedAt": "2026-07-07T10:00:00"
}
```

Lỗi có thể gặp:

| HTTP | Code | Khi nào xảy ra |
| --- | --- | --- |
| 401 | `MISSING_TOKEN` | Không gửi Authorization header hoặc không đúng dạng Bearer |
| 401 | `INVALID_TOKEN` | Token sai, hết hạn, hoặc chữ ký không hợp lệ |
| 403 | `USER_BANNED` | Tài khoản bị khóa |
| 404 | `USER_NOT_FOUND` | Token hợp lệ nhưng user trong DB không còn tồn tại |
| 500 | `DATABASE_ERROR` | Lỗi kết nối/truy vấn DB |

## 5. API đăng xuất

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

API này hiện tại chỉ trả thành công:

```http
204 No Content
```

Vì auth đang dùng JWT stateless:

```text
Server không lưu session đăng nhập
-> server không có gì để xóa trong DB/session
-> frontend cần tự xóa token đang lưu
```

Luồng logout hiện tại:

```text
Frontend gọi POST /api/auth/logout
-> backend trả 204
-> frontend xóa token khỏi localStorage/session/memory
-> user được xem như đã đăng xuất
```

Lưu ý: phase hiện tại chưa có JWT blacklist. Nghĩa là nếu token cũ vẫn còn bị giữ ở đâu đó và chưa hết hạn, token đó vẫn có thể dùng được cho tới khi hết hạn.

## 6. AuthResponse là gì

`AuthResponse` là response khi đăng ký hoặc đăng nhập thành công.

```json
{
  "token": "jwt-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "user": {}
}
```

Ý nghĩa:

- `token`: JWT token để frontend dùng cho các API cần đăng nhập.
- `tokenType`: luôn là `Bearer`.
- `expiresInSeconds`: thời gian sống của token, hiện lấy từ `jwt.expires-minutes`.
- `user`: thông tin user, không có `passwordHash`.

Frontend sẽ gọi API cần đăng nhập bằng header:

```text
Authorization: Bearer jwt-token
```

## 7. ErrorResponse là gì

`ErrorResponse` là response khi API bị lỗi.

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Username/email or password is incorrect"
}
```

Ý nghĩa:

- `code`: mã lỗi cho frontend xử lý logic.
- `message`: nội dung lỗi để hiển thị hoặc debug.

Ví dụ frontend có thể dùng `code` để hiển thị thông báo:

```text
USERNAME_EXISTS -> Username đã tồn tại
EMAIL_EXISTS -> Email đã tồn tại
INVALID_CREDENTIALS -> Sai tài khoản hoặc mật khẩu
```

## 8. JWT chạy như thế nào

Khi register/login thành công:

```text
AuthService
-> JwtUtil.generateToken(user)
-> tạo token chứa userId, username, role
-> trả token về frontend
```

Token hiện chứa:

```text
subject = user id
claim userId
claim username
claim role
issuer = melodyhub
expiresAt = thời điểm hết hạn
```

Khi gọi `/me`:

```text
Frontend gửi Authorization: Bearer token
-> AuthServlet lấy token
-> AuthService gọi JwtUtil.getUserIdFromToken(token)
-> JwtUtil verify token
-> lấy userId
-> UserRepository tìm user trong DB
-> trả UserResponse
```

JWT secret và thời gian hết hạn nằm trong:

```properties
jwt.secret=change-this-secret-for-local-dev
jwt.expires-minutes=1440
```

`1440` phút = 24 giờ.

## 9. Ví dụ curl

Register:

```bash
curl -X POST http://localhost:8080/melodyhub-backend/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trac",
    "email": "trac@example.com",
    "password": "123456",
    "displayName": "Trac Dinh"
  }'
```

Login:

```bash
curl -X POST http://localhost:8080/melodyhub-backend/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "trac",
    "password": "123456"
  }'
```

Me:

```bash
curl http://localhost:8080/melodyhub-backend/api/auth/me \
  -H "Authorization: Bearer jwt-token"
```

Logout:

```bash
curl -X POST http://localhost:8080/melodyhub-backend/api/auth/logout \
  -H "Authorization: Bearer jwt-token"
```

## 10. Những việc chưa làm ở phase này

- Chưa có CORS filter. Phần này nằm ở Task 7.
- Chưa có refresh token.
- Chưa có blacklist token khi logout.
- Chưa có frontend form đăng ký/đăng nhập.
- Chưa test API thật với MySQL đang chạy.
- `db.password` và `jwt.secret` trong `application.properties` vẫn là giá trị local placeholder.
