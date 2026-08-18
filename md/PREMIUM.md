# Premium payments

MelodyHub Premium uses a self-service VietQR flow. The user chooses a plan,
transfers the displayed amount using its unique `MHUB-` transfer note, then
selects **I've completed the transfer**. The order is immediately marked
`CONFIRMED` and `users.premium_until` is extended by the plan duration; no Admin
action is required.

Premium is evaluated when read, so an expired `premium_until` automatically
returns the account to the free tier. The active premium-only functions are
synced lyrics (and lyric cards) and creating more than three playlists.

The API contract is maintained in [payment.md](../docs/payment.md).
