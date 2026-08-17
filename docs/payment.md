# MelodyHub — Payment & Premium Tier Plan

## Goal

Add a **paid Premium tier** to MelodyHub. Free users get the base experience;
paying users unlock premium-only features. Payment is done via a **QR bank
transfer** (your own VietQR / bank QR) — the buyer scans, transfers with a
unique note, and the order is confirmed to activate a **time-limited** premium
subscription (e.g. 1 month).

This plan also becomes an in-repo doc at `md/PREMIUM.md` (first task below) so
the phasing/flow lives with the code.

---

## Decisions (from user)

- **Payment method**: Display a QR code for bank transfer. Recommended:
  **VietQR dynamic image** (`img.vietqr.io`) which bakes the amount + transfer
  note into the QR — no secrets, no SDK. Fallback: a **pasted static QR image
  URL** (your own), with the amount + note shown as text to type manually.
- **Premium model**: **Time-limited** — `users.premium_until` datetime.
  `isPremium = premium_until != null && premium_until > now`. Expiry auto-drops
  to free (checked on read, no cron needed).
- **Gated features**: synced lyrics (karaoke), unlimited playlists, downloads/
  offline, premium badge + no-ads.

## Open item resolved by design

Since there is no real gateway callback, confirmation is **manual/admin**:
the buyer marks "I've paid", an ADMIN verifies the transfer in their bank and
confirms → premium activates. A dev-only `payment.auto-confirm=true` flag lets
you demo the full flow without the admin step.

---

## Data model changes (`backend/.../db/schema.sql`)

1. `users` — add column:
   ```sql
   premium_until DATETIME NULL AFTER status
   ```
2. New table `payment_orders`:
   ```sql
   CREATE TABLE payment_orders (
     id            BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id       INT NOT NULL,
     plan_code     VARCHAR(32) NOT NULL,          -- e.g. MONTHLY, QUARTERLY
     amount        INT NOT NULL,                   -- VND, integer
     currency      VARCHAR(8) NOT NULL DEFAULT 'VND',
     premium_days  INT NOT NULL,                   -- days added on confirm
     transfer_note VARCHAR(64) NOT NULL UNIQUE,    -- e.g. MHUB-000123
     status        VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING|CONFIRMED|REJECTED|EXPIRED
     confirmed_by  INT NULL,                        -- admin user id
     confirmed_at  DATETIME NULL,
     created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
     INDEX idx_payment_user (user_id),
     INDEX idx_payment_status (status)
   );
   ```
   (Requires `docker compose down -v && up --build` to re-init MySQL.)

---

## Config keys (`application.properties` + Docker `CATALINA_OPTS`)

```
payment.plans=MONTHLY:29000:30,QUARTERLY:79000:90   # code:amountVND:days
payment.currency=VND
payment.auto-confirm=false                          # dev demo shortcut
# QR — choose ONE style:
payment.qr.vietqr.bank=970422        # e.g. MB Bank BIN (VietQR)
payment.qr.vietqr.account=0123456789
payment.qr.vietqr.account-name=NGUYEN VAN A
payment.qr.static-image-url=          # OR paste your own QR image URL here
```
No new secrets committed. Existing dev secrets stay untouched.

---

## Backend changes

**Entity/DTO**
- `User.java` — add `LocalDateTime premiumUntil;` (positional constructor +
  `mapRow` order updated), add helper `public boolean isPremium()`.
- `UserResponse.java` — add `premiumUntil` + `boolean premium` (from
  `user.isPremium()`) so the frontend/auth store can gate on it. `fromEntity`
  updated positionally.
- `UserRepository` — include `premium_until` in SELECT/mapRow; add
  `updatePremiumUntil(userId, LocalDateTime)`.

**Payment layer**
- `entity/PaymentOrder.java` + `PaymentStatus` enum.
- `repository/PaymentRepository.java` — `create`, `findById`, `findByNote`,
  `findByUser(page)`, `findPending(page)`, `updateStatus(...)` (PreparedStatement
  + try-with-resources).
- `service/payment/PaymentService.java`:
  - `createOrder(userId, planCode)` → validates plan, generates
    `transfer_note = MHUB-<zero-padded id>`, returns order + QR payload.
  - `getOrder(userId, orderId)` / `listMyOrders`.
  - `markPaid(userId, orderId)` → no-op state (stays PENDING) OR auto-confirm
    when `payment.auto-confirm=true`.
  - `confirm(adminId, orderId)` → sets CONFIRMED, extends
    `premium_until = max(now, premium_until) + premium_days`.
  - `reject(adminId, orderId)`.
  - `buildQr(order)` → VietQR image URL (amount+addInfo=note) or static URL.
