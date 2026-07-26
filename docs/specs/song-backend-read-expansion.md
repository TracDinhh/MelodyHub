# Song Backend Read Expansion

> **Status:** ready-for-agent
> **Builds on:** [song-backend-mvp.md](song-backend-mvp.md) — the read-only `GET /api/songs`
> list endpoint is already implemented and working.

## Problem Statement

MelodyHub now has a working Song list endpoint (`GET /api/songs`) that returns
every Published Song as a flat JSON array. As a developer building toward a real
music catalog, I can list Songs but I cannot do anything else with them from the
backend:

- I cannot open a single Song by its URL-facing slug, so a future detail page has
  no data to render.
- Each Song in the list carries only its own columns. There is no Artist and no
  Album information, so the catalog cannot show "who performed this" or "what
  release it came from" — the two facts a listener most wants.
- The list returns the entire catalog with no upper bound. As the `songs` table
  grows, one request will return an unbounded payload.
- There is no way to narrow the catalog. I cannot search Songs by title or filter
  them by genre, so browsing does not scale past a handful of rows.

The catalog read path is proven but thin. It needs to become complete enough that
a frontend could be built against it without further backend read work.

The role roadmap also permits an Artist Dashboard only for accounts that are
already ARTISTs, but it does not define a controlled way for a normal USER to
become one. Without that path, Artist access would depend on manual database
changes or a broad Admin role edit, neither of which gives applicants a visible
status or gives review decisions a durable audit trail.

## Solution

Extend the existing Song backend read path — still read-only, still public, still
Servlet + service + JDBC — so that it exposes a complete catalog-browsing API.

Four capabilities are added on top of the existing list endpoint, all under the
existing `/api/songs/*` servlet seam:

1. **Song detail** — `GET /api/songs/{slug}` returns one Published Song by its
   unique, URL-facing slug, or a stable 404 when no visible Song matches.
2. **Artist expansion** — every Song in both list and detail responses carries its
   Artists (main first, then featured), so the catalog can attribute performers.
3. **Album expansion** — a Song that belongs to an Album carries that Album's
   display metadata; a standalone single carries a null album, unchanged.
4. **Catalog scaling** — the list endpoint accepts optional pagination and a
   title search term and a genre filter, and always applies a safe default limit
   so it can never return an unbounded catalog.

Every response continues to hide internal fields (`filePath`, `deletedAt`), return
stable `ErrorResponse(code, message)` JSON on failure, and pass through the
existing CORS filter. No database schema change is required — all data already
exists in `songs`, `artists`, `song_artists`, `albums`, `genres`, and
`song_genres`.

For platform roles, keep public registration USER-only and keep the existing
single login contract for every account. Add an authenticated Artist Application
workflow in which a USER submits artist details, an ADMIN reviews the request,
and only an approval atomically promotes the User to ARTIST and creates or links
their Artist profile. Role-aware frontend routing and all user-facing Artist
Dashboard work follow this approval gate.

## User Stories

### Song detail

1. As a listener, I want to open one Song by its slug, so that a detail page can show a single track.
2. As a listener, I want the detail response to contain the same Song fields as the list, so that the frontend can reuse one rendering shape.
3. As a listener, I want a request for an unknown slug to return a clear not-found error, so that the UI can show a "Song not found" state.
4. As a listener, I want a request for a Draft, Hidden, or soft-deleted Song's slug to also return not-found, so that non-visible Songs cannot be reached by guessing their slug.
5. As a frontend developer, I want the detail endpoint under the same `/api/songs` path, so that I do not have to learn a second base URL.
6. As a frontend developer, I want slugs to be the public identifier, so that detail links stay stable and human-readable.

### Artist expansion

7. As a listener, I want each Song to show its main Artist, so that I know who performed the track.
8. As a listener, I want featured Artists included when a Song has them, so that collaborations are represented correctly.
9. As a listener, I want Artists returned in a stable display order, so that the main Artist appears first and featured Artists follow.
10. As a listener, I want a Song with no Artist rows to still render, so that incomplete catalog data does not break the response.
11. As a frontend developer, I want each expanded Artist to include id, name, slug, role, and position, so that I can display and link performers.
12. As a frontend developer, I want Artist data on both list and detail responses, so that attribution is consistent everywhere a Song appears.
13. As a backend developer, I want Artists for a page of Songs loaded without a per-Song query, so that the list endpoint does not degrade as the catalog grows.

### Album expansion

14. As a listener, I want a Song that belongs to an Album to show that Album, so that I understand where the track comes from.
15. As a listener, I want a standalone single to return a null album, so that Songs without an Album still appear cleanly.
16. As a listener, I want each expanded Album to include title, slug, cover image, album type, and release date, so that the frontend can present the release.
17. As a frontend developer, I want to prefer the Song's own cover and fall back to the Album cover, so that Songs without their own artwork still show an image.
18. As a backend developer, I want a soft-deleted Album to not leak into a Song response, so that removed releases stay invisible even if a Song still references them.

### Catalog scaling — pagination, search, filter

19. As a listener, I want the catalog list to return newest Published Songs first, so that recent additions are easy to find (unchanged from the MVP).
20. As a listener, I want to page through the catalog, so that I can browse large result sets without loading everything at once.
21. As a listener, I want a sensible default page size, so that a plain request returns a reasonable amount of data.
22. As a listener, I want to search Songs by title, so that I can find a track by name.
23. As a listener, I want to filter Songs by genre, so that I can browse one style of music.
24. As a listener, I want search and genre filter to combine with paging, so that narrowed results still page correctly.
25. As a listener, I want the list response to tell me the total number of matching Songs, so that the UI can show how many results exist and how many pages there are.
26. As a frontend developer, I want invalid pagination parameters rejected with a clear error, so that bad input does not silently return surprising data.
27. As a backend developer, I want an enforced maximum page size, so that a client cannot request an unbounded catalog by asking for a huge page.
28. As a backend developer, I want the list endpoint to always apply a limit even when no pagination is supplied, so that the catalog can never be returned unbounded by default.

### Cross-cutting

29. As a listener, I want only Published, non-deleted Songs from every read endpoint, so that visibility rules are identical across list, detail, search, and filter.
30. As a frontend developer, I want internal fields (file path, deleted timestamp) hidden on every response, so that no endpoint leaks storage or moderation internals.
31. As a frontend developer, I want database failures returned as a stable generic error, so that SQL details never reach the client.
32. As a backend developer, I want every new endpoint to route through the existing Song servlet, so that there is a single seam to test and reason about.
33. As a tester, I want to verify every capability through HTTP requests against the Docker stack, so that I test the user-visible contract, not private methods.

