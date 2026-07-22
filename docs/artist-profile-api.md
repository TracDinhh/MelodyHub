# Artist Profile API

Task 4.1 exposes the current authenticated Artist profile.

## Endpoint

```text
GET /api/artist/profile
Authorization: Bearer <token>
```

A successful response contains `id`, `name`, `slug`, `bio`, `imageUrl`,
`createdAt`, and `updatedAt`. It does not expose the linked User id or the
soft-delete timestamp.

## Verification Requests

Missing token, verified as `401 MISSING_TOKEN`:

```bash
curl -i http://localhost:8080/api/artist/profile
```

Invalid token, verified as `401 INVALID_TOKEN`:

```bash
curl -i \
  -H "Authorization: Bearer invalid-token" \
  http://localhost:8080/api/artist/profile
```

Unknown Artist route, verified as `404 NOT_FOUND`:

```bash
curl -i http://localhost:8080/api/artist/unknown
```

Linked Artist account, expected `200` with the current Artist's profile:

```bash
curl -i \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  http://localhost:8080/api/artist/profile
```

Authenticated non-Artist account, expected `403 FORBIDDEN`:

```bash
curl -i \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  http://localhost:8080/api/artist/profile
```

Unlinked or soft-deleted Artist profile, expected `404
ARTIST_PROFILE_NOT_FOUND`:

```bash
curl -i \
  -H "Authorization: Bearer ${UNLINKED_ARTIST_TOKEN}" \
  http://localhost:8080/api/artist/profile
```

The token-backed `200`, `403`, and profile `404` checks require matching local
database fixtures. They were not run during Task 4.1 because access to inspect or
prepare those local fixtures was not approved.

## Update Profile

Task 4.2 updates the current Artist's editable profile metadata. `name` and
`slug` are required. Blank `bio` or `imageUrl` values clear those optional
fields.

```text
PUT /api/artist/profile
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Son Tung M-TP",
  "slug": "son-tung-mtp",
  "bio": "Vietnamese artist",
  "imageUrl": "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg"
}
```

Linked Artist account, expected `200` with the refreshed profile:

```bash
curl -i -X PUT \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Son Tung M-TP",
    "slug": "son-tung-mtp",
    "bio": "Vietnamese artist",
    "imageUrl": "https://ik.imagekit.io/melodyhub/artists/son-tung-mtp/profile.jpg"
  }' \
  http://localhost:8080/api/artist/profile
```

Missing token, verified as `401 MISSING_TOKEN` when the JSON body is valid:

```bash
curl -i -X PUT \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Artist","slug":"test-artist","bio":null,"imageUrl":null}' \
  http://localhost:8080/api/artist/profile
```

Malformed JSON from an authenticated Artist returns `400 INVALID_JSON`:

```bash
curl -i -X PUT \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":' \
  http://localhost:8080/api/artist/profile
```

Validation failures return `400` with one of these codes:

- `INVALID_REQUEST`
- `INVALID_ARTIST_NAME`
- `INVALID_ARTIST_SLUG`
- `INVALID_ARTIST_BIO`
- `INVALID_ARTIST_IMAGE_URL`

A slug already owned by another Artist returns `409 ARTIST_SLUG_EXISTS`.
Authenticated non-Artist accounts still receive `403 FORBIDDEN`, and missing or
soft-deleted linked profiles receive `404 ARTIST_PROFILE_NOT_FOUND`.

Artist authorization is checked before request-body parsing, so missing or
invalid tokens return `401` even when the JSON body is malformed. An invalid
bearer token was verified as `401 INVALID_TOKEN`. The
token-backed successful update, duplicate-slug, validation, non-Artist, and
missing-profile checks require matching local database fixtures and were not run
because access to inspect or prepare those fixtures was not approved.
