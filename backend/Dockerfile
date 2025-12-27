# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine-3.23 AS builder
WORKDIR /workspace

# Copy gradle wrapper and config first (caching layer)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jdk-alpine-3.23
WORKDIR /app

# create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /var/log/quickclock && chown spring:spring /var/log/quickclock
USER spring:spring

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]