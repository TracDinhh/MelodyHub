# Tài liệu: Remember Me & Forgot Password

## Mục lục
- [1. Remember Me](#1-remember-me)
- [2. Forgot Password Flow](#2-forgot-password-flow)
- [Danh sách files thay đổi](#danh-sách-files-thay-đổi)

---

## 1. Remember Me

### Mục đích
Cho phép user chọn "Remember me" khi login để session được lưu lâu hơn (qua các browser tabs và browser restarts).

### Luồng hoạt động

```
┌─────────────────────────────────────────────────────────────────────┐
│                        REMEMBER ME FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. User tick checkbox "Remember me"                                │
│                    │                                               │
│                    ▼                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ LoginView.vue - loginForm.rememberMe = true                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                    │                                               │
│                    ▼                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ authStore.login(credentials, rememberMe)                     │   │
│  │   ↓                                                         │   │
│  │ saveSession(authResponse, remember)                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                    │                                               │
│          remember = true                                            │
│                    ▼                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ http.js - tokenStorage.set(token, remember)                  │   │
│  │   ↓                                                         │   │
│  │ if (remember) → localStorage.setItem(TOKEN_KEY, token)     │   │
│  │ else          → sessionStorage.setItem(TOKEN_KEY, token)   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Chi tiết thay đổi

#### `frontend/src/services/http.js`
```javascript
// TRƯỚC ĐÂY
export const tokenStorage = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY)
};

// SAU KHI THAY ĐỔI
export const REMEMBER_KEY = 'melodyhub.remember';
export const tokenStorage = {
  key: TOKEN_KEY,
  get: () => sessionStorage.getItem(TOKEN_KEY),           // Ưu tiên sessionStorage trước
  set: (token, remember = false) => {
    const storage = remember ? localStorage : sessionStorage;  // Chọn storage
    storage.setItem(TOKEN_KEY, token);
  },
  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);                   // Clear cả 2
  },
  getPersistent: () => localStorage.getItem(TOKEN_KEY)   // Lấy từ localStorage
};
```

#### `frontend/src/stores/auth.store.js`
```javascript
// Thêm state rememberMe
const rememberMe = ref(readRememberMe());

// saveSession nhận thêm tham số remember
function saveSession(authResponse, remember = false) {
  tokenStorage.set(authResponse.token, remember);
  refreshTokenStorage.set(authResponse.refreshToken, remember);
  if (remember) {
    localStorage.setItem(REMEMBER_KEY, 'true');
  }
}