### Image storage infrastructure

34. As an Artist, I want every Song cover stored under my Artist slug, so that my media is organized separately from other Artists' media.
35. As an Artist, I want my cover folder created automatically, so that my first upload does not require manual ImageKit setup.
36. As a backend developer, I want Artist Dashboard code to use one image-storage abstraction, so that provider details do not spread across servlets and services.
37. As a backend developer, I want ImageKit isolated behind the storage abstraction, so that later cover-upload tasks do not call the ImageKit SDK directly.
38. As a deployment operator, I want ImageKit credentials supplied through environment variables, so that secrets are not committed to the repository or baked into the application image.
39. As a backend developer, I want each successful image upload to return its URL, ImageKit file id, and ImageKit file path, so that later tasks can display and manage the stored image.
40. As a tester, I want the image-storage contract verified independently from an Artist HTTP endpoint, so that the infrastructure can be completed before upload APIs exist.

### Artist application and role activation

41. As a new listener, I want public registration to create a `USER` account only, so that privileged roles cannot be self-assigned.
42. As a listener, I want to sign in through the same login endpoint as every other account, so that I do not need to know a role-specific authentication flow.
43. As a `USER`, I want to submit an Artist Application with my artist display name, biography, avatar URL, and optional social links, so that I can request access to Artist tools.
44. As a `USER`, I want my submitted Artist Application to show as `PENDING`, so that I know it is awaiting an Admin decision.
45. As a `USER`, I want to view my latest Artist Application and its decision, so that I understand whether I can access the Artist Dashboard.
46. As a `USER`, I want the system to prevent a second pending application, so that my request cannot be duplicated or reviewed inconsistently.
47. As an `ADMIN`, I want to list and inspect Artist Applications by status, so that I can review candidate Artists efficiently.
48. As an `ADMIN`, I want to approve a pending Artist Application, so that the applicant becomes an `ARTIST` and receives an Artist profile.
49. As an `ADMIN`, I want to link an approved account to an existing unlinked Artist profile when appropriate, so that duplicate Artist profiles are not created.
50. As an `ADMIN`, I want to reject a pending Artist Application without changing the applicant's role, so that only approved candidates gain Artist access.
51. As an approved Artist, I want the next authenticated request to recognize my `ARTIST` role, so that I can enter the Artist Dashboard without a separate account.
52. As a frontend user, I want the app to route me according to the role returned by the backend, so that listeners, Artists, and Admins reach the correct workspace.
53. As a frontend developer, I want role-protected routes to keep an unauthorized user out of Artist and Admin screens, so that the UI matches the backend permission model.
54. As a security reviewer, I want role checks enforced by the backend on every privileged API, so that changing browser state cannot grant Artist or Admin access.

## Implementation Decisions

### Overall

- The feature stays **read-only** and **public** (no bearer token), consistent with the MVP.
- No database schema change. All data is read from existing tables: `songs`, `artists`, `song_artists`, `albums`, `genres`, `song_genres`.
- The single HTTP seam is the existing `SongServlet` mapped at `/api/songs/*`. All new routes are added there by inspecting `getPathInfo()`, matching the existing routing style. No `@WebServlet` annotations; `web.xml` mapping is unchanged.
- The visibility rule — a **Published Song** is `status = 'PUBLISHED'` **and** `deleted_at IS NULL` — must be applied identically by every read path. It currently lives in repository SQL; keep it in SQL for every new query rather than re-implementing it in the service.
- Response DTOs continue to expose a static `fromEntity(...)` mapper and continue to omit `filePath` and `deletedAt`.
- Errors continue to return `ErrorResponse(code, message)` JSON. `SQLException` maps to `500 DATABASE_ERROR`. New error codes are added for this feature (below) and mapped to HTTP status inside the servlet, mirroring how `AuthServlet.getStatusCode` maps auth codes.

### Song detail endpoint

- Route: `GET /api/songs/{slug}` — the servlet treats a non-root, single-segment path (e.g. `/blinding-lights`) as a slug lookup.
- The slug is the public identifier (unique and URL-facing per the schema). Detail is looked up by slug, not numeric id.
- A new repository read fetches one Song by slug, applying the same Published-Song visibility filter as the list. A non-visible or non-existent slug produces no row.
- The service returns the detail response or signals not-found; the servlet maps not-found to `404` with a new code `SONG_NOT_FOUND`.
- The detail response body is the same expanded `SongResponse` shape used by the list, so the frontend reuses one model.

### Artist expansion

- A new `Artist` entity maps the `artists` table; a new read-only Artist repository loads Artists. An `Album` entity and read-only Album repository are introduced similarly (below).
- Expansion source: the `song_artists` join table, which carries `role` (`MAIN` / `FEATURED`) and `position`. Artists for a Song are ordered by `role` (MAIN before FEATURED) then `position`, so the primary Artist is first.
- The `SongResponse` gains an ordered list of a nested artist view exposing `id`, `name`, `slug`, `role`, and `position`. A Song with no `song_artists` rows returns an empty list, not null, and does not error.
- **N+1 avoidance is a required decision, not an optimization.** For the list endpoint, Artists for the whole page must be loaded in a single batched query keyed by the page's Song ids (e.g. `WHERE song_id IN (...)`) and grouped in memory — not one query per Song. The detail endpoint may load Artists for its single Song directly.
- Soft-deleted Artists (`artists.deleted_at` not null) are excluded from expansion.

### Album expansion

- The `SongResponse` gains a nullable nested album view. It is populated only when the Song's `album_id` is present **and** the referenced Album is not soft-deleted; otherwise it is null (this is the standalone-single case and is valid).
- The album view exposes `id`, `title`, `slug`, `coverUrl`, `albumType`, and `releaseDate`.
- Cover fallback (prefer Song cover, else Album cover) is a **frontend** presentation decision. The backend returns both the Song `coverUrl` and the Album `coverUrl` as-is and does not collapse them, so the client can choose.
- As with Artists, Albums for a page of Songs should be batch-loaded by the set of non-null `album_id`s rather than one query per Song.

### Catalog scaling — pagination, search, filter

- The list endpoint accepts optional query parameters: `page`, `size`, `q` (title search), `genre` (genre slug). All are optional; a bare `GET /api/songs` behaves as today but with an enforced default limit.
- Defaults and bounds (a prototype of the validation rule; exact numbers are the decision, not the surrounding code):
  - `page` defaults to `1`, must be a positive integer.
  - `size` defaults to `20`, must be a positive integer, capped at a maximum of `50`. A request above the cap is rejected rather than silently clamped, to keep the contract explicit.
  - Invalid `page`/`size` (non-numeric, zero, negative, or `size` over the cap) → `400 INVALID_QUERY_PARAM`.
- `q` performs a case-insensitive substring match on Song title. Empty or absent `q` means no title constraint.
- `genre` filters to Songs joined to the genre with that slug via `song_genres` / `genres`. An unknown genre slug returns an empty result set (not an error).
- Search, genre filter, pagination, and the Published-Song visibility rule all compose in a single SQL query per request (plus the batched Artist and Album loads for the returned page).
- **The list response shape changes** from a bare JSON array to a wrapper object carrying the page of Songs plus pagination metadata: the items, the total count of matching Songs, the current `page`, and the `size`. This is a deliberate one-time contract change made now, before a frontend depends on the array shape. The detail endpoint returns a single Song object (no wrapper).

  A prototype of the wrapper shape (fields are the decision; names may be adjusted to match existing DTO conventions):

  ```
  {
    "items": [ SongResponse, ... ],
    "total": <count of all matching Published Songs>,
    "page":  <current page, 1-based>,
    "size":  <page size actually applied>
  }
  ```

### New error codes (mapped to HTTP status in the servlet)

- `SONG_NOT_FOUND` → 404
- `INVALID_QUERY_PARAM` → 400
- (existing) `DATABASE_ERROR` → 500, `NOT_FOUND` → 404 for unmatched routes.

### Image storage infrastructure

- ImageKit is the image storage provider for cover images. Artist Dashboard
  servlets, services, and repositories must not depend on the ImageKit SDK or
  ImageKit-specific request types directly.
- Add a reusable, provider-neutral `ImageStorageService` abstraction. Its upload
  operation receives the owning Artist's slug plus the image content and required
  file metadata, and returns a storage result containing exactly `imageUrl`,
  `fileId`, and `filePath`.
- Add an `ImageKitStorageService` implementation of that abstraction. All
  ImageKit authentication, folder preparation, upload calls, and provider response
  mapping belong inside this implementation.
- ImageKit credentials are configuration only and are read from environment
  variables: `IMAGEKIT_PUBLIC_KEY`, `IMAGEKIT_PRIVATE_KEY`, and
  `IMAGEKIT_URL_ENDPOINT`. They must not have committed secret defaults and must
  never be written to application logs or API responses.
- Cover images use the exact folder structure
  `/artists/{artistSlug}/covers/`. For example, the Artist slug `son-tung-mtp`
  resolves to `/artists/son-tung-mtp/covers/`.
- The Artist slug comes from the authenticated Artist profile established in
  Phase 3. It must be non-blank and valid before any provider call. Clients do not
  supply an arbitrary destination folder.
- Folder preparation is idempotent: `ImageKitStorageService` includes the Artist
  cover folder in every upload request. ImageKit's upload API creates missing
  nested folders automatically and reuses an already-existing folder.
- Task 3.6 introduces infrastructure only. It does not add an HTTP endpoint,
  mutate a Song, or permit a cover upload by itself. The later Artist cover-upload
  task composes authorization, Song ownership, and `ImageStorageService`.

### Artist application and role activation amendment

- MelodyHub keeps one authentication system, one public registration endpoint, and
  one login endpoint. Public registration always creates a `USER` with `ACTIVE`
  status. A request must not accept a role field, and no public Artist or Admin
  registration route is introduced.
- A login response and `GET /api/auth/me` remain the single identity contract. The
  backend determines the authenticated role from the persisted User record; the
  frontend must not select or submit a role during sign-in. Logout and refresh
  token routes remain session-maintenance routes, not alternative login flows.
- Introduce an `Artist Application` as a first-class persisted record owned by one
  User. It contains an id, applicant user id, artist display name, biography,
  optional avatar URL, optional social-links JSON object, status, reviewer user
  id, review timestamp, optional rejection reason, and audit timestamps.
- Artist Application status is exactly `PENDING`, `APPROVED`, or `REJECTED`.
  An applicant can have at most one `PENDING` application. Rejected applications
  remain immutable history; a USER may submit a new application after rejection.
  An already-approved Artist cannot submit another application.
- The application avatar is a URL in this phase. Avatar file upload and ImageKit
  profile-media storage are separate future work; this feature must not bypass the
  existing image-storage boundary or invent a second provider integration.
- A USER submits an application through one authenticated self-service endpoint
  and reads their own latest application through one authenticated read endpoint.
  The response excludes reviewer-only internals except the visible status and any
  approved rejection reason.
- Admin review uses an `ADMIN`-only route family under `/api/admin/artist-applications`.
  It supports paged/filterable review of applications, one application detail read,
  approval, and rejection. The existing authorization service is the sole backend
  role-enforcement seam.
- Approval is one database transaction: lock and confirm the application is still
  `PENDING`; promote the applicant from `USER` to `ARTIST`; create an Artist
  profile from the application or link an Admin-selected, active, unlinked Artist
  profile; record the reviewer and approval decision; then commit. A conflict,
  invalid profile link, or database failure rolls back the entire transition.
- Rejection is one database transaction that moves only a `PENDING` application
  to `REJECTED`, records the reviewer and optional reason, and leaves the User role
  as `USER`.
- The frontend has one sign-in form. It may offer Listener, Artist Studio, and
  Admin Console as destination context, but that context is never sent as a role.
  After authentication, the app routes by the returned role: `USER` to listener
  Home, `ARTIST` to Artist Dashboard, and `ADMIN` to Admin Dashboard. A selected
  destination that conflicts with the actual role falls back to Home with a
  non-privileged access notice.
- Frontend role guards are navigation behavior only. Artist and Admin APIs must
  continue to enforce authorization server-side, and an approved role change is
  recognized from the persisted User record on the next authenticated request.

## Testing Decisions

