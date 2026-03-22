# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first (better layer caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ─── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Expose application port
EXPOSE 8081

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
