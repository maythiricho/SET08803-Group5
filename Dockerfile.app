FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Preload dependencies
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Build project
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy shaded JAR built by Maven
COPY --from=build /src/target/*-shaded.jar /target/devops.jar

# Default DB connection (overridden by Compose env)
ENV DB_HOST=db DB_PORT=3306 DB_NAME=world DB_USER=app DB_PASSWORD=app123

ENTRYPOINT ["java","-jar","/target/devops.jar"]