- **The single test seam is the HTTP contract at `GET /api/songs/*`.** Every capability is verified by making a request against the running Docker Compose stack (MySQL + backend) and asserting the externally visible response. This matches both prior Song specs and the project's current reality of no automated test harness.
- A good test asserts **external behavior only**: HTTP status, `Content-Type`, JSON shape, filtering rules, ordering, pagination metadata, and error bodies. Tests must not assert private method names, SQL string formatting, or repository internals.
- Because verification is manual for now, the deliverable for each task includes the exact curl/Hoppscotch requests used to check it, kept alongside the implementation so the contract is reproducible. Keep each task small enough to verify by hand.
- Build verification: `mvn clean package` from `backend/` must succeed. Runtime verification: `docker compose up --build` from the repo root, then exercise the endpoints. If the schema seed changes, reset with `docker compose down -v` first.
- Prior art: the auth endpoints (`/api/auth/*`) are the closest existing HTTP-contract example — same servlet/service/JDBC style, same `ObjectMapper` config, same `ErrorResponse` shape. New endpoints should be smoke-tested the same way auth was.
- Data scenarios to cover across the seam (seed rows in MySQL, then request):
  - Detail: a Published Song slug → 200 with the full shape; a Draft, a Hidden, and a soft-deleted slug → each 404 `SONG_NOT_FOUND`; an unknown slug → 404.
  - Artists: a Song with one MAIN + one FEATURED Artist → both returned, MAIN first; a Song with no Artists → empty list, still 200.
  - Album: a Song with an Album → album object present; a standalone single → null album; a Song referencing a soft-deleted Album → null album.
  - Pagination: more Songs than one page → correct `items`, `total`, `page`, `size`; `page`/`size` out of range → 400 `INVALID_QUERY_PARAM`; `size` above cap → 400.
  - Search/filter: `q` matching a subset → only matches returned; `genre` slug → only that genre; unknown `genre` → empty result, 200; `q` + `genre` + `page` combined → correctly narrowed and paged.
  - Ordering: newest `created_at` first, `id DESC` tie-break, preserved under paging.
- If an automated harness is introduced later, the natural choice is JUnit 5 driving HTTP against a Testcontainers MySQL — but that is explicitly **not** required by this spec and should not block the tasks below.
- The ImageKit infrastructure is the one exception to the HTTP-only test seam
  because it intentionally exists before an upload endpoint. Test it at the
  `ImageStorageService` contract: given an Artist slug and valid image input, the
  result contains non-blank `imageUrl`, `fileId`, and `filePath`, and the path is
  under `/artists/{artistSlug}/covers/`.
- Storage contract tests should use a fake or stubbed provider boundary and assert
  externally visible results rather than ImageKit SDK internals. A separate,
  opt-in smoke test may use ImageKit test credentials to verify folder creation,
  the already-existing-folder case, and one real upload without making live
  credentials a normal build requirement.
- Configuration verification must prove that missing required ImageKit environment
  variables fail with a clear configuration error and that no credential value is
  included in logs or returned errors.
- The role-activation seam is the authenticated HTTP contract: a USER submits an
  Artist Application, an ADMIN reviews it, and a later `/api/auth/me` plus an
  ARTIST-only request demonstrate the resulting role and profile link. This is
  higher-value than testing repository implementation details or frontend state.
- Required role-activation scenarios are: public registration always returns a
  USER; a USER can create and read one PENDING application; duplicate PENDING
  creation is rejected; non-Admins cannot review; approval atomically promotes,
  profiles, and marks the application approved; rejection preserves the USER
  role; and the next authenticated role lookup and Artist-only request honor the
  review result.
- Frontend verification exercises the login-to-role-routing contract with the
  existing auth store and route guard: USER reaches listener Home, ARTIST reaches
  the Artist Dashboard, ADMIN reaches the Admin Dashboard, and a forged or stale
  browser role never bypasses the backend `403` response.

## Roadmap

Tasks are grouped into phases. Each task is independently implementable and
verifiable at the HTTP seam. Dependencies are called out per task.

This roadmap preserves the completed Song Catalog read work and the completed
role foundation. MelodyHub has three account roles: `USER`, `ARTIST`, and
`ADMIN`. From this amendment forward, an Artist Application and Admin approval
are the only user-facing path from `USER` to `ARTIST`; user-facing Artist
Dashboard delivery is sequenced after that path.

Primary test seams for future tasks:

- Public listener read APIs continue through `/api/songs/*`.
- Authenticated user identity continues through the existing auth/JWT seam.
- Artist Dashboard APIs are verified as an `ARTIST` account and must only expose
  that Artist's own profile and Songs.
- Admin Dashboard APIs are verified as an `ADMIN` account and may manage platform
  resources across Users, Artists, Songs, Albums, and Genres.
- Artist Application submission and review use one authenticated HTTP seam, and
  approval is verified by the resulting current-user role and ARTIST-only access.

### Phase 0 - Authentication baseline

> Already implemented before this roadmap expansion. Keep this work as the
> identity foundation for role-aware dashboard APIs.

- **[x] Task 0.1 - Register account.** Keep the existing registration endpoint as
  the way a new account enters MelodyHub. Purpose: preserve the completed auth
  work. *Depends on:* nothing.
- **[x] Task 0.2 - Login account.** Keep the existing login endpoint and token
  response as the way a client starts an authenticated session. Purpose: preserve
  the completed auth work. *Depends on:* 0.1.

### Phase 1 — Song detail read path

> Smallest self-contained slice; reuses the existing repository columns, row
> mapper, and DTO with no contract change to the list endpoint.

- **[x] Task 1.1 — Repository: find one Published Song by slug.** Add a read that
  selects a single Song by `slug` with the same `PUBLISHED` + `deleted_at IS NULL`
  filter as the list. Purpose: the data access for a detail page. *Depends on:*
  nothing (reuses `SONG_COLUMNS` / `mapRow`). — Done: `SongRepository.findBySlug`
  returns `Optional<Song>`.
- **[x] Task 1.2 — Service: get Song by slug.** Add a service method that returns the
  detail response or signals not-found. Purpose: give the not-found decision a
  home in the service layer. *Depends on:* 1.1. — Done: `SongService.getBySlug`
  returns `Optional<SongResponse>` (empty = not-found, for the servlet to map).
- **[x] Task 1.3 — Servlet: route `GET /api/songs/{slug}` and add `SONG_NOT_FOUND`.**
  Treat a single-segment path as a slug lookup; map not-found to 404. Purpose:
  expose detail at the HTTP seam. *Depends on:* 1.2. — Done: `doGet` routes a
  single-segment path to `handleGetBySlug`, which returns 200 with the
  `SongResponse` or 404 `SONG_NOT_FOUND`.

### Phase 2 — Catalog scaling (do before expansion changes the list shape)

