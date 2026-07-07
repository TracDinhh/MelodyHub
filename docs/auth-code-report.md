# AuthServlet Code Report

File này chỉ giải thích code trong:

```text
backend/src/main/java/com/melodyHub/controller/AuthServlet.java
```

`AuthServlet` là class nhận request HTTP cho phần auth. Nó không tự xử lý đăng ký/đăng nhập trong DB, mà chỉ:

```text
nhận request
-> đọc JSON/header
-> gọi AuthService
-> trả JSON response
```

## 1. Khai báo class

```java
public class AuthServlet extends HttpServlet
```

`extends HttpServlet` nghĩa là class này là một Java Servlet. Tomcat sẽ tự gọi:

- `doPost()` khi client gửi request `POST`.
- `doGet()` khi client gửi request `GET`.
- `init()` khi servlet được khởi tạo.

## 2. Các import quan trọng

```java
import com.fasterxml.jackson.databind.ObjectMapper;
```

`ObjectMapper` dùng để chuyển đổi giữa JSON và object Java.

Ví dụ:

```text
JSON body -> RegisterRequest
AuthResponse object -> JSON response
```

```java
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
```

Dùng để Jackson hiểu các kiểu ngày giờ Java như `LocalDateTime`.

```java
import com.melodyHub.service.AuthService;
```

Servlet sẽ gọi `AuthService` để xử lý logic thật sự.

```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```

- `HttpServletRequest`: dữ liệu request client gửi lên.
- `HttpServletResponse`: dữ liệu response backend trả về.

## 3. Constant

```java
private static final String CONTENT_TYPE_JSON = "application/json";
private static final String BEARER_PREFIX = "Bearer ";
```

`CONTENT_TYPE_JSON` dùng khi backend trả JSON:

```http
Content-Type: application/json
```

`BEARER_PREFIX` dùng để đọc token từ header:

```http
Authorization: Bearer <token>
```

`private static final` nghĩa là:

- `private`: chỉ dùng trong class này.
- `static`: thuộc về class, không cần tạo object.
- `final`: giá trị không đổi.

## 4. ObjectMapper

```java
private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

Đây là object chuyên xử lý JSON.

```java
.registerModule(new JavaTimeModule())
```

Giúp Jackson xử lý `LocalDateTime`.

```java
.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
```

Không trả ngày giờ dạng số timestamp, mà trả dạng dễ đọc:

```text
2026-07-07T10:00:00
```

## 5. AuthService field và init()

```java
private AuthService authService;
```

Biến này giữ service để servlet gọi.

```java
@Override
public void init() throws ServletException {
    authService = new AuthService();
}
```

`init()` chạy một lần khi Tomcat tạo servlet.

Trong `init()`, code tạo `AuthService`:

```java
authService = new AuthService();
```

Sau đó các request register/login/me sẽ dùng lại service này.

## 6. doPost()

```java
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
```

Method này xử lý request `POST`.

Hiện tại các API POST là:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
```

### Set encoding

```java
request.setCharacterEncoding(StandardCharsets.UTF_8.name());
```

Dòng này giúp đọc body UTF-8, tránh lỗi tiếng Việt.

### Chọn endpoint bằng switch

```java
switch (getPath(request)) {
    case "/register" -> handleRegister(request, response);
    case "/login" -> handleLogin(request, response);
    case "/logout" -> response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    default -> writeError(...);
}
```

`getPath(request)` lấy phần path sau `/api/auth`.

Ví dụ:

```text
/api/auth/register -> /register
/api/auth/login -> /login
/api/auth/logout -> /logout
```

Nếu path là `/register`, servlet gọi:

```java
handleRegister(request, response);
```

Nếu path là `/login`, servlet gọi:

```java
handleLogin(request, response);
```

Nếu path là `/logout`, servlet chỉ trả:

```java
204 No Content
```

Vì JWT logout hiện tại là phía frontend tự xóa token.

### Các catch trong doPost()

```java
catch (AuthException exception)
```

Bắt lỗi auth do `AuthService` ném ra, ví dụ:

- username trùng.
- email trùng.
- sai mật khẩu.
- user bị banned.

```java
writeError(response, getStatusCode(exception), exception.getCode(), exception.getMessage());
```

Chuyển lỗi thành JSON response.

```java
catch (SQLException exception)
```

Bắt lỗi database và trả:

```json
{
  "code": "DATABASE_ERROR",
  "message": "Database error occurred"
}
```

```java
catch (IOException exception)
```

Bắt lỗi đọc body JSON. Ví dụ client gửi JSON sai format.

## 7. doGet()

```java
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
```

Method này xử lý request `GET`.

Hiện tại chỉ có:

```text
GET /api/auth/me
```

Code:

```java
if ("/me".equals(getPath(request))) {
    handleMe(request, response);
    return;
}
```

Nếu path là `/me`, gọi `handleMe()`.

Nếu path khác, trả lỗi `404 NOT_FOUND`.

## 8. handleRegister()

```java
private void handleRegister(HttpServletRequest request, HttpServletResponse response)
        throws IOException, AuthException, SQLException {
    RegisterRequest registerRequest = objectMapper.readValue(request.getInputStream(), RegisterRequest.class);
    writeJson(response, HttpServletResponse.SC_CREATED, authService.register(registerRequest));
}
```

Method này xử lý `POST /api/auth/register`.

Dòng này:

```java
RegisterRequest registerRequest = objectMapper.readValue(request.getInputStream(), RegisterRequest.class);
```

đọc JSON body và đổi thành object `RegisterRequest`.

Ví dụ body:

```json
{
  "username": "trac",
  "email": "trac@example.com",
  "password": "123456",
  "displayName": "Trac Dinh"
}
```

sẽ thành object Java `RegisterRequest`.

Dòng này:

