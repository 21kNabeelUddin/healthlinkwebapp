### Multi-stage build for Spring Boot on Render

# Build stage
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app
# Build context is the repo root; copy only the backend folder so gradlew is present at /app/gradlew
COPY healthlink_backend/ .
# Ensure Gradle wrapper is executable; skip tests for faster deploy (remove -x test if desired)
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]