> Settles the list response contract (wrapper + metadata + limit) before Artist
> and Album fields are added, so the shape changes only once.

- **[x] Task 2.1 — Enforce a default limit on the list query.** Cap the existing
  `getAll` so it can never return an unbounded catalog even with no parameters.
  Purpose: remove the current unbounded-result risk. *Depends on:* nothing. —
  Done: list query applies `LIMIT ?` (default 20); this bound was folded into the
  paged query in Task 2.2.
- **[x] Task 2.2 — Add `page` / `size` with validation and a response wrapper.**
  Parse and validate pagination params (`INVALID_QUERY_PARAM` on bad input,
  enforced max size), return the `{ items, total, page, size }` wrapper. Purpose:
  pageable browsing and a stable list contract. *Depends on:* 2.1. **Contract
  change:** list response goes from array to wrapper. — Done: `SongRepository`
  now has `getPage(size, offset)` + `count()`; `SongService.getPage(page, size)`
  assembles `PagedResponse<SongResponse>`; `SongServlet` parses/validates `page`
  (default 1) and `size` (default 20, max 50), returning `400 INVALID_QUERY_PARAM`
  on non-numeric, zero, negative, or over-cap input.
- **[x] Task 2.3 — Title search (`q`) and genre filter (`genre`).** Add optional
  case-insensitive title match and genre-slug filter that compose with paging and
  the visibility rule in one query. Purpose: make the catalog browsable at scale.
  *Depends on:* 2.2. — Done: `SongServlet` passes optional `q` and `genre`
  parameters through the paged list route; `SongRepository` composes title search
  and genre-slug filtering into both the page query and total-count query while
  preserving Published/non-deleted visibility.

### Phase 3 - Role and Artist account foundation

> Introduces the platform identity model required before any dashboard write API.
> This is the next phase to implement.

- **[x] Task 3.1 - Schema: add the `ARTIST` role.** Update the account role design so
  the allowed roles are exactly `USER`, `ARTIST`, and `ADMIN`, preserving existing
  `USER` and `ADMIN` accounts. Purpose: make Artist accounts first-class without
  breaking current auth. *Depends on:* Phase 0. — Done: the schema and backend
  role enum both include `ARTIST`.
- **[x] Task 3.2 - Schema: link Artist accounts to Artist profiles.** Add an
  account-to-Artist-profile relationship so an authenticated `ARTIST` user maps
  to exactly one Artist profile for the MVP. Existing Artist rows may remain
  unlinked until an Admin connects them. Purpose: define ownership for dashboard
  reads and writes. *Depends on:* 3.1 and the existing `artists` table. — Done:
  Artist profiles can optionally link to one `ARTIST` User account through
  `user_id`, with database checks preventing non-Artist account links.
- **[x] Task 3.3 - Auth response: expose role consistently.** Ensure login/current-user
  responses expose the authenticated user's role so clients can route to listener,
  artist, or admin experiences. Purpose: support dashboard navigation without
  extra lookup calls. *Depends on:* 3.1. — Done: auth responses already expose
  `UserResponse.role`, and the role model now supports `ARTIST`.
- **[x] Task 3.4 - Authorization seam: protect role-based APIs.** Add a reusable
  authorization check for authenticated routes, including `ARTIST`-only and
  `ADMIN`-only access. Purpose: avoid duplicating token/role checks in every
  dashboard servlet. *Depends on:* 3.1 and Phase 0. — Done: a reusable
  authorization service validates tokens, banned users, and required roles.
- **[x] Task 3.5 - Artist account lookup.** Add the read path that resolves the
  current authenticated `ARTIST` user to their linked Artist profile, returning a
  stable not-found or forbidden error when the link is missing. Purpose: give all
  Artist Dashboard tasks one ownership source. *Depends on:* 3.2 and 3.4. —
  Done: the Artist account service resolves the authenticated Artist profile from
  the current token.
- **[x] Task 3.6 - ImageKit cover storage infrastructure.** Configure ImageKit through
  `IMAGEKIT_PUBLIC_KEY`, `IMAGEKIT_PRIVATE_KEY`, and `IMAGEKIT_URL_ENDPOINT`; add
  the reusable `ImageStorageService` abstraction and its `ImageKitStorageService`
  implementation; upload to `/artists/{artistSlug}/covers/` so ImageKit creates
  missing folders automatically; and return `imageUrl`, `fileId`, and `filePath` for every successful
  upload. Purpose: establish one reusable storage seam before any Artist upload
  endpoint is implemented. This task adds no servlet or Song mutation. *Depends
  on:* 3.2. *Blocks:* Active Phase 7 Task 7.2. - Done: environment-backed ImageKit
  configuration, the provider-neutral storage contract, the ImageKit adapter,
  Artist cover-folder routing, and required upload result metadata are implemented.

> **Historical roadmap:** the phases below preserve completed Artist API work and
> the previously planned future sequence. The active task ordering that follows
> this historical record is authoritative for all unfinished work.

### Phase 4 - Artist Dashboard: profile and own catalog

> First authenticated Artist Dashboard slice. It gives an Artist their identity
> and a private view of their own Songs before write actions are added. Start this
> phase only after the ImageKit cover storage foundation in Task 3.6 is complete.

- **[x] Task 4.1 - View own Artist profile.** Add an `ARTIST`-only API to retrieve the
  current Artist profile. Purpose: the dashboard can show the Artist name, slug,
  bio, and image metadata. *Depends on:* 3.5 and 3.6. - Done: `GET
  /api/artist/profile` resolves the authenticated `ARTIST` account to its active
  Artist profile and returns profile metadata without ownership or soft-delete
  internals.
- **[x] Task 4.2 - Update own Artist profile metadata.** Add an `ARTIST`-only API to
  update safe profile fields such as name, slug, bio, and image URL metadata.
  Purpose: Artists can maintain their public profile information. *Depends on:*
  4.1. - Done: `PUT /api/artist/profile` validates and updates only the current
  authenticated Artist's name, slug, bio, and image URL, returns the refreshed
  profile, and reports stable validation and duplicate-slug errors.
- **[x] Task 4.3 - List own Songs.** Add an `ARTIST`-only API that returns only Songs
  owned by the current Artist, including Draft, Published, and Hidden Songs but
  excluding soft-deleted Songs by default. Ownership for the MVP is the linked
  Artist appearing as the Song's `MAIN` Artist. Purpose: Artists can manage only
  their own catalog. *Depends on:* 3.5 and Phase 2 paging conventions. - Done:
  `GET /api/artist/songs` returns the current Artist's newest-first private Song
  page with the existing default, maximum, and response-wrapper conventions.
