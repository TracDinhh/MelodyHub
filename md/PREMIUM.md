# Premium payments

MelodyHub Premium uses a VietQR bank-transfer flow. A user selects a plan, receives a QR image and a unique `MHUB-` transfer note, then marks the order as paid. An administrator verifies the bank transfer and confirms the order; `users.premium_until` is extended by the plan duration.

`payment.auto-confirm=true` is a development-only shortcut that confirms an order when the buyer marks it paid. Premium status is evaluated when it is read, so an expired `premium_until` automatically returns the user to the free tier.

The implementation phases and API contract are maintained in [payment.md](../docs/payment.md).
