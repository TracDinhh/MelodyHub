# Song Backend MVP

## Problem Statement

MelodyHub has working authentication and a MySQL-backed music schema, but the backend does not yet expose a usable Song API. A user can register and log in, but there is no backend endpoint that returns real Song catalog data from the database for the future frontend to consume.

The project needs a backend-first Song feature that proves the main music catalog read path before adding frontend UI, playlist behavior, likes, upload, playback, or admin tools.

## Solution

Build a read-only Song backend MVP for MelodyHub.

The backend will expose a public Song catalog endpoint that returns Published Songs from the existing MySQL schema. The endpoint will follow the same Java Servlet + service + JDBC repository style as the existing auth implementation. It will return JSON using stable response DTOs and stable error DTOs.

The first backend slice should return Songs that are visible to listeners: status `PUBLISHED` and not soft-deleted. Each Song should include the basic catalog metadata needed by the future frontend: id, title, slug, album id, track number, duration, cover image, lyrics field if present, status, play count, created timestamp, and updated timestamp. Artist and Album expansion can be implemented later if this first backend slice is intentionally kept smaller, but the API shape should not block adding them.

## User Stories

1. As a listener, I want the backend to return published songs, so that the app can show real music catalog data.
2. As a listener, I want hidden songs excluded, so that content that should not be shown stays invisible.
3. As a listener, I want draft songs excluded, so that unfinished content is not visible.
4. As a listener, I want soft-deleted songs excluded, so that removed content stays out of the catalog.
5. As a listener, I want songs ordered consistently, so that the catalog does not jump around between requests.
6. As a listener, I want newer songs first, so that recent catalog additions are easy to find.
7. As a listener, I want each Song to include its title, so that I can identify it.
8. As a listener, I want each Song to include its slug, so that future detail links can use stable URLs.
9. As a listener, I want each Song to include duration in seconds, so that the frontend can show track length.
10. As a listener, I want each Song to include cover image metadata when available, so that the future UI can show artwork.
11. As a listener, I want standalone singles to be valid, so that songs without albums can still appear.
12. As a listener, I want album-linked songs to preserve their album id, so that later album expansion is possible.
13. As a listener, I want each Song to include play count, so that the backend can later support popularity-based views.
14. As a listener, I want missing optional fields to be returned predictably as null, so that clients can render fallback UI.
15. As a backend developer, I want the Song API to use the existing MySQL schema, so that no schema migration is needed for this slice.
16. As a backend developer, I want the Song entity to match the songs table, so that database rows map cleanly into Java objects.
17. As a backend developer, I want a dedicated Song repository, so that song database access is separate from auth database access.
18. As a backend developer, I want a Song service layer, so that catalog visibility rules have a clear home.
19. As a backend developer, I want a Song servlet endpoint, so that routing follows the existing Servlet backend pattern.
20. As a backend developer, I want the endpoint to return JSON, so that it is easy to test with Hoppscotch/Postman/curl.
21. As a backend developer, I want stable error responses, so that frontend work later can handle failures consistently.
22. As a backend developer, I want database errors hidden behind generic API errors, so that SQL details do not leak to clients.
23. As a backend developer, I want CORS to keep working through the existing API filter, so that browser clients can call the endpoint.
24. As a backend developer, I want the endpoint to be public for the MVP, so that catalog browsing does not depend on login.
25. As a tester, I want to call one endpoint and see real published Song rows, so that the feature can be verified end-to-end.
26. As a tester, I want published/draft/hidden/deleted rows covered, so that visibility filtering is proven.
27. As a tester, I want no-album and album-linked rows covered, so that nullable album behavior is proven.
28. As a learner building MelodyHub, I want the backend Song feature to stay small, so that I can understand one layer at a time.
29. As a learner building MelodyHub, I want no frontend work in this slice, so that backend behavior can be tested before UI work starts.
30. As a learner building MelodyHub, I want no upload or admin work in this slice, so that the feature does not become too broad.

## Implementation Decisions

- This is a backend-only Song feature.
- The feature is read-only for the MVP.
- The existing MySQL schema is the source of truth.
- No database schema change is required for this feature.
- The canonical domain term is Song.
- A Published Song is visible in the listener-facing catalog.
- A Song with `status = PUBLISHED` and `deleted_at IS NULL` is visible in this MVP.
- A Song with `status = DRAFT` must not be returned.
- A Song with `status = HIDDEN` must not be returned.
- A Song with non-null `deleted_at` must not be returned.
- Returned Songs must be ordered by `created_at DESC`, then `id DESC`.
- The endpoint will be `GET /api/songs`.
- The endpoint will be public for now and will not require a bearer token.
- The endpoint will return a JSON array or a JSON wrapper containing the list of Songs. Pick one shape and keep it stable for the frontend.
- Song response data should include id, title, slug, album id, track number, duration seconds, cover URL, lyrics, status, play count, created timestamp, and updated timestamp.
- The response should not expose `deleted_at`.
- The response should not expose internal database errors.
- The response should not require client knowledge of table names.
- A Song entity should map the songs table fields.
- A Song repository should use JDBC and the existing database connection style.
- A Song service should hold catalog read rules and call the repository.
- A Song servlet should handle HTTP routing, JSON serialization, and error responses.
- The servlet should follow the same JSON/ObjectMapper style as the auth servlet.
- The servlet should be mapped under the API path using the existing web app routing style.
- The existing CORS filter should continue to apply to the Song endpoint.
- If the current Song repository already exists, update it instead of creating a duplicate repository.
- If a Song status enum already exists, reuse it instead of introducing string constants in several places.
- Artist expansion is not required in the first backend-only slice unless it stays simple.
- Album expansion is not required in the first backend-only slice unless it stays simple.
- Playback file serving is not part of this slice.
- Creating, updating, hiding, deleting, or uploading Songs is not part of this slice.
- Frontend rendering is not part of this slice.

## Testing Decisions

- The highest-value test seam is the HTTP API contract for `GET /api/songs`.
- The first manual verification should be a curl/Hoppscotch request against the running Docker backend.
- A good backend test checks externally visible behavior: HTTP status, content type, JSON shape, ordering, and filtering.
- Tests should not assert private method names or exact SQL string formatting.
- Tests should include at least one published Song.
- Tests should include at least one draft Song and verify it is excluded.
- Tests should include at least one hidden Song and verify it is excluded.
- Tests should include at least one soft-deleted Song and verify it is excluded.
- Tests should include two published Songs with different timestamps and verify newest-first ordering.
- Tests should include same-timestamp Songs if practical and verify id-desc ordering.
- Tests should include a Song without an album and verify nullable album fields do not break JSON serialization.
- Tests should include database error behavior if a practical integration seam exists.
- Build verification should include Maven packaging for the backend.
- Runtime verification should include Docker Compose when available because the project now runs backend + MySQL through Compose.
- If no automated integration-test harness exists yet, document the curl/Hoppscotch requests used for verification and keep the implementation small enough to test manually.

## Out of Scope

- Frontend Song UI.
- Vue API integration.
- Playlist features.
- Song likes.
- Artist follows.
- Listen history.
- Play count incrementing.
- Audio playback.
- Audio file streaming.
- Song upload.
- Cover upload.
- Admin-only Song creation.
- Admin-only Song update.
- Admin-only Song hiding.
- Admin-only Song deletion.
- Artist CRUD.
- Album CRUD.
- Genre browsing.
- Search and filters.
- Recommendations.
- Lyrics synchronization endpoints.
- Authentication changes.
- Role or permission changes.

## Further Notes

- This spec intentionally slices the backend first.
- The implementation should stay close to the current auth architecture: Servlet, service, repository, DTOs, and JSON error responses.
- MelodyHub is currently using MySQL through Docker Compose, so local verification should prefer the Docker stack when checking real API behavior.
- The previous broader Song Catalog MVP included frontend work; this backend spec is the smaller prerequisite slice.