- **[x] Task 4.4 - View one own Song by id or slug.** Add an `ARTIST`-only detail API
  for a single own Song, including non-public statuses. Purpose: the edit screen
  can load one Song without leaking another Artist's Song. *Depends on:* 4.3. -
  Done: `GET /api/artist/songs/{id-or-slug}` resolves an owned Song by positive
  numeric id or slug and returns `SONG_NOT_FOUND` for missing, deleted, or
  non-owned Songs.

### Phase 5 - Artist Dashboard: Song metadata writes

> Adds create, update, and soft-delete behavior without file upload complexity.

- **Task 5.1 - Schema: allow draft Songs before audio upload.** Adjust the Song
  storage design so an Artist can create a Draft Song before an audio file exists,
  while public list/detail APIs still only expose Published, non-deleted Songs.
  Purpose: support incremental Song creation. *Depends on:* Phase 2 and 3.5.
- **Task 5.2 - Create own Song metadata.** Add an `ARTIST`-only API to create a
  Draft Song with title, slug, optional album, track number, duration metadata,
  cover metadata, and lyrics text where available. The creating Artist is attached
  as the `MAIN` Artist. Purpose: Artists can start a Song draft. *Depends on:*
  5.1 and 3.5.
- **Task 5.3 - Update own Song metadata.** Add an `ARTIST`-only API to update
  editable metadata for the current Artist's own Song. Purpose: Artists can revise
  Song details before or after publishing. *Depends on:* 4.4 and 5.2.
- **Task 5.4 - Soft-delete own Song.** Add an `ARTIST`-only API that sets the
  Song's soft-delete timestamp instead of physically deleting it. Purpose: Artists
  can remove a Song from their dashboard/public catalog without destroying data.
  *Depends on:* 4.4.
- **Task 5.5 - Preserve public visibility rules after writes.** Ensure Artist
  write actions cannot accidentally expose Draft, Hidden, or soft-deleted Songs
  through the public `/api/songs/*` APIs. Purpose: keep listener catalog behavior
  stable while dashboard writes are introduced. *Depends on:* 5.2, 5.3, and 5.4.

### Phase 6 - Artist Dashboard: file and lyrics uploads

> Adds the media upload surfaces after ownership and Song metadata writes exist.

- **Task 6.1 - Audio and lyrics upload configuration.** Define the remaining
  backend storage and request limits for audio files and `.lrc` uploads. Cover
  image provider configuration is already owned by Task 3.6. Purpose: keep
  non-image upload behavior predictable across local Docker and future
  deployment. *Depends on:* 5.1.
- **Task 6.2 - Upload cover image for own Song.** Add an `ARTIST`-only upload API
  for Song cover images. Resolve the Artist slug from the authenticated profile,
  verify Song ownership, call `ImageStorageService`, update the Song's cover
  metadata, and return `imageUrl`, `fileId`, and `filePath`. The endpoint must not
  call ImageKit directly. Purpose: Artists can provide artwork for their own Songs
  while all files remain under `/artists/{artistSlug}/covers/`. *Depends on:* 3.6
  and 4.4.
- **Task 6.3 - Upload audio file for own Song.** Add an `ARTIST`-only upload API
  for audio files that persists the file location on the Song. Purpose: a Draft
  Song can become playable once audio exists. *Depends on:* 6.1 and 5.2.
- **Task 6.4 - Upload synchronized lyrics file.** Add an `ARTIST`-only upload API
  for `.lrc` files, parse timestamped lyric lines, and replace that Song's
  synchronized lyric rows transactionally. Purpose: Artists can attach timed
  lyrics to their own Songs. *Depends on:* 4.4 and the existing synchronized
  lyrics table.
- **Task 6.5 - Validate upload access and content.** Reject unsupported file
  types, oversized files, malformed `.lrc` content, and attempts to upload to
  another Artist's Song with stable JSON errors. Purpose: make uploads testable
  and safe enough for the dashboard MVP. *Depends on:* 6.2, 6.3, and 6.4.

### Phase 7 - Admin Dashboard foundation

> Adds the `ADMIN` backend surface before individual resource management screens.

- **Task 7.1 - Admin route and authorization pattern.** Establish the admin API
  route pattern and require `ADMIN` authorization for every endpoint under it.
  Purpose: give Admin Dashboard work one protected seam. *Depends on:* 3.4.
- **Task 7.2 - Admin pagination and filter conventions.** Reuse the existing
  paged response style for admin lists, with clear validation errors for bad
  query parameters. Purpose: keep Admin Dashboard lists consistent with Song
  Catalog pagination. *Depends on:* 7.1 and Phase 2.
- **Task 7.3 - Admin error contract.** Define stable admin error codes for
  forbidden access, missing resources, invalid input, duplicate slugs/emails, and
  database failures. Purpose: make management APIs predictable for the frontend.
  *Depends on:* 7.1.

### Phase 8 - Admin Dashboard: Users and Artists

> Gives administrators control over accounts and Artist profiles, including the
> Artist account linkage introduced in Phase 3.

- **Task 8.1 - Admin list Users.** Add an `ADMIN`-only paged API to list Users
  with search/filter support for role and status. Purpose: administrators can
  review accounts. *Depends on:* 7.2.
- **Task 8.2 - Admin update User role/status.** Add an `ADMIN`-only API to change
  a User's role among `USER`, `ARTIST`, and `ADMIN`, and to activate or ban an
  account. Purpose: administrators can grant Artist access and moderate accounts.
  *Depends on:* 8.1 and 3.1.
- **Task 8.3 - Admin list Artist profiles.** Add an `ADMIN`-only paged API to list
  Artist profiles, including whether each profile is linked to an Artist account.
  Purpose: administrators can audit platform Artists. *Depends on:* 7.2 and 3.2.
- **Task 8.4 - Admin create/update Artist profile.** Add `ADMIN`-only APIs to
  create and update Artist profile metadata. Purpose: administrators can manage
  Artist records even before an Artist account is linked. *Depends on:* 8.3.
- **Task 8.5 - Admin link/unlink Artist account.** Add an `ADMIN`-only API to
  connect or disconnect an `ARTIST` User from an Artist profile. Purpose:
  administrators control which account owns each Artist Dashboard. *Depends on:*
  8.2 and 8.4.
- **Task 8.6 - Admin soft-delete or restore Artist profile.** Add `ADMIN`-only
  APIs to hide or restore Artist profiles using soft-delete behavior. Purpose:
  administrators can moderate Artists without losing historical data. *Depends
  on:* 8.4.

### Phase 9 - Admin Dashboard: Songs, Albums, and Genres

> Completes Admin management for music catalog resources.

- **Task 9.1 - Admin list Songs across all Artists.** Add an `ADMIN`-only paged API
  to list Songs across all statuses and Artists, with search/filter support.
  Purpose: administrators can audit the full catalog. *Depends on:* 7.2 and
  Phase 5.
- **Task 9.2 - Admin update Song metadata/status.** Add an `ADMIN`-only API to
  update Song metadata and status, including hiding or publishing Songs. Purpose:
  administrators can moderate catalog visibility. *Depends on:* 9.1.
- **Task 9.3 - Admin soft-delete or restore Song.** Add `ADMIN`-only APIs to remove
  or restore Songs with soft-delete behavior. Purpose: administrators can reverse
  moderation mistakes without physical deletes. *Depends on:* 9.1.
- **Task 9.4 - Admin manage Albums.** Add `ADMIN`-only create, update, list, and
  soft-delete APIs for Albums, preserving the Album-to-Artist relationship.
  Purpose: administrators can manage releases. *Depends on:* 8.3.
- **Task 9.5 - Admin manage Genres.** Add `ADMIN`-only create, update, list, and
  delete APIs for Genres, including duplicate-slug validation. Purpose:
  administrators can maintain browse filters. *Depends on:* 7.2.
- **Task 9.6 - Keep public catalog stable after admin actions.** Verify that Admin
  changes to Songs, Albums, Artists, and Genres do not leak Draft, Hidden,
  soft-deleted, or invalidly linked data through public listener APIs. Purpose:
  preserve the completed Song Catalog contract. *Depends on:* 9.2, 9.3, 9.4, and
  9.5.

### Recommended next task

> The Phase 4-9 plan above is retained as an implementation history. Its remaining
> future-task ordering is superseded by the active roadmap below; completed API
> tasks remain completed and are reused rather than removed.

### Active Phase 4 - Become an Artist

> This phase is the required gate before any new user-facing Artist Dashboard
> work. It preserves public USER-only registration and the single login endpoint.

- **Task 4.1 - Schema: Artist Application lifecycle.** Add the Artist Application
  persistence model with applicant identity, artist display name, bio, optional
  avatar URL, optional social links, `PENDING` / `APPROVED` / `REJECTED` status,
  reviewer metadata, optional rejection reason, and audit timestamps. Enforce at
  most one PENDING application per User. *Depends on:* 3.1 and 3.4.
- **Task 4.2 - User: submit Artist Application.** Add the authenticated USER-only
  API that validates and creates a PENDING application. Public registration stays
  unchanged and creates USER accounts only. *Depends on:* 4.1 and 3.4.
- **Task 4.3 - User: view own Artist Application.** Add the authenticated read
  API for an account's most recent application and visible status. It must never
  expose another applicant's data. *Depends on:* 4.2 and 3.4.
- **Task 4.4 - Admin: review queue and application detail.** Add ADMIN-only paged
  list and detail APIs, including status filtering, so an Admin can inspect
  PENDING applications before deciding. *Depends on:* 4.1, 3.4, and Phase 2
  pagination conventions.
- **Task 4.5 - Admin: approve Artist Application atomically.** Approve one
  PENDING application in a transaction: promote its User to ARTIST, create an
  Artist profile from the application or link an Admin-selected active unlinked
  profile, and record the approval. *Depends on:* 4.4, 3.2, 3.4, and 3.5.
- **Task 4.6 - Admin: reject Artist Application.** Reject one PENDING application,
  record the reviewer and optional reason, and retain the applicant as USER.
  *Depends on:* 4.4.
- **Task 4.7 - Frontend: role-aware sign-in and navigation.** Keep one login form
  and route from the role returned by the existing auth contract. Add user-facing
  application status, an Artist Dashboard route guarded for ARTIST, and an Admin
  route guarded for ADMIN. Client guards guide navigation only; backend checks
  remain authoritative. *Depends on:* 3.3, 4.3, and 4.5.

### Active Phase 5 - Artist Dashboard entry and existing read APIs

> The backend read APIs below are already complete. The first remaining task is
> their Artist Dashboard integration, which is intentionally blocked on approval.

- **[x] Task 5.1 - View own Artist profile API.** `GET /api/artist/profile`
  resolves only the authenticated ARTIST's active profile. *Depends on:* 3.5.
- **[x] Task 5.2 - Update own Artist profile API.** `PUT /api/artist/profile`
  updates only the authenticated ARTIST's safe profile metadata. *Depends on:*
  5.1.
- **[x] Task 5.3 - List own Songs API.** `GET /api/artist/songs` returns the
  current ARTIST's private Song page without soft-deleted Songs. *Depends on:*
  3.5 and Phase 2.
- **[x] Task 5.4 - View one own Song API.** `GET /api/artist/songs/{id-or-slug}`
  resolves only a Song owned by the authenticated ARTIST. *Depends on:* 5.3.
- **Task 5.5 - Frontend: Artist Dashboard read shell.** Build the profile and
  own-catalog dashboard experience using the completed APIs, with no access for
  USER or ADMIN accounts. *Depends on:* 4.7 and 5.1 through 5.4.

### Active Phase 6 - Artist Dashboard: Song metadata writes

- **Task 6.1 - Schema: allow Draft Songs before audio upload.** Adjust Song
  storage so an Artist can create a Draft before an audio file exists, while
  public APIs continue to expose only Published, non-deleted Songs. *Depends on:*
  Phase 2 and 4.5.
- **Task 6.2 - Create own Song metadata.** Add an ARTIST-only API to create a
  Draft Song and attach the current Artist as its MAIN Artist. *Depends on:* 6.1
  and 5.5.
- **Task 6.3 - Update own Song metadata.** Add an ARTIST-only API to change
  editable metadata for the current Artist's own Song. *Depends on:* 5.4 and 6.2.
- **Task 6.4 - Soft-delete own Song.** Add an ARTIST-only soft-delete API for the
  current Artist's own Song. *Depends on:* 5.4.
- **Task 6.5 - Preserve public visibility after writes.** Verify Artist writes
  cannot expose Draft, Hidden, or soft-deleted Songs through `/api/songs/*`.
  *Depends on:* 6.2 through 6.4.

