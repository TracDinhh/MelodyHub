# CLAUDE.md

Guidance for working in the MelodyHub repository.

## Project overview

MelodyHub is a web music application for browsing and managing a music catalog
(songs, albums, artists, playlists). It has two parts:

- **backend/** — Java 17 Servlet API (Jakarta Servlet 6) packaged as a WAR, run on Tomcat 10.1, backed by MySQL 8.4. Plain JDBC, no Spring, no ORM.
- **frontend/** — Vue 3 + Vite SPA using Bootstrap 5.

## Commands

### Backend (run from `backend/`)

```bash
mvn clean package          # build target/melodyhub-backend.war
```

There is no test suite yet. If you add tests, use JUnit 5 and run with `mvn test`.

### Frontend (run from `frontend/`)

```bash
npm install
npm run dev                # Vite dev server on 0.0.0.0
npm run build              # production build to dist/
npm run preview            # preview the production build
```

### Full stack via Docker (run from repo root)

```bash
cp .env.example .env
docker compose up --build  # MySQL on :3306, backend (ROOT.war) on :8080
```

The backend is served at `http://localhost:8080/api/...`. MySQL initializes on
first run from `backend/src/main/resources/db/schema.sql`. If you change the
schema after MySQL has initialized, reset the volume first:

```bash
docker compose down -v && docker compose up --build
```

## Backend architecture

Package root: `com.melodyHub`. Layered flow:

```
controller (Servlet)  →  service  →  repository  →  MySQL (JDBC)
        ↑ dto/request/response          ↑ entity
```

- **controller/** — `HttpServlet` subclasses (`AuthServlet`, `SongServlet`). Routing is done by inspecting `request.getPathInfo()` inside `doGet`/`doPost`. Servlets are mapped in `src/main/webapp/WEB-INF/web.xml` (e.g. `/api/auth/*`, `/api/songs/*`), not with `@WebServlet` annotations. Each servlet owns a Jackson `ObjectMapper` (with `JavaTimeModule`, dates as ISO strings not timestamps) and private `writeJson` / `writeError` helpers.
- **service/** — Business logic. Constructor-injectable (a no-arg constructor wires the default repository; a second constructor takes a dependency for testing). Uses `Objects.requireNonNull` guards.
- **repository/** — Plain JDBC. Always use `PreparedStatement` with parameters (never string-concatenated SQL) and try-with-resources for connections/statements/result sets. Maps `ResultSet` rows to entities manually, handling nullable columns and `Timestamp → LocalDateTime`.
- **entity/** — Domain models using Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`). Enums like `SongStatus`, `UserRole`, `UserStatus` parse from DB string values.
- **dto/** — `request/` and `response/` records/classes. Response DTOs expose a static `fromEntity(...)` mapper and deliberately omit sensitive/internal fields (e.g. `SongResponse` drops `filePath` and `deletedAt`).
- **config/** — `AppConfig` reads `application.properties`, overridable per key via JVM system properties (`-Dkey=value`). `DatabaseConfig` opens JDBC connections via `DriverManager`.
- **filter/** — `CorsFilter` mapped to `/api/*`.
- **util/** — `JwtUtil` (auth0 java-jwt), `PasswordUtil` (jBCrypt).
- **exception/** — `AuthException` carries a string `code`; servlets map codes to HTTP status.

### Error responses

Errors return `ErrorResponse(code, message)` as JSON. Auth error codes map to
HTTP status in `AuthServlet.getStatusCode` (e.g. `INVALID_CREDENTIALS` → 401,
`USERNAME_EXISTS` → 409, `USER_BANNED` → 403). `SQLException` → 500
`DATABASE_ERROR`; malformed JSON body → 400 `INVALID_JSON`.

### Configuration keys

Defaults live in `backend/src/main/resources/application.properties`; Docker
overrides them via `CATALINA_OPTS` system properties. Keys: `db.url`,
`db.username`, `db.password`, `db.driver-class-name`, `upload.base-dir`,
`cors.allowed-origin`, `jwt.secret`, `jwt.expires-minutes`, `app.name`.

## Database

Full schema in `backend/src/main/resources/db/schema.sql` (MySQL, `utf8mb4`).
Tables: `users`, `artists`, `albums`, `songs`, `song_lyrics`, `song_artists`,
`genres`, `song_genres`, `playlists`, `playlist_songs`, `song_likes`,
`artist_follows`, `listen_history`. Notes:

- Passwords stored as BCrypt hashes in `users.password_hash` — never plaintext.
- Soft deletes via a nullable `deleted_at` column (queries filter `deleted_at IS NULL`).
- `songs.status` is `DRAFT | PUBLISHED | HIDDEN`; listener-facing queries filter `status = 'PUBLISHED'`.
- Slugs are unique and URL-facing.

## Conventions

- Java: 4-space indent, `final` utility classes with private constructors, constants as `private static final`, prefer immutable/injectable services.
- Match the existing style of the file and package you're editing before introducing new patterns or dependencies.
- Domain vocabulary is defined in [CONTEXT.md](md/CONTEXT.md) — use those terms (e.g. "Song", "Published Song", "Album", "Artist") and avoid the listed alternatives.

## Git workflow

See [docs/gitflow/git.md](docs/gitflow/git.md). Summary:

- Never commit directly to `main`. Branch from up-to-date `main`.
- Branch names: `<type>/<member>/<feature>` (e.g. `feat/tracdinh/song-management`). Types: `feat`, `fix`, `refactor`, `docs`, `style`, `chore`.
- Commit messages: `<type>: <short description>` (e.g. `feat: create song api`).
- One feature per branch; merge into `main` via Pull Request.
