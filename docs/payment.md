# MelodyHub Premium payment flow

## Current behaviour

Premium is activated directly by the purchaser. After completing a VietQR bank
transfer, they press **I've completed the transfer** on `/premium`. The backend
changes their pending order to `CONFIRMED` and extends `users.premium_until`
immediately. Admins do not review or confirm payment orders.

```text
Choose plan → receive QR and transfer note → transfer with banking app
→ press “I've completed the transfer” → Premium activated immediately
```

## API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payments/orders` | Create or resume the caller's pending order. |
| `GET` | `/api/payments/orders` | List the caller's orders. |
| `GET` | `/api/payments/orders/{id}` | Read an order owned by the caller. |
| `POST` | `/api/payments/orders/{id}/paid` | Confirm the caller has transferred money and activate Premium immediately. |

`POST /api/payments/orders/{id}/paid` is idempotent for an already confirmed
order: it returns the confirmed order and does not add Premium days a second
time.

## Premium entitlements

- Synced karaoke lyrics and lyric-card creation.
- More than three playlists (free accounts may create three).
- Premium badge and ad-free listening presentation.

The backend enforces lyrics and playlist limits. The frontend displays a Premium
upgrade popup when a free user tries to use either gated feature.

## Configuration

```properties
payment.plans=MONTHLY:29000:30,QUARTERLY:79000:90
payment.currency=VND
payment.qr.vietqr.bank=970422
payment.qr.vietqr.account=0123456789
payment.qr.vietqr.account-name=NGUYEN VAN A
payment.qr.static-image-url=
```

## Important limitation

This self-service implementation trusts the user's **I've completed the
transfer** action; it does not verify a real bank transaction. Before a public
or paid deployment, replace that endpoint behaviour with a verified payment
provider webhook or bank-transaction reconciliation.