### Active Phase 7 - Artist Dashboard: media and lyrics

- **Task 7.1 - Audio and lyrics upload configuration.** Define storage, request
  limits, and validation for audio files and `.lrc` uploads. *Depends on:* 6.1.
- **Task 7.2 - Upload cover image for own Song.** Add the ARTIST-only cover upload
  API that verifies ownership and uses `ImageStorageService` under the Artist's
  cover folder. *Depends on:* 3.6 and 5.4.
- **Task 7.3 - Upload audio file for own Song.** Add the ARTIST-only audio upload
  API that persists the file location. *Depends on:* 7.1 and 6.2.
- **Task 7.4 - Upload synchronized lyrics file.** Add the ARTIST-only `.lrc`
  upload, parse, and transactional replacement flow. *Depends on:* 5.4 and 7.1.
- **Task 7.5 - Validate upload ownership and content.** Return stable errors for
  invalid file types, sizes, lyrics content, and cross-Artist attempts. *Depends
  on:* 7.2 through 7.4.

### Active Phase 8 - General Admin Dashboard foundation

> Artist Application review is the first ADMIN-only slice. This phase generalizes
> its conventions before wider catalog administration begins.

- **Task 8.1 - Generalize the Admin route and authorization pattern.** Reuse the
  application-review pattern for every `/api/admin/*` endpoint. *Depends on:*
  3.4 and 4.4.
- **Task 8.2 - Admin pagination and filter conventions.** Reuse the existing
  paged response contract for administrative lists. *Depends on:* 8.1 and Phase 2.
- **Task 8.3 - Admin error contract.** Define stable forbidden, not-found,
  validation, conflict, and database error codes. *Depends on:* 8.1.

### Active Phase 9 - Admin Dashboard: Users and Artists

- **Task 9.1 - Admin list Users.** Add ADMIN-only paged User listing with search
  and role/status filtering. *Depends on:* 8.2.
- **Task 9.2 - Admin update User status.** Add ADMIN-only activation and ban
  controls. Direct USER-to-ARTIST promotion remains the Artist Application
  approval flow in Task 4.5. *Depends on:* 9.1.
- **Task 9.3 - Admin list Artist profiles.** Add ADMIN-only paged Artist profile
  listing, including account-link state. *Depends on:* 8.2 and 3.2.
- **Task 9.4 - Admin create or update Artist profiles.** Add administrative Artist
  profile management for profiles not yet linked to an account. *Depends on:* 9.3.
- **Task 9.5 - Admin link or unlink Artist accounts.** Add explicit account-link
  management while preserving the ARTIST-role database invariant. *Depends on:*
  9.4 and 4.5.
- **Task 9.6 - Admin soft-delete or restore Artist profiles.** Add reversible
  Artist profile moderation. *Depends on:* 9.4.

### Active Phase 10 - Admin Dashboard: Songs, Albums, and Genres

- **Task 10.1 - Admin list Songs across Artists.** Add ADMIN-only paged Song
  listing across all statuses and Artists. *Depends on:* 8.2 and Phase 6.
- **Task 10.2 - Admin update Song metadata and status.** Add catalog moderation
  controls, including hide and publish. *Depends on:* 10.1.
- **Task 10.3 - Admin soft-delete or restore Songs.** Add reversible Song
  moderation. *Depends on:* 10.1.
- **Task 10.4 - Admin manage Albums.** Add Album create, update, list, and
  soft-delete APIs while preserving Artist relationships. *Depends on:* 9.3.
- **Task 10.5 - Admin manage Genres.** Add Genre create, update, list, and delete
  APIs with duplicate-slug validation. *Depends on:* 8.2.
- **Task 10.6 - Keep the public catalog stable after Admin actions.** Verify that
  Admin changes do not leak private or deleted catalog data to listeners.
  *Depends on:* 10.2 through 10.5.

### Recommended next task

**Task 4.1 - Schema: Artist Application lifecycle.** It establishes the durable
state and invariants required for user submission, Admin review, role promotion,
and the later Artist Dashboard entry flow.

## Out of Scope

- Direct public registration as ARTIST or ADMIN, role-selection fields in public
  registration, and separate Artist or Admin login endpoints.
- Automatic approval, automatic profile matching by name or slug, or any
  non-Admin path that promotes a USER to ARTIST.
- Editing, cancelling, or withdrawing a PENDING Artist Application; email or
  push notifications about application decisions; and appeal workflows.
- Avatar file uploads for applications. This phase accepts an avatar URL only;
  profile-media storage is future work.
- Artist Dashboard Song creation, update, deletion, cover/audio/lyrics upload,
  playback, and streaming. These begin only in the later active phases.
- General Admin management of Users, Artists, Songs, Albums, and Genres beyond
  the Artist Application review endpoints scheduled in Active Phase 4.
- Play-count incrementing and listen history.
- Introducing a new automated test harness; verification remains at the existing
  HTTP and browser-routing seams.

## Further Notes

- This spec continues the backend-first slicing of the two MVP specs and stays
  within the current architecture: Servlet routing on `getPathInfo()`, a thin
  constructor-injectable service, plain-JDBC repositories with `PreparedStatement`
  and try-with-resources, Lombok entities, and `fromEntity` response DTOs.
- The Published-Song visibility rule is the invariant that ties every task
  together — list, detail, search, and filter must all enforce `status =
  'PUBLISHED' AND deleted_at IS NULL`, and expansions must additionally exclude
  soft-deleted Artists and Albums. A single shared SQL predicate is preferable to
  repeating the literal in each query.
- The list-response contract change (array → wrapper) is intentionally scheduled in
  Phase 2, before any frontend depends on the array shape. If it slips until after
  a client is built, it becomes a breaking change instead of a free one.
- N+1 avoidance for Artist and Album expansion is a hard requirement of the design,
  not a later optimization: the batched-load approach must be built into Phases 3
  and 4 from the first commit, because retrofitting it after a per-Song query ships
  is a rewrite of the same code.
- MelodyHub runs backend + MySQL through Docker Compose, so real API verification
  should prefer the Docker stack; reset the volume (`docker compose down -v`) when
  the schema seed changes.
- ImageKit credentials remain deployment secrets. Local Docker and deployed
  environments must inject the three ImageKit environment variables; the spec
  must never contain real credential values.
- The existing ARTIST-only profile and own-Song APIs remain valid completed
  backend work. This amendment changes when their user-facing dashboard is
  delivered, not their authorization or data-ownership contract.