- `dto/response/PaymentOrderResponse.java` + `dto/request/CreateOrderRequest`.

**Controller** `controller/payment/PaymentServlet.java` (extends JsonServlet),
mapped `/api/payments/*` in `web.xml`:
- `POST /api/payments/orders`        — create order (auth) → returns order + QR + note.
- `GET  /api/payments/orders`        — my orders (auth).
- `GET  /api/payments/orders/{id}`   — order status (auth, owner) — frontend polls this.
- `POST /api/payments/orders/{id}/paid`    — buyer "I've transferred" (auth, owner).
- `GET  /api/payments/admin/pending`       — list pending (ADMIN).
- `POST /api/payments/admin/orders/{id}/confirm` — confirm (ADMIN).
- `POST /api/payments/admin/orders/{id}/reject`  — reject (ADMIN).

**Entitlement enforcement**
- `AuthorizationService.requirePremium(token)` → loads user, throws
  `AuthException("PREMIUM_REQUIRED")` (maps to **402 Payment Required**) if not
  premium. Add code→status mapping in the relevant servlet(s).
- Gate points:
  - `SongServlet.handleGetLyrics` — synced (timestamped) lyrics require premium;
    plain lyrics stay free.
  - `PlaylistService.create` — free limit **3** playlists (add
    `countByUser`); premium unlimited → throws `PLAYLIST_LIMIT` (403) for free.
  - Download endpoint — **deferred** (download is not really implemented yet;
    gate when built).

---

## Frontend changes

- `services/paymentService.js` — createOrder, getOrder, listMine, markPaid,
  adminPending, adminConfirm, adminReject.
- `stores/auth.store.js` — expose `isPremium` + `premiumUntil` from
  UserResponse; refresh after confirm.
- `views/PremiumView.vue` (`/premium`) — plan cards → create order → show **QR
  image + amount + transfer note** + "Tôi đã chuyển khoản" button → poll order
  status → success state when CONFIRMED (auth store refreshed).
- `router/index.js` — `/premium` route; add `meta.requiresPremium` support in
  the `beforeEach` guard (redirect free users to `/premium`).
- Gating UI:
  - Synced-lyrics toggle in `SongDetailView` shows a lock + "Upgrade" CTA for
    free users.
  - Playlist "create" shows limit notice at 3 for free users.
  - Premium **badge** on profile/header; a small "no ads" line (no ads exist
    yet — cosmetic placeholder).
- Admin: `views/admin/PaymentAdminView.vue` — pending orders table with
  Confirm/Reject (ADMIN route).
- Nav: "Upgrade to Premium" / "Premium" entry in sidebar/header.

---

## Phasing (do-first → do-later)

**Phase 0 — Foundation** (no user-visible change, everything depends on it)
- `md/PREMIUM.md` doc, schema (`premium_until` + `payment_orders`),
  `User.isPremium`, `UserRepository` premium column, `UserResponse` fields,
  auth store `isPremium`.

**Phase 1 — Payment flow (core ask)**
- Config (plans + QR), PaymentEntity/Repository/Service/Servlet + web.xml,
  `paymentService.js`, `PremiumView.vue` (plans → QR → note → poll → success),
  nav entry. With `auto-confirm=true` you can demo end-to-end immediately.

**Phase 2 — Entitlement enforcement (the "free can't, paid can" ask)**
- `requirePremium` guard (402) + router `requiresPremium`; gate synced lyrics
  and playlist-count limit; premium badge + upgrade CTAs.

**Phase 3 — Admin confirmation + polish**
- Admin pending/confirm/reject endpoints + `PaymentAdminView.vue`; order-expiry
  handling; (deferred) real download endpoint gated to premium.

---

## Verification per phase
- Backend: `mvn clean package` (from `backend/`).
- Frontend: `npm run build` (from `frontend/`).
- Manual: create order → QR shows note → confirm (auto or admin) → user becomes
  premium → gated features unlock; after `premium_until` passes → back to free.

## Constraints honored
- Branch from `main` (new `feat/tracdinh/premium-payment`), merge via PR; no
  direct commits to `main`; commit/push only when you ask.
- No real gateway secrets; committed dev secrets untouched.
- Match existing servlet/service/repository style (JsonServlet, JDBC
  PreparedStatement + try-with-resources, Lombok entities, positional
  fromEntity/mapRow).
