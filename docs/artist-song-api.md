# Artist Song API

Tasks 4.3 and 4.4 expose an authenticated Artist's private Song catalog. A Song
belongs to the current Artist only when `song_artists` links it to that Artist
with the `MAIN` role. Draft, Published, and Hidden Songs are included;
soft-deleted Songs are excluded.

## List Own Songs

```text
GET /api/artist/songs?page=1&size=20
Authorization: Bearer <token>
```

`page` defaults to `1`. `size` defaults to `20` and cannot exceed `50`. Results
are ordered by `createdAt` descending, then `id` descending.

```bash
curl -i \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  "http://localhost:8080/api/artist/songs?page=1&size=20"
```

A successful response uses the existing paging wrapper:

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

Invalid pagination returns `400 INVALID_QUERY_PARAM`:

```bash
curl -i \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  "http://localhost:8080/api/artist/songs?page=0"
```

## View One Own Song

The identifier may be a positive numeric Song id or a Song slug. Numeric input
tries the id first and falls back to slug, allowing numeric slugs when no owned id
matches.

```text
GET /api/artist/songs/{id-or-slug}
Authorization: Bearer <token>
```

```bash
curl -i \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  http://localhost:8080/api/artist/songs/42
```

```bash
curl -i \
  -H "Authorization: Bearer ${ARTIST_TOKEN}" \
  http://localhost:8080/api/artist/songs/my-draft-song
```

A missing, soft-deleted, or another Artist's Song returns the same `404` body so
the endpoint does not reveal ownership:

```json
{
  "code": "SONG_NOT_FOUND",
  "message": "Song was not found"
}
```

Missing or invalid tokens return `401`; authenticated non-Artist accounts return
`403 FORBIDDEN`; and an Artist account without an active linked profile returns
`404 ARTIST_PROFILE_NOT_FOUND`.

These live HTTP requests, including the ownership and soft-delete SQL behavior,
are intentionally left for manual verification. Local service tests cover
status-preserving paging, offset bounds, and id/slug resolution while checking
that each operation delegates to the ownership-aware repository method.
