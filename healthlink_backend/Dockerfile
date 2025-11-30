# Build stage
FROM gradle:9.2.1-jdk21-alpine AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S healthlink && adduser -S healthlink -G healthlink

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Change ownership
RUN chown -R healthlink:healthlink /app

# Switch to non-root user
USER healthlink

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
