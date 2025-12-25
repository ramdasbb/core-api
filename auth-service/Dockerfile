# Build stage
FROM maven:3.8.7-openjdk-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-slim

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/auth-service-*.jar app.jar

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
