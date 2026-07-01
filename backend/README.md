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
