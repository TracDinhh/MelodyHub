# Giải thích tính năng Premium Payment

Luồng Premium hiện tại là **tự kích hoạt**; Admin không duyệt đơn thanh toán.

```text
PremiumView.vue
  → POST /api/payments/orders (tạo order và QR)
  → người dùng chuyển khoản
  → POST /api/payments/orders/{id}/paid
  → PaymentService.markPaid()
  → payment_orders.status = CONFIRMED
  → users.premium_until được cộng số ngày của gói
  → authStore.refreshUser()
  → giao diện Premium được mở khóa
```

`PaymentService.markPaid()` chỉ chuyển order `PENDING` sang `CONFIRMED` một lần.
Nếu trình duyệt gửi lại request sau khi order đã xác nhận, service chỉ trả order
hiện tại, không cộng thêm ngày Premium.

Các chức năng giới hạn Premium hiện có là:

- Lời bài hát đồng bộ và tạo lyric card. Backend trả `402 PREMIUM_REQUIRED`
  cho tài khoản miễn phí; giao diện hiển thị popup nâng cấp.
- Tạo playlist thứ tư. Backend trả `403 PLAYLIST_LIMIT`; giao diện chặn trước
  bằng cùng popup nâng cấp.

Lưu ý: vì QR chuyển khoản không gửi callback ngân hàng vào ứng dụng, nút **I've
completed the transfer** dựa trên lời xác nhận của người dùng. Đây chỉ phù hợp
cho demo/nội bộ. Triển khai thực tế cần webhook đã xác thực từ cổng thanh toán
hoặc bước đối soát giao dịch tự động.
