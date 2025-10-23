# --- Stage 1: Build ---
# Use a base image with Maven and Java to build the app
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies first
# This uses Docker's layer caching effectively
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code and build the application
COPY src ./src
RUN mvn package -DskipTests

# --- Stage 2: Run ---
# Use a lightweight JRE-only image for the final production container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR file from the 'build' stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
