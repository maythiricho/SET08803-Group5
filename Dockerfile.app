# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# Preload dependencies (faster CI)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Build project
COPY src ./src
RUN mvn -q -DskipTests clean package
# This produces target/devops.jar via maven-assembly-plugin

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the fat JAR built by Maven (devops.jar)
COPY --from=build /src/target/devops.jar ./devops.jar

# Default DB connection (can be overridden by env/args)
ENV DB_HOST=db DB_PORT=3306 DB_NAME=world DB_USER=app DB_PASSWORD=app123

# Run the application; we also pass db:3306 and timeout as args
ENTRYPOINT ["java", "-jar", "devops.jar", "db:3306", "30000"]
