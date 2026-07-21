# MelodyHub Backend

Java Servlet backend scaffold for MelodyHub.

## Requirements

- JDK 17+
- Maven 3.9+
- Tomcat 10.1+ or another Jakarta Servlet 6 compatible server

## Project Structure

```text
backend/
├── src/main/java/com/melodyHub/
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── exception
│   ├── filter
│   ├── repository
│   ├── service
│   └── util
├── src/main/resources
├── src/main/webapp/WEB-INF
├── uploads
│   ├── avatars
│   ├── covers
│   └── songs
└── pom.xml
```

## Build

```bash
mvn clean package
```

The WAR file is created at:

```text
target/melodyhub-backend.war
```

## Run On Tomcat

Copy `target/melodyhub-backend.war` into Tomcat's `webapps` folder, then start Tomcat.

Default health endpoint:

```text
GET /melodyhub-backend/api/health
```

## Configuration

Default values live in `src/main/resources/application.properties`.

You can override a property with a JVM system property:

```bash
-Dupload.base-dir=/absolute/path/to/uploads
```

## Run With Docker Compose

From the repo root:

```bash
cp .env.example .env
docker compose up --build
```

Docker Compose starts:

- MySQL 8.4 on `localhost:3306`
- automatic first-run schema initialization from `backend/src/main/resources/db/schema.sql`
- Tomcat 10.1 with the backend WAR deployed as `ROOT.war`

The backend is available at:

```text
http://localhost:8080/api/auth/...
```

The backend receives database and runtime settings through `CATALINA_OPTS` JVM system properties, matching the existing `AppConfig` override behavior.

ImageKit cover storage reads these environment variables directly:

```text
IMAGEKIT_PUBLIC_KEY
IMAGEKIT_PRIVATE_KEY
IMAGEKIT_URL_ENDPOINT
```

Set all three before using `ImageKitStorageService`. Keep the private key out of
source control and application logs. Cover uploads are stored under
`/artists/{artistSlug}/covers/`; ImageKit creates a missing folder when the first
file is uploaded to that path.

If you change the schema after MySQL has already initialized, reset the Docker volume before starting again:

```bash
docker compose down -v
docker compose up --build
```
