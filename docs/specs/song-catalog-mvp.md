# Song Catalog MVP

## Problem Statement

MelodyHub has working authentication, but after a user can register and log in, the app does not yet expose real music content from the database. The frontend currently shows mock song rows, so the project cannot prove the core web music flow of retrieving and displaying songs that already exist in the system.

The next feature needs to be small, useful, and aligned with the current Servlet + JDBC backend. It should establish the public song-reading path without adding playlist management, likes, uploads, or admin workflows.

## Solution

Build a Song Catalog MVP that returns published songs from the existing song data model and displays them in the frontend instead of hardcoded mock data.

The backend will expose a read-only songs endpoint. The endpoint will return only songs that are published and not soft-deleted. Each song in the response will include basic song metadata, artist information, album information when available, cover image, duration, play count, and timestamps needed for display.

The frontend will call this endpoint and render the returned catalog in the current dashboard/song-list experience. Loading, empty, and error states should be handled simply so the user can tell whether the API is working.

## User Stories

1. As a visitor, I want to see published songs, so that I can browse available music before deeper account features exist.
2. As a logged-in user, I want to see the same published songs, so that login leads to real music content.
3. As a music listener, I want each song to show its title, so that I can identify the track.
4. As a music listener, I want each song to show its primary artist, so that I know who performed it.
5. As a music listener, I want featured artists to be included when a song has them, so that collaborations are represented correctly.
6. As a music listener, I want each song to show its album when it belongs to one, so that I understand where the song comes from.
7. As a music listener, I want standalone singles to work without an album, so that songs are not hidden just because the album field is empty.
8. As a music listener, I want each song to show a cover image when available, so that the catalog feels like a music app rather than a plain database table.
9. As a music listener, I want songs without their own cover image to still display cleanly, so that incomplete metadata does not break the catalog.
10. As a music listener, I want each song to show duration, so that I know how long the track is.
11. As a music listener, I want duration to be formatted in a readable way on the frontend, so that raw seconds do not leak into the UI.
12. As a music listener, I want the catalog to include play count, so that popular songs can be shown later without changing the response shape.
13. As a music listener, I want only published songs to appear, so that drafts and hidden content are not visible.
14. As a content owner, I want hidden songs excluded, so that removed or moderated content is not shown to users.
15. As a content owner, I want draft songs excluded, so that unfinished content is not shown to users.
16. As a content owner, I want soft-deleted songs excluded, so that deleted catalog items stay out of normal browsing.
17. As a frontend developer, I want one simple songs endpoint, so that I can replace mock data without building several screens at once.
18. As a frontend developer, I want a predictable JSON response shape, so that rendering code stays simple.
19. As a frontend developer, I want nullable album fields when a song has no album, so that singles are easy to handle.
20. As a frontend developer, I want artists returned in display order, so that the UI can show the main artist first and featured artists after.
21. As a backend developer, I want the catalog query to use the existing schema, so that this feature does not require a migration.
22. As a backend developer, I want a repository dedicated to reading songs, so that song data access is separate from auth data access.
23. As a backend developer, I want a service layer for catalog rules, so that publication and deletion filtering has a clear home.
24. As a backend developer, I want a servlet endpoint for song retrieval, so that the feature follows the current backend style.
25. As a backend developer, I want the songs endpoint registered through the existing web application mapping style, so that routing remains consistent.
26. As a backend developer, I want database errors returned as stable JSON errors, so that the frontend can handle failures consistently.
27. As a backend developer, I want invalid query parameters rejected consistently if pagination or limits are added, so that the API contract is predictable.
28. As a tester, I want to verify the endpoint through HTTP behavior, so that tests cover the user-visible contract instead of private implementation details.
29. As a tester, I want seeded published and non-published songs to behave differently, so that visibility rules are proven.
30. As a tester, I want album and no-album songs covered, so that both catalog shapes are safe.
31. As a tester, I want main and featured artists covered, so that artist ordering is proven.
32. As a learner building the project, I want this feature to stay small, so that I can understand the backend flow before adding playlists, likes, uploads, or admin tools.

## Implementation Decisions

