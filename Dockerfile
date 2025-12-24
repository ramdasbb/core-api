# Multi-stage Dockerfile for building and running the backend
FROM maven:3.9.4-eclipse-temurin-21 as build
WORKDIR /workspace
COPY . /workspace
RUN mvn -T1C -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the fat JAR - adjust path if your modules produce separate jars
COPY backend/auth-service/target/*-SNAPSHOT.jar /app/auth-service.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/auth-service.jar"]