```java
authService.register(registerRequest)
```

gọi service xử lý đăng ký.

Dòng này:

```java
writeJson(response, HttpServletResponse.SC_CREATED, ...)
```

trả JSON với status:

```text
201 Created
```

## 9. handleLogin()

```java
private void handleLogin(HttpServletRequest request, HttpServletResponse response)
        throws IOException, AuthException, SQLException {
    LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
    writeJson(response, HttpServletResponse.SC_OK, authService.login(loginRequest));
}
```

Method này xử lý `POST /api/auth/login`.

Dòng này:

```java
LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
```

đọc JSON body và đổi thành object `LoginRequest`.

Ví dụ body:

```json
{
  "usernameOrEmail": "trac",
  "password": "123456"
}
```

Dòng này:

```java
authService.login(loginRequest)
```

gọi service xử lý đăng nhập.

Nếu thành công, trả JSON status:

```text
200 OK
```

## 10. handleMe()

```java
private void handleMe(HttpServletRequest request, HttpServletResponse response)
        throws IOException, AuthException, SQLException {
    writeJson(response, HttpServletResponse.SC_OK, authService.getCurrentUser(getBearerToken(request)));
}
```

Method này xử lý `GET /api/auth/me`.

`getBearerToken(request)` lấy token từ header.

```java
authService.getCurrentUser(...)
```

gọi service để kiểm tra token và lấy thông tin user.

Nếu thành công, trả JSON status:

```text
200 OK
```

## 11. getPath()

```java
private String getPath(HttpServletRequest request) {
    String pathInfo = request.getPathInfo();
    return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
}
```

`request.getPathInfo()` lấy path sau servlet mapping.

Vì servlet được map:

```text
/api/auth/*
```

nên:

```text
/api/auth/register -> /register
/api/auth/login -> /login
/api/auth/me -> /me
```

Dòng này:

```java
return pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
```

nghĩa là:

- Nếu `pathInfo` null hoặc rỗng thì trả `/`.
- Nếu có path thì trả chính path đó.

Đây là toán tử 3 ngôi:

```java
condition ? valueIfTrue : valueIfFalse
```

## 12. getBearerToken()

```java
private String getBearerToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
        return null;
    }

    return authorization.substring(BEARER_PREFIX.length()).trim();
}
```

Method này lấy token từ header.

Ví dụ header:

```text
Authorization: Bearer abc.xyz.123
```

Dòng này lấy toàn bộ header:

```java
String authorization = request.getHeader("Authorization");
```

Dòng này kiểm tra header có hợp lệ không:

```java
if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
    return null;
}
```

Nếu thiếu header hoặc không bắt đầu bằng `Bearer ` thì trả `null`.

Dòng này cắt bỏ chữ `Bearer `:

```java
return authorization.substring(BEARER_PREFIX.length()).trim();
```

Kết quả:

```text
abc.xyz.123
```

## 13. getStatusCode()

```java
private int getStatusCode(AuthException exception) {
    return switch (exception.getCode()) {
        case "INVALID_REQUEST",
                "INVALID_USERNAME",
                "INVALID_EMAIL",
                "INVALID_PASSWORD",
                "INVALID_DISPLAY_NAME" -> HttpServletResponse.SC_BAD_REQUEST;
        case "INVALID_CREDENTIALS",
                "MISSING_TOKEN",
                "INVALID_TOKEN" -> HttpServletResponse.SC_UNAUTHORIZED;
        case "USER_BANNED" -> HttpServletResponse.SC_FORBIDDEN;
        case "USERNAME_EXISTS",
                "EMAIL_EXISTS" -> HttpServletResponse.SC_CONFLICT;
        case "USER_NOT_FOUND" -> HttpServletResponse.SC_NOT_FOUND;
        default -> HttpServletResponse.SC_BAD_REQUEST;
    };
}
```

Method này đổi `AuthException.code` thành HTTP status.

Ví dụ:

```text
INVALID_USERNAME -> 400 Bad Request
INVALID_CREDENTIALS -> 401 Unauthorized
USER_BANNED -> 403 Forbidden
USERNAME_EXISTS -> 409 Conflict
USER_NOT_FOUND -> 404 Not Found
```

Mục đích: `AuthService` chỉ cần ném code lỗi, còn `AuthServlet` quyết định HTTP status.

## 14. writeJson()

```java
private void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
    response.setStatus(statusCode);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(CONTENT_TYPE_JSON);
    objectMapper.writeValue(response.getWriter(), body);
}
```

Method này trả JSON response.

```java
response.setStatus(statusCode);
```

Set HTTP status.

```java
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
```

Set UTF-8.

```java
response.setContentType(CONTENT_TYPE_JSON);
```

Báo response là JSON.

```java
objectMapper.writeValue(response.getWriter(), body);
```

Chuyển object Java thành JSON và ghi vào response.

## 15. writeError()

```java
private void writeError(HttpServletResponse response, int statusCode, String code, String message)
        throws IOException {
    writeJson(response, statusCode, new ErrorResponse(code, message));
}
```

Method này dùng để trả lỗi.

Nó tạo object:

```java
new ErrorResponse(code, message)
```

rồi gọi `writeJson()` để trả JSON.

Ví dụ:

```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Username/email or password is incorrect"
}
```

## 16. Tóm tắt ngắn

`AuthServlet` làm 4 việc chính:

```text
1. Nhận request HTTP
2. Chọn endpoint bằng path
3. Đọc JSON/header rồi gọi AuthService
4. Trả JSON thành công hoặc JSON lỗi
```

Nó không trực tiếp:

```text
hash password
tạo JWT
query database
validate toàn bộ business rule
```

Những việc đó nằm ở `AuthService`, `PasswordUtil`, `JwtUtil`, và `UserRepository`.