// login nhận thêm tham số remember
async function login(credentials, remember = false) {
  // ...
  saveSession(authResponse, remember);
}
```

#### `frontend/src/views/LoginView.vue`
```vue
<!-- Thêm checkbox Remember me -->
<div v-if="mode === 'login'" class="flex items-center justify-between">
  <label class="flex cursor-pointer items-center gap-2.5 text-xs text-[#777]">
    <input v-model="loginForm.rememberMe" type="checkbox" class="peer sr-only" />
    <span class="grid size-4 shrink-0 place-items-center rounded border border-white/15 
                 peer-checked:border-[#1DB954] peer-checked:bg-[#1DB954] peer-checked:text-black">
      <Check v-if="loginForm.rememberMe" :size="10" :stroke-width="4" />
    </span>
    <span>Remember me</span>
  </label>
  <RouterLink :to="{ name: 'forgot-password' }" class="text-xs text-[#1DB954] hover:underline">
    Forgot password?
  </RouterLink>
</div>
```

### Storage Strategy

| Remember Me | Token Storage | Refresh Token Storage | Duration |
|-------------|---------------|----------------------|----------|
| `true` | `localStorage` | `localStorage` | Persistent (qua browser restarts) |
| `false` | `sessionStorage` | `sessionStorage` | Tab-only |

---

## 2. Forgot Password Flow

### Mục đích
Cho phép user quên mật khẩu có thể reset password qua email.

### Luồng hoạt động chi tiết

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         FORGOT PASSWORD FLOW                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ════════════════════════════════════════════════════════════════════════    │
│  BƯỚC 1: User yêu cầu reset password                                        │
│  ════════════════════════════════════════════════════════════════════════    │
│                                                                               │
│  /forgot-password                                                            │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ ForgotPasswordView.vue                                                  │  │
│  │ - User nhập email                                                      │  │
│  │ - Gọi: authService.requestPasswordReset(email)                        │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ /api/auth/forgot-password (POST)                                        │  │
│  │ Body: { email: "user@example.com" }                                   │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ AuthServlet.doPost() → handleForgotPassword()                          │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ PasswordResetService.requestReset(email)                               │  │
│  │   1. Validate email                                                    │  │
│  │   2. Tìm user theo email                                               │  │
│  │   3. Generate secure token (32 bytes random, Base64 URL-safe)         │  │
│  │   4. Hash token bằng SHA-256                                           │  │
│  │   5. Lưu token_hash + expires_at vào DB                               │  │
│  │   6. Gửi email với reset link                                          │  │
│  │   7. Return plain token (hoặc null nếu email không tồn tại)           │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ EmailService.sendPasswordResetEmail(email, token)                       │  │
│  │   - Build HTML email với reset link                                     │  │
│  │   - Gửi qua SMTP (nếu configured)                                      │  │
│  │   - Link format: {baseUrl}/reset-password?token={token}                 │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
│  ════════════════════════════════════════════════════════════════════════    │
│  BƯỚC 2: User click link trong email                                        │
│  ════════════════════════════════════════════════════════════════════════    │
│                                                                               │
│  /reset-password?token=abc123...                                             │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ ResetPasswordView.vue                                                  │  │
│  │ - Extract token từ URL query params                                     │  │
│  │ - User nhập password mới + confirm password                             │  │
│  │ - Gọi: authService.resetPassword(token, newPassword)                  │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ /api/auth/reset-password (POST)                                         │  │
│  │ Body: { token: "abc123...", newPassword: "newpass123" }                │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ AuthServlet.doPost() → handleResetPassword()                           │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│       │                                                                      │
│       ▼                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ PasswordResetService.resetPassword(token, newPassword)                  │  │
│  │   1. Validate token và password                                        │  │
│  │   2. Hash token bằng SHA-256                                           │  │
│  │   3. Tìm token trong DB (còn hạn, chưa used)                          │  │
│  │   4. Tìm user theo token.userId                                       │  │
│  │   5. Hash password mới bằng BCrypt                                    │  │
│  │   6. Update password_hash của user                                     │  │
│  │   7. Mark token as used (used_at = NOW)                                │  │
│  │   8. Delete all tokens của user (security)                            │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Security Considerations

1. **Email Enumeration Prevention**: Luôn return success message dù email có tồn tại hay không
2. **Token Storage**: Lưu SHA-256 hash của token, không lưu plain token
3. **Token Expiry**: Token có thời hạn 60 phút (configurable)
4. **One-time Use**: Token chỉ sử dụng được 1 lần
5. **User Session Invalidation**: Xóa tất cả tokens cũ khi user request reset mới

---

## Danh sách files thay đổi

### Frontend

#### 1. `frontend/src/services/http.js`
| Thay đổi | Mô tả |
|----------|-------|
| Thêm `REMEMBER_KEY` constant | Key để lưu remember preference |
| `tokenStorage.set()` | Thêm tham số `remember`, chọn localStorage hoặc sessionStorage |
| `tokenStorage.clear()` | Clear cả sessionStorage và localStorage |
| `tokenStorage.getPersistent()` | Method mới để lấy token từ localStorage |
| `refreshTokenStorage` | Tương tự như tokenStorage |
| `refreshSession()` | Sửa để lấy refreshToken từ localStorage nếu sessionStorage rỗng |

#### 2. `frontend/src/stores/auth.store.js`
| Thay đổi | Mô tả |
|----------|-------|
| Thêm `REMEMBER_KEY` | Import từ http.js |
| `readRememberMe()` | Function đọc remember preference từ localStorage |
| `rememberMe` state | Reactive state cho remember checkbox |
| `saveSession()` | Thêm tham số `remember`, lưu preference vào localStorage |
| `login()` | Truyền remember parameter xuống saveSession |

#### 3. `frontend/src/services/authService.js`
| Thay đổi | Mô tả |
|----------|-------|
| Thêm `requestPasswordReset(email)` | Gọi API request password reset |
| Thêm `resetPassword(token, newPassword)` | Gọi API reset password |
| Sửa `logout()` | Lấy refreshToken từ cả 2 storage |

#### 4. `frontend/src/views/LoginView.vue`
| Thay đổi | Mô tả |
|----------|-------|
| Import `KeyRound` icon | Icon cho password fields (tùy chọn) |
| Thêm `loginForm.rememberMe` | State cho checkbox |
| Thêm "Remember me" checkbox | UI cho remember me |
| Thêm "Forgot password?" link | Link đến trang forgot password |
| Sửa `submit()` | Truyền remember value khi login |

#### 5. `frontend/src/views/ForgotPasswordView.vue` (NEW)
| Mục | Mô tả |
|-----|-------|
| Component mới | Trang yêu cầu reset password |
| Step 1: Request form | Input email + nút gửi |
| Step 2: Success message | Thông báo đã gửi email |
| Validation | Kiểm tra email format |
| Error handling | Hiển thị lỗi từ API |

#### 6. `frontend/src/views/ResetPasswordView.vue` (NEW)
| Mục | Mô tả |
|-----|-------|
| Component mới | Trang đặt lại password |
| Extract token từ URL | `route.query.token` |
| Password strength indicator | Visual feedback cho password strength |
| Validation | Minimum 6 characters, passwords must match |
| Success state | Thông báo thành công + link về login |

#### 7. `frontend/src/router/index.js`
| Thay đổi | Mô tả |
|----------|-------|
| Thêm route `/forgot-password` | Đến ForgotPasswordView |
| Thêm route `/reset-password` | Đến ResetPasswordView |

---

### Backend

#### 1. `backend/pom.xml`
| Thêm dependency | Mục đích |
|-----------------|----------|
| `jakarta.mail` | Gửi email SMTP |

```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>2.0.1</version>
</dependency>
```

#### 2. `backend/src/main/java/com/melodyHub/entity/PasswordResetToken.java` (NEW)
| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Integer | Primary key |
| `userId` | Integer | Foreign key đến users |
| `token` | String | SHA-256 hash của token |
| `expiresAt` | LocalDateTime | Thời điểm hết hạn |
| `createdAt` | LocalDateTime | Thời điểm tạo |

#### 3. `backend/src/main/java/com/melodyHub/dto/request/ForgotPasswordRequest.java` (NEW)
```java
public record ForgotPasswordRequest(String email) {}
```

#### 4. `backend/src/main/java/com/melodyHub/dto/request/ResetPasswordRequest.java` (NEW)
```java
public record ResetPasswordRequest(String token, String newPassword) {}
```

#### 5. `backend/src/main/resources/db/schema.sql`
```sql
-- Thêm table mới
CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    token_hash  CHAR(64) NOT NULL,          -- SHA-256 hash
    expires_at  DATETIME(6) NOT NULL,
    used_at     DATETIME(6) NULL,           -- NULL = chưa dùng
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id, expires_at);
```

#### 6. `backend/src/main/java/com/melodyHub/repository/PasswordResetTokenRepository.java` (NEW)
| Method | Mô tả |
|--------|-------|
| `create(token)` | Tạo record mới |
| `findValidByTokenHash(hash)` | Tìm token chưa hết hạn, chưa used |
| `markUsed(id)` | Đánh dấu token đã sử dụng |
| `deleteExpired()` | Xóa các tokens hết hạn |
| `deleteByUserId(userId)` | Xóa tất cả tokens của user |

#### 7. `backend/src/main/java/com/melodyHub/repository/UserRepository.java`
| Thêm method | Mô tả |
|-------------|--------|
| `updatePassword(userId, passwordHash)` | Update password_hash của user |

```java
public void updatePassword(int userId, String passwordHash) throws SQLException {
    String sql = """
        UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = ?
        """;
    // ... execute
}
```

#### 8. `backend/src/main/java/com/melodyHub/service/EmailService.java` (NEW)
| Method | Mô tả |
|--------|-------|
| `getInstance()` | Singleton pattern |
| `sendPasswordResetEmail(toEmail, token)` | Gửi email reset password |
| `buildPasswordResetHtml()` | Tạo HTML email với styling |
| `sendHtmlEmail()` | Gửi email qua SMTP |

**Email template**: Sử dụng HTML với styling matching MelodyHub branding (dark theme, green accent #1DB954)

#### 9. `backend/src/main/java/com/melodyHub/service/auth/PasswordResetService.java` (NEW)
| Method | Mô tả |
|--------|-------|
| `requestReset(email)` | Tạo và lưu token, gửi email |
| `resetPassword(token, newPassword)` | Validate token, update password |
| `generateSecureToken()` | Tạo 32 bytes random, Base64 encoded |
| `hashToken()` | SHA-256 hash |

#### 10. `backend/src/main/java/com/melodyHub/controller/auth/AuthServlet.java`
| Thêm | Mô tả |
|------|-------|
| Import `ForgotPasswordRequest` | DTO cho request |
| Import `ResetPasswordRequest` | DTO cho request |
| Import `PasswordResetService` | Service mới |
| `passwordResetService` field | Instance của service |
| `init()` | Khởi tạo passwordResetService |
| `/forgot-password` case | Xử lý request reset |
| `/reset-password` case | Xử lý reset password |
| `handleForgotPassword()` | Handler method |
| `handleResetPassword()` | Handler method |

---

## Configuration

### Email Settings (`application.properties`)

```properties
# SMTP Configuration
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your-email@gmail.com
smtp.password=your-app-password

# Email sender
smtp.from.address=noreply@melodyhub.com
smtp.from.name=MelodyHub

# App settings
app.base-url=http://localhost:5173

# Password reset
auth.password-reset.expiry-minutes=60
```

### Ví dụ Gmail SMTP
Với Gmail, cần tạo App Password:
1. Enable 2-Factor Authentication
2. Go to Google Account → Security → App passwords
3. Generate new app password for "Mail"
4. Use that password in `smtp.password`

### Development Mode
Nếu không có SMTP config, service sẽ log token ra console:
```
[EmailService] SMTP not configured. Token for user@example.com: abc123...
```

Reset link format: `http://localhost:5173/reset-password?token=abc123...`

---

## API Endpoints

### POST `/api/auth/forgot-password`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success - 200):**
```json
{
  "message": "If an account exists with that email, a reset link has been sent."
}
```

**Response (Error - 400):**
```json
{
  "code": "INVALID_EMAIL",
  "message": "Email is invalid"
}
```

### POST `/api/auth/reset-password`

**Request:**
```json
{
  "token": "abc123...",
  "newPassword": "newpassword123"
}
```

**Response (Success - 200):**
```json
{
  "message": "Password has been reset successfully."
}
```

**Response (Error - 400):**
```json
{
  "code": "INVALID_TOKEN",
  "message": "Reset token is invalid or has expired"
}
```

**Response (Error - 400):**
```json
{
  "code": "INVALID_PASSWORD",
  "message": "Password must be at least 6 characters"
}
```
