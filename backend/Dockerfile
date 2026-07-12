FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM tomcat:10.1-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/* \
    && mkdir -p \
        /usr/local/tomcat/uploads/songs \
        /usr/local/tomcat/uploads/covers \
        /usr/local/tomcat/uploads/avatars

COPY --from=build /app/target/melodyhub-backend.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
