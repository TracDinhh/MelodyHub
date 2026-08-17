# Giải thích thay đổi Premium Payment

Nhóm code hiện tại thêm tính năng mua gói Premium bằng QR chuyển khoản, admin xác
nhận thanh toán và mở khóa một số chức năng cho tài khoản Premium.

Kiến trúc vẫn đi theo các tầng có sẵn của dự án:

```text
Vue View → Frontend Service → Servlet → Service → Repository → MySQL
```

- **View** quản lý giao diện và trạng thái loading.
- **Frontend Service** gom các URL API.
- **Servlet** nhận HTTP request, kiểm tra đăng nhập và quyền.
- **Service** chứa luật nghiệp vụ thanh toán.
- **Repository** chạy câu SQL và map dữ liệu.

## Luồng chạy chính

```text
Người dùng mở /premium và chọn gói
→ PremiumView gọi paymentService
→ POST /api/payments/orders
→ PaymentServlet xác thực người dùng
→ PaymentService kiểm tra gói và tìm order PENDING của user
→ Có order đang chờ: trả lại order cũ; chưa có: tạo order mới
→ Backend trả QR + nội dung chuyển khoản
→ Admin mở /admin/payments và xác nhận order
→ Order chuyển thành CONFIRMED
→ users.premium_until được cộng thêm số ngày của gói
→ Frontend tải lại thông tin user
→ authStore.isPremium trở thành true
```

## Luồng tạo order chi tiết

Khi người dùng chọn gói trong `PremiumView.vue`:

1. `choosePlan()` gọi `paymentService.createOrder(plan.code)`.
2. Frontend gửi `POST /api/payments/orders` với body:

   ```json
   { "planCode": "MONTHLY" }
   ```

3. `PaymentServlet` lấy Bearer token và gọi `requireAuthenticated()`.
4. `PaymentService.createOrder()` đọc danh sách plan từ cấu hình, ví dụ:

   ```properties
   payment.plans=MONTHLY:29000:30,QUARTERLY:79000:90
   ```

   Mỗi phần lần lượt là `mã gói : số tiền : số ngày Premium`.

5. Repository tìm order `PENDING` gần nhất của user. Nếu đã có, service trả lại
   chính order đó, kể cả khi người dùng tải lại trang hoặc bấm một gói khác.
6. Nếu chưa có order chờ, repository tạo order mới và lấy ID tự tăng từ database.
7. Từ ID đó, service tạo nội dung chuyển khoản riêng như `MHUB-000123`.
8. `buildQr()` ghép bank, account, amount và transfer note thành URL VietQR.
9. Response trả về frontend gồm thông tin order và `qrImageUrl` để hiển thị.

`createOrder()` được đồng bộ hóa trên service nên hai request đến gần như cùng lúc
trên cùng một server cũng phải kiểm tra lần lượt. User chỉ tạo thêm order sau khi
order cũ không còn ở trạng thái `PENDING`.

Nếu tài khoản đang còn Premium, backend từ chối tạo order mới bằng HTTP `409` và
code `PREMIUM_ALREADY_ACTIVE`. Trang Premium cũng chỉ hiện trạng thái cùng ngày
hết hạn, không hiện lại lựa chọn thanh toán. Nếu request đến từ một tab cũ,
frontend tải lại user rồi tự chuyển sang màn hình Premium đang hoạt động.

QR chỉ giúp điền sẵn thông tin chuyển khoản. Nó không phải callback từ ngân hàng,
vì vậy backend chưa tự biết người dùng đã chuyển tiền thật hay chưa.

## Các Payment API

| Method | Endpoint | Người gọi | Chức năng |
|---|---|---|---|
| `POST` | `/api/payments/orders` | User | Tạo order mới hoặc trả lại order PENDING hiện có |
| `GET` | `/api/payments/orders` | User | Xem các order của mình |
| `GET` | `/api/payments/orders/{id}` | Chủ order | Xem trạng thái order |
| `POST` | `/api/payments/orders/{id}/paid` | Chủ order | Báo đã chuyển khoản hoặc auto-confirm khi demo |
| `GET` | `/api/payments/admin/pending` | Admin | Lấy danh sách order đang chờ |
| `POST` | `/api/payments/admin/orders/{id}/confirm` | Admin | Xác nhận và cộng ngày Premium |
| `POST` | `/api/payments/admin/orders/{id}/reject` | Admin | Từ chối order |

`PaymentServlet` tự phân tích `pathInfo` để chọn hàm service tương ứng. Những API
admin còn kiểm tra thêm `user.getRole() == ADMIN`.

## Luồng admin xác nhận

`PaymentAdminView.vue` gọi API lấy danh sách `PENDING`. Khi admin bấm Confirm:

1. Frontend gọi endpoint `/confirm`.
2. `PaymentServlet` xác nhận người gọi có role `ADMIN`.
3. `PaymentService.confirm()` tải order và user sở hữu order.
4. Service chỉ cho xử lý order còn `PENDING`.
5. Ngày bắt đầu được tính như sau:

   ```text
   nếu premium_until hiện tại vẫn còn hạn:
       start = premium_until hiện tại
   ngược lại:
       start = thời gian hiện tại

   premium_until mới = start + premium_days
   ```

6. `PaymentRepository` đổi trạng thái thành `CONFIRMED`.
7. `UserRepository` cập nhật `users.premium_until`.
8. `PremiumView` đang poll order mỗi 5 giây. Khi thấy `CONFIRMED`, nó gọi
   `authStore.refreshUser()` để lấy quyền Premium mới.

Việc cộng từ ngày hết hạn cũ giúp người dùng mua thêm gói mà không mất những ngày
Premium còn lại.

## Các file backend chính

- `PaymentServlet.java`: nhận và phân loại các request payment.
- `PaymentService.java`: xử lý tạo order, tạo QR, confirm, reject và cộng ngày Premium.
- `PaymentRepository.java`: đọc/ghi bảng `payment_orders` bằng JDBC.
- `PaymentOrder.java`, `PaymentStatus.java`: model và trạng thái của order.
- `CreateOrderRequest.java`: nhận `planCode` từ frontend.
- `PaymentOrderResponse.java`: dữ liệu order và URL QR trả về frontend.
- `User.java`: thêm `premiumUntil` và hàm `isPremium()`.
- `UserRepository.java`: đọc và cập nhật `premium_until`.
- `AuthorizationService.java`: thêm `requirePremium()` để chặn tài khoản miễn phí.
- `SongServlet.java`: synced lyrics chỉ dành cho Premium.
- `PlaylistService.java`: tài khoản miễn phí chỉ tạo tối đa 3 playlist.
- `PlaylistServlet.java`: trả lỗi `PLAYLIST_LIMIT` khi vượt giới hạn.

## Database và cấu hình

`schema.sql` thêm:

- cột `users.premium_until` để lưu ngày hết hạn Premium;
- bảng `payment_orders` để lưu gói, số tiền, nội dung chuyển khoản và trạng thái.

`application.properties` và `docker-compose.yml` thêm cấu hình giá gói, số ngày,
thông tin VietQR và chế độ tự động xác nhận khi demo.

`web.xml` đăng ký `PaymentServlet` tại `/api/payments/*`.

### Bảng `payment_orders`

Một số cột quan trọng:

- `user_id`: người mua gói;
- `plan_code`: mã gói đã chọn;
- `amount`, `currency`: số tiền thanh toán;
- `premium_days`: số ngày sẽ cộng khi confirm;
- `transfer_note`: nội dung chuyển khoản duy nhất;
- `status`: `PENDING`, `CONFIRMED`, `REJECTED` hoặc `EXPIRED`;
- `confirmed_by`, `confirmed_at`: admin và thời điểm xử lý.

Giá tiền và số ngày được chụp lại vào order. Sau này dù cấu hình plan thay đổi,
order cũ vẫn giữ đúng dữ liệu tại lúc tạo.

## Các file frontend chính

- `paymentService.js`: chứa các request payment.
- `PremiumView.vue`: chọn gói, tạo order, hiển thị QR và kiểm tra trạng thái định kỳ.
- `PaymentAdminView.vue`: admin xem order đang chờ rồi confirm hoặc reject.
- `auth.store.js`: thêm `isPremium`, `premiumUntil` và `refreshUser()`.
- `router/index.js`: thêm route `/premium` và `/admin/payments`.
- `SongDetailView.vue`: hiện khóa nâng cấp nếu user miễn phí xem synced lyrics.
- `PlaylistsView.vue`: hiện cảnh báo giới hạn 3 playlist.
- `LeftSidebar.vue`, `TopHeader.vue`: thêm link và badge Premium.
- `AdminLayout.vue`: thêm menu quản lý payment.

## Cách Premium được kiểm tra

```java
premiumUntil != null && premiumUntil.isAfter(LocalDateTime.now())
```

Khi ngày hết hạn đã qua, tài khoản tự trở về miễn phí mà không cần cron job.

Backend dùng `AuthorizationService.requirePremium(token)` để bảo vệ API. Hàm này:

1. xác thực token và tải user từ database;
2. gọi `user.isPremium()`;
3. ném lỗi `PREMIUM_REQUIRED` nếu tài khoản không còn hạn.

### Những chức năng đang bị giới hạn

