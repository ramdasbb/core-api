# Multi-stage Dockerfile for building and running auth-service
FROM maven:3.9.4-eclipse-temurin-21 as builder

WORKDIR /workspace

# Copy entire core_api
COPY . /workspace

# Build only auth-service module with dependencies
RUN mvn clean package -DskipTests -pl auth-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the JAR from builder - match actual artifact name
COPY --from=builder /workspace/auth-service/target/auth-service-*.jar auth-service.jar

EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8001/api/v1/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
