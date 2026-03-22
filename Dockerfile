# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN apt-get update && apt-get install -y maven && mvn package -DskipTests -B

# ─── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

# ECS injects SERVER_PORT=8080 at runtime; local dev uses docker-compose override.
EXPOSE 8081

# Health check: ECS always sets SERVER_PORT=8080 via task-definition env vars.
# The shell expands ${SERVER_PORT:-8080} so local non-zero ports also work.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- "http://localhost:${SERVER_PORT:-8081}/actuator/health" || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