- **Synced lyrics:** `SongServlet` trả HTTP 402 cho tài khoản miễn phí.
- **Playlist:** free user tạo tối đa 3 playlist; lần thứ tư trả lỗi
  `PLAYLIST_LIMIT` với HTTP 403.
- **Frontend:** hiển thị nút nâng cấp, badge Premium và màn hình khóa lyrics.

Việc ẩn giao diện ở frontend chỉ phục vụ UX. Kiểm tra ở backend mới là phần bảo
mật chính vì người dùng vẫn có thể tự gửi HTTP request.

## Trạng thái trên frontend

`UserResponse` trả xuống hai trường:

```json
{
  "premiumUntil": "2026-09-16T10:00:00",
  "premium": true
}
```

`auth.store.js` chuyển chúng thành:

- `isPremium`: dùng để bật/tắt giao diện;
- `premiumUntil`: dùng khi cần hiển thị ngày hết hạn;
- `refreshUser()`: tải lại user sau khi order được confirm.

## Các trường hợp có thể xảy ra khi tự động xác nhận

| Trường hợp | Vấn đề có thể xảy ra | Cách xử lý nên dùng |
|---|---|---|
| Người dùng bấm mua nhiều lần | Tạo nhiều order PENDING, admin khó đối soát | Chỉ cho một order PENDING còn hạn hoặc dùng idempotency key |
| Người dùng double-click | Hai request chạy gần như cùng lúc | Disable nút ở UI và chặn trùng ở database/backend |
| Request tạo order timeout | Frontend retry và tạo order thứ hai | Retry bằng cùng idempotency key để nhận lại kết quả cũ |
| Mở hai tab hoặc hai thiết bị | Mỗi nơi tạo một order | Backend kiểm tra active order, không chỉ dựa vào trạng thái UI |
| Người dùng bấm “đã trả” nhưng chưa chuyển | Nếu tin frontend sẽ cấp Premium miễn phí | Chỉ cấp quyền từ webhook/provider hoặc admin xác nhận |
| Người dùng đóng tab sau khi chuyển | Trang success không chạy | Webhook vẫn cập nhật order ở server, không phụ thuộc trình duyệt |
| Webhook gửi lại nhiều lần | Premium bị cộng ngày nhiều lần | Lưu event/reference UNIQUE và xử lý idempotent |
| Hai webhook đến đồng thời | Cả hai cùng thấy order PENDING | Lock row hoặc conditional update trong transaction |
| Webhook đến sai thứ tự | Event cũ ghi đè trạng thái mới | Chỉ cho phép state transition hợp lệ và đối chiếu trạng thái provider |
| Webhook giả mạo | Kẻ xấu tự gửi request để lấy Premium | Kiểm tra HMAC/signature trước khi đọc dữ liệu |
| Đúng note nhưng sai số tiền | Cấp sai gói | So sánh order code, amount, currency và trạng thái thành công |
| Chuyển thiếu hoặc dư tiền | Không biết cấp gói nào | Đưa order vào trạng thái cần kiểm tra, không tự confirm |
| Chuyển khoản sau khi order hết hạn | Order đã EXPIRED nhưng tiền vẫn đến | Ghi nhận giao dịch, chuyển sang hàng đợi đối soát thủ công |
| Admin và webhook confirm cùng lúc | Có thể cộng Premium hai lần | Một transaction, một state transition PENDING → CONFIRMED |
| Order CONFIRMED nhưng update user lỗi | Đã nhận tiền nhưng user vẫn free | Update order và entitlement trong cùng transaction |
| Backend webhook tạm thời bị down | Provider retry, có thể gửi trùng | Trả 2xx sau khi lưu event; xử lý bằng queue và cho phép retry |
| Frontend vẫn thấy tài khoản free | Auth store đang giữ user cũ | Refresh `/api/auth/me` hoặc phát sự kiện cập nhật session |
| Người dùng đã Premium mua thêm | Có thể mất ngày còn lại | Cộng từ `max(now, premium_until)` như code hiện tại |
| Hoàn tiền hoặc chargeback | User vẫn giữ Premium dù tiền đã trả lại | Có trạng thái refund và quy tắc thu hồi/điều chỉnh entitlement |
| Sai múi giờ hoặc clock server | Premium hết hạn sớm/muộn | Lưu UTC và đồng bộ clock server |

## Các ứng dụng lớn thường làm như thế nào?

Luồng phổ biến là tách **payment state** khỏi **feature entitlement**:

```text
Client tạo checkout/order
→ Payment provider xử lý tiền
→ Provider gửi signed webhook
→ Webhook được lưu theo event ID/reference UNIQUE
→ Worker kiểm tra order + amount + currency
→ Transaction đổi order thành PAID/CONFIRMED
→ Cập nhật subscription hoặc entitlement
→ Frontend đọc entitlement mới từ backend
```

Một số nguyên tắc thường gặp:

1. **Không tin client/return URL.** Người dùng có thể đóng tab hoặc giả request.
2. **Webhook là nguồn xác nhận chính.** payOS gửi kết quả thanh toán đến webhook của merchant;
   payload có `orderCode`, `amount`, `reference`, `paymentLinkId` và `signature`.
3. **Xác minh chữ ký.** payOS hướng dẫn kiểm tra signature bằng HMAC-SHA256 với
   checksum key; webhook sai chữ ký phải bị từ chối.
4. **Idempotency cho create/update.** Stripe khuyên dùng idempotency key để retry
   request mà không tạo thêm object hoặc side effect.
5. **Chấp nhận webhook gửi trùng.** Stripe nói cùng event có thể được giao nhiều lần;
   hệ thống cần lưu event ID đã xử lý và bỏ qua lần sau.
6. **Xử lý nhanh, bất đồng bộ.** Webhook nên xác minh, lưu event, trả 2xx sớm rồi
   chuyển nghiệp vụ nặng sang queue/worker.
7. **Entitlement là nguồn quyền nội bộ.** Stripe mô tả việc cập nhật access expiry
   trong database khi nhận sự kiện thanh toán thành công; feature đọc quyền này thay
   vì gọi provider ở mọi request.
8. **Có reconciliation.** Job định kỳ so sánh order nội bộ với provider để sửa các
   trường hợp webhook bị mất hoặc hệ thống lỗi giữa chừng.

Nguồn chính thức:

- [Stripe — Webhook best practices](https://docs.stripe.com/webhooks#best-practices)
- [Stripe — Idempotent requests](https://docs.stripe.com/api/idempotent_requests)
- [Stripe — Subscription webhooks và cập nhật access expiry](https://docs.stripe.com/billing/subscriptions/webhooks)
- [Stripe — Entitlements](https://docs.stripe.com/billing/entitlements)
- [payOS — Payment webhook](https://payos.vn/docs/du-lieu-tra-ve/webhook/)
- [payOS — Kiểm tra webhook signature](https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/)

## Hướng phù hợp cho MelodyHub

### Giai đoạn 1 — Giảm request rác ngay

- Đã triển khai: khi user có order `PENDING`, trả lại order đó thay vì tạo mới.
- Đã chặn hai request đồng thời trong cùng một instance backend.
- Thêm `expires_at` cho order và job đổi order cũ sang EXPIRED.
- Disable nút chọn gói khi request đang chạy hoặc đã có order chờ.
- Thêm idempotency key cho API tạo order.

### Giai đoạn 2 — Tự động xác nhận thật

- Tích hợp payment request của payOS thay vì chỉ ghép ảnh VietQR.
- Thêm endpoint webhook công khai.
- Xác minh signature bằng checksum key.
- Đối chiếu `orderCode`, `amount`, `currency` và trạng thái thành công.
- Dùng transaction để confirm order và cập nhật `premium_until`.
- Lưu transaction `reference` hoặc event ID UNIQUE để chống xử lý trùng.

### Giai đoạn 3 — Vận hành ổn định

- Queue xử lý webhook và retry có kiểm soát.
- Job reconciliation với provider.
- Dashboard cho order sai tiền, quá hạn, refund và webhook lỗi.
- Audit log ghi ai hoặc event nào đã cấp Premium.

## Một số lưu ý

- QR chỉ chứa thông tin chuyển khoản, chưa tự xác minh tiền từ ngân hàng.
- Admin phải confirm thì `premium_until` mới được cập nhật.
- Endpoint `markPaid` đã có nhưng giao diện hiện chưa gọi tới.
- Update trạng thái order và update ngày Premium đang là hai thao tác database riêng.
- Database cũ cần migration hoặc tạo lại volume dev để có schema mới.
- `EXPIRED` đã có trong enum/schema nhưng chưa thấy luồng tự đổi order sang trạng thái này.
- Guard router `requiresPremium` đã được viết nhưng hiện chưa có route cụ thể sử dụng.

Hai câu update lúc confirm hiện dùng connection riêng:

```text
UPDATE payment_orders → UPDATE users
```

Nếu câu thứ hai lỗi sau khi câu đầu thành công, order có thể đã `CONFIRMED` nhưng
user chưa được cộng Premium. Khi hoàn thiện production nên đặt hai thao tác trong
cùng một database transaction.

## Kiểm tra build

```bash
cd backend && mvn test
cd ../frontend && npm run build
```
