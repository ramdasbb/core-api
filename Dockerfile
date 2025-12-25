# Multi-stage Dockerfile for building and running auth-service
FROM maven:3.9.4-eclipse-temurin-21 as build
WORKDIR /workspace
COPY . /workspace
RUN mvn -T1C -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the auth-service JAR from the build stage
COPY --from=build /workspace/auth-service/target/*-SNAPSHOT.jar /app/auth-service.jar
EXPOSE 8001
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8001/api/v1/health || exit 1
ENTRYPOINT ["java","-jar","/app/auth-service.jar"]
