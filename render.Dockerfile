# Render.com Dockerfile for auth-service
FROM maven:3.9.4-eclipse-temurin-21 as builder

WORKDIR /app

# Copy entire core_api
COPY . .

# Build auth-service module only
RUN mvn clean package -DskipTests -pl auth-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy JAR from builder - match actual artifact name pattern
COPY --from=builder /app/auth-service/target/auth-service-*.jar auth-service.jar

EXPOSE 8001

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8001/api/v1/health || exit 1

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