- This MVP is a read-only Song Catalog feature.
- The feature will use the existing relational music schema. No schema changes are required for the MVP.
- The feature will introduce the domain concepts needed to read songs: Song, Artist, Album, and Published Song.
- A Song is a track in the catalog.
- A Published Song is a Song whose status allows it to appear in normal user-facing catalog views.
- An Artist is a performer connected to a Song. Songs may have a main artist and featured artists.
- An Album is an optional release container for a Song. A Song can exist without an Album.
- The backend will expose a `GET /api/songs` endpoint.
- The endpoint will be public for this MVP. It does not require a bearer token because the catalog is browseable content.
- The endpoint will return only songs with published status.
- The endpoint will exclude soft-deleted songs.
- The endpoint will exclude draft songs.
- The endpoint will exclude hidden songs.
- The endpoint will include songs with no album.
- The endpoint will include artist data for each song.
- Artist data will include at least id, name, slug, role, and display position.
- The first main artist should be easy for the frontend to display as the primary artist.
- Album data will include at least id, title, slug, cover image, album type, and release date when the song has an album.
- Song data will include at least id, title, slug, duration in seconds, cover image, play count, created timestamp, and updated timestamp.
- The response may include the song file path only if the frontend needs it for a later playback slice. This MVP is catalog display, not playback, so the preferred response excludes direct audio file path exposure for now.
- Cover image should prefer the song cover when present. The frontend may fall back to the album cover when the song cover is absent.
- Lyrics and timed lyric lines are out of scope for this MVP.
- Playlist ownership and playlist membership are out of scope for this MVP.
- Song likes and artist follows are out of scope for this MVP.
- Listen history and play counting behavior are out of scope for this MVP.
- Uploading songs, covers, or avatars is out of scope for this MVP.
- Admin creation, approval, hiding, and editing flows are out of scope for this MVP.
- The backend implementation should follow the existing Servlet + service + JDBC repository style.
- The frontend should replace the current mock song list with data fetched from the new endpoint.
- The frontend should show a loading state while the song catalog request is in flight.
- The frontend should show an empty state when there are no published songs.
- The frontend should show a simple error state when the catalog cannot be loaded.
- The frontend should avoid adding a full routing system unless necessary for this MVP.
- The current dashboard/table can be adapted for the MVP; a polished multi-page music experience is not required yet.
- The API should return JSON and use the same general JSON response/error style already used by auth.
- CORS behavior should continue to be handled by the existing API-wide CORS layer.
- Authentication logic should not be mixed into the Song Catalog endpoint for this MVP.
- The default ordering should be deterministic. Recommended order: newest published songs first, then id descending as a tie-breaker.
- Pagination is optional for the first implementation if the dataset is tiny, but the response and repository should not make future pagination hard.
- If pagination is included now, use simple `page` and `size` query parameters with conservative defaults.
- If pagination is deferred, cap the number of returned songs with a simple default limit to avoid accidentally returning an unbounded catalog later.

## Testing Decisions

- The highest-value test seam is the HTTP API contract for `GET /api/songs`.
- Good tests should assert externally visible behavior: response status, JSON shape, filtering rules, null album handling, artist ordering, and error behavior.
- Tests should avoid asserting private method names, SQL string formatting, or internal helper behavior.
- Backend verification should include the existing Maven package/build gate used for the auth work.
- Backend behavior should be manually smoke-tested through an HTTP request against the deployed servlet when a local database is available.
- The songs endpoint should be tested with at least one published song, one draft song, one hidden song, and one soft-deleted song.
- The songs endpoint should be tested with one song that has an album and one song that does not.
- The songs endpoint should be tested with one song that has a main artist and a featured artist.
- The frontend should be verified with the normal Vite build.
- The frontend should be manually checked for loading, empty, success, and error states if a dev server is run.
- Existing auth tests are not present, so this feature should not depend on a broad test suite being available before implementation.

## Out of Scope

- Playlist creation, editing, deletion, ordering, and public/private playlist browsing.
- Liking songs.
- Following artists.
- Listening history.
- Incrementing play counts.
- Audio playback.
- Lyrics and synchronized lyric display.
- Song upload.
- Cover upload.
- Avatar upload.
- Admin dashboard behavior.
- Song moderation or approval workflows.
- Creating, editing, hiding, or deleting songs through the UI.
- Creating, editing, hiding, or deleting artists or albums through the UI.
- Search and advanced filters.
- Genre browsing.
- Recommendations.
- Authentication changes.
- Role and permission changes.

## Further Notes

- This feature is intentionally the smallest real music slice after auth.
- The implementation should stay task-by-task and easy to review.
- The backend should continue using manual JDBC because that is the current project style.
- The frontend should keep the current Bootstrap/Vue setup and avoid a redesign unless the user asks for it.
- A later slice can add playback after the catalog is visible.
- A later slice can add admin song management after the public read path is stable.
- A later slice can add playlist behavior once songs can be reliably listed and selected.
