# SmartVillage Backend

A multi-module Maven backend for SmartVillage using Spring Boot 3.x (Java 21) and Scala/Akka for async operations.

## Modules

- **auth-service** – User authentication (signup/login with JWT)
- **notice-board-service** – Notice and announcement management
- **services-directory-service** – Village services directory (shops, healthcare, etc.)
- **feedback-service** – Feedback and grievance submissions
- **payment-service** – Mock tax payment endpoints
- **dev-work-service** – Development work tracking
- **rating-service** – Service ratings (future async with Akka)
- **rating-akka** – Scala/Akka async module
- **village-core-service** – Core village information
- **common-utils** – Shared utilities

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **PostgreSQL 14+** (local or cloud)

## Quick Start

### 1. Setup PostgreSQL (Local)

```bash
# Create database
createdb smartvillage

# Or use Docker:
docker run --name smartvillage-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:15
```

### 2. Build All Modules

```bash
cd backend
mvn -T1C clean package
```

### 3. Build Auth Service Only

```bash
mvn -pl auth-service -am clean package
```

## Running Auth Service

### Option A: Maven (Recommended for Development)

```bash
mvn -pl auth-service spring-boot:run
```

The service will start on **http://localhost:8081** with Swagger UI at:
```
http://localhost:8081/swagger-ui.html
```

### Option B: Java (After Building)

```bash
java -jar auth-service/target/auth-service-0.1.0.jar
```

### Option C: Docker

```bash
mvn clean package
docker build -t smartvillage-backend:latest -f Dockerfile .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/smartvillage \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  smartvillage-backend:latest
```

## Configuration

Auth service reads from `auth-service/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smartvillage
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update  # change to 'validate' in production

server:
  port: 8081

jwt:
  secret: change-me-super-secret          # override via JWT_SECRET env var
  expiration-ms: 86400000                 # 24 hours

springdoc:
  api-docs:
    path: /v3/api-docs
```

**Environment Variables** (override YAML):
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smartvillage
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=your-secret-key
```

## Auth API Endpoints

### 1. Sign Up

```bash
curl -X POST http://localhost:8081/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword",
    "name": "John Doe"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "name": "John Doe"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  "name": "John Doe"
}
```

### 3. Use Token (Protected Endpoints)

Add the token to request headers:
```bash
curl -X GET http://localhost:8081/api/v1/protected-endpoint \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Running Other Services

Each module has its own `application.yml` and port:

- **notice-board-service** – Port 8082
  ```bash
  mvn -pl notice-board-service spring-boot:run
  ```

- **services-directory-service** – Port 8083
  ```bash
  mvn -pl services-directory-service spring-boot:run
  ```

- **feedback-service** – Port 8084
  ```bash
  mvn -pl feedback-service spring-boot:run
  ```

- **payment-service** – Port 8085
  ```bash
  mvn -pl payment-service spring-boot:run
  ```

- **dev-work-service** – Port 8086
  ```bash
  mvn -pl dev-work-service spring-boot:run
  ```

- **rating-akka** (Scala/Akka) – Port 9000
  ```bash
  mvn -pl rating-akka compile exec:java -Dexec.mainClass=com.smartvillage.ratingakka.Server
  ```

## Database Migrations (Flyway)

Migrations are in `{module}/src/main/resources/db/migration/`:

- `auth-service/...V1__create_users.sql` – Creates `users` table
- `notice-board-service/.../V1__create_notices.sql` – Creates `notices` table

Migrations run automatically on service startup. Add more migrations with `V2__...sql`, `V3__...sql` etc.

## Running Tests

```bash
# Run all tests
mvn clean test

# Run auth-service tests only
mvn -pl auth-service test
```

## Deployment

### Render.com (Recommended Free Tier)

1. Create a new PostgreSQL database on [Neon.tech](https://neon.tech) (free tier)
2. Create a new Web Service on [Render.com](https://render.com)
   - Build command: `mvn -T1C -DskipTests clean package`
   - Start command: `java -jar auth-service/target/auth-service-0.1.0.jar`
3. Add environment variables:
   ```
   SPRING_DATASOURCE_URL=postgresql://...
   SPRING_DATASOURCE_USERNAME=...
   SPRING_DATASOURCE_PASSWORD=...
   JWT_SECRET=your-production-secret
   ```

### Railway.app

Similar setup to Render. Reference [Railway docs](https://docs.railway.app/).

## Troubleshooting

**Port already in use:**
```bash
# Change port in application.yml or use env var:
export SERVER_PORT=8081
mvn -pl auth-service spring-boot:run
```

**PostgreSQL connection refused:**
- Ensure PostgreSQL is running: `psql -U postgres -c "SELECT 1"`
- Check connection string in `application.yml` or environment variables
- For Docker, use `host.docker.internal:5432` or create a Docker network

**JWT token invalid:**
- Ensure `jwt.secret` matches between signup and protected requests
- Check token expiration: default is 24 hours (`jwt.expiration-ms: 86400000`)

## Next Steps

- Add validation (Jakarta Validation annotations)
- Add unit and integration tests
- Implement refresh tokens
- Add API rate limiting
- Setup CI/CD with GitHub Actions
- Add frontend integration (CORS configuration)
