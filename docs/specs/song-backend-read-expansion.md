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

## Roadmap

Tasks are grouped into phases. Each task is independently implementable and
verifiable at the HTTP seam. Dependencies are called out per task.

### Phase 1 — Song detail read path

> Smallest self-contained slice; reuses the existing repository columns, row
> mapper, and DTO with no contract change to the list endpoint.

- **Task 1.1 — Repository: find one Published Song by slug.** Add a read that
  selects a single Song by `slug` with the same `PUBLISHED` + `deleted_at IS NULL`
  filter as the list. Purpose: the data access for a detail page. *Depends on:*
  nothing (reuses `SONG_COLUMNS` / `mapRow`).
- **Task 1.2 — Service: get Song by slug.** Add a service method that returns the
  detail response or signals not-found. Purpose: give the not-found decision a
  home in the service layer. *Depends on:* 1.1.
- **Task 1.3 — Servlet: route `GET /api/songs/{slug}` and add `SONG_NOT_FOUND`.**
  Treat a single-segment path as a slug lookup; map not-found to 404. Purpose:
  expose detail at the HTTP seam. *Depends on:* 1.2.

### Phase 2 — Catalog scaling (do before expansion changes the list shape)

> Settles the list response contract (wrapper + metadata + limit) before Artist
> and Album fields are added, so the shape changes only once.

- **Task 2.1 — Enforce a default limit on the list query.** Cap the existing
  `getAll` so it can never return an unbounded catalog even with no parameters.
  Purpose: remove the current unbounded-result risk. *Depends on:* nothing.
- **Task 2.2 — Add `page` / `size` with validation and a response wrapper.**
  Parse and validate pagination params (`INVALID_QUERY_PARAM` on bad input,
  enforced max size), return the `{ items, total, page, size }` wrapper. Purpose:
  pageable browsing and a stable list contract. *Depends on:* 2.1. **Contract
  change:** list response goes from array to wrapper.
- **Task 2.3 — Title search (`q`) and genre filter (`genre`).** Add optional
  case-insensitive title match and genre-slug filter that compose with paging and
  the visibility rule in one query. Purpose: make the catalog browsable at scale.
  *Depends on:* 2.2.

### Phase 3 — Artist expansion

> Applies to both list and detail responses. Introduces the first read-only
> Artist entity/repository and the batched-load pattern.

- **Task 3.1 — Artist entity + read-only Artist repository.** Map the `artists`
  table and add a batched read of Artists for a set of Song ids via `song_artists`,
  excluding soft-deleted Artists, ordered MAIN-then-position. Purpose: the data
  access for attribution, N+1-safe from the start. *Depends on:* nothing (can run
  parallel to Phases 1–2).
- **Task 3.2 — Attach Artists to the Song response.** Add the ordered nested
  artist view to `SongResponse` and populate it for both the paged list (batched)
  and the single detail Song. Purpose: performers appear everywhere a Song does.
  *Depends on:* 3.1, and on 1.3 + 2.2 existing so both response paths are present.

### Phase 4 — Album expansion

> Mirrors Phase 3 for the optional Album relationship.

- **Task 4.1 — Album entity + read-only Album repository.** Map the `albums`
  table and add a batched read of Albums for a set of non-null `album_id`s,
  excluding soft-deleted Albums. Purpose: the data access for release metadata.
  *Depends on:* nothing (parallelizable with Phase 3).
- **Task 4.2 — Attach Album to the Song response.** Add the nullable nested album
  view to `SongResponse`, populated only for Songs with a present, non-deleted
  Album; null for standalone singles. Purpose: releases appear on Songs that have
  them. *Depends on:* 4.1, and on the same list + detail response paths as 3.2.

### Recommended next task after `GET /api/songs`

**Task 1.1 → 1.3 (the Song detail endpoint, `GET /api/songs/{slug}`).** It is the
smallest independent slice: it reuses `SONG_COLUMNS`, `mapRow`, and the existing
`SongResponse` unchanged, adds exactly one repository method, one service method,
one servlet route, and one error code — and it proves the single-resource read
path and the slug contract without touching the list response shape. Do Phase 2
(scaling) next so the list contract settles before Phases 3–4 add fields to it.

## Out of Scope

- Any write operation: creating, updating, hiding, or soft-deleting Songs, Artists, or Albums.
- Song upload, cover upload, avatar upload.
- Admin-only endpoints, moderation, or approval workflows.
- Authentication or role/permission changes; every endpoint here stays public.
- Play-count incrementing and listen history.
- Audio playback and audio file streaming (`filePath` stays hidden).
- Lyrics and timed **Song Lyric Line** endpoints (`song_lyrics` is untouched; the plain `lyrics` text field already on the Song is unchanged).
- Playlists, Song likes, Artist follows.
- Recommendations, advanced multi-field search, sorting options beyond the fixed newest-first order.
- Standalone Artist or Album endpoints (Artists and Albums appear only as expansions on a Song here).
- Frontend rendering, Vue integration, and duration formatting.
- Introducing an automated test harness (JUnit / Testcontainers). Verification stays manual at the HTTP seam.

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
