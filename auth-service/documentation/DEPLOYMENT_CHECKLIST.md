# AUTH SERVICE - DEPLOYMENT CHECKLIST

## Pre-Deployment Validation

### Code Quality
- [x] All 20 endpoints implemented
- [x] Consistent error handling
- [x] Proper HTTP status codes
- [x] Input validation on all endpoints
- [x] Security best practices applied
- [x] Code follows Spring Boot conventions
- [x] Dependencies managed via Maven

### Database
- [x] Flyway migrations created and tested
- [x] All tables with proper constraints
- [x] Indexes on foreign keys
- [x] System data seeding included
- [x] Case-insensitive email validation
- [x] Audit log table created
- [x] Refresh token table created

### Security
- [x] JWT authentication filter implemented
- [x] Spring Security configured
- [x] CORS properly configured
- [x] Password hashing with Bcrypt
- [x] Token expiration enforced
- [x] Soft deletion for data preservation
- [x] Public endpoint whitelist defined

### Configuration
- [x] application.yml configured
- [x] JWT settings configured
- [x] Database connection settings ready
- [x] Flyway enabled
- [x] Logging configured
- [x] Server port set to 8001

### Documentation
- [x] API documentation complete
- [x] Implementation progress tracked
- [x] Architecture documented
- [x] Endpoints documented with examples
- [x] Error codes documented
- [x] Token claims documented

---

## Pre-Deployment Checklist

### 1. Environment Setup
- [ ] PostgreSQL server running
- [ ] PostgreSQL version 14+ (or compatible)
- [ ] Java 21 installed and configured
- [ ] Maven 3.8+ installed
- [ ] Git repository cloned

### 2. Database Preparation
```bash
# Create database
createdb smartvillage

# Verify connection
psql -U postgres -d smartvillage -c "SELECT version();"
```

- [ ] Database created
- [ ] User has appropriate permissions
- [ ] Database is accessible from application host

### 3. Build Verification
```bash
# Navigate to auth-service directory
cd core_api/auth-service

# Clean and build
mvn clean install -DskipTests

# Verify JAR created
ls -la target/auth-service-*.jar
```

- [ ] Build succeeds without errors
- [ ] All dependencies resolved
- [ ] JAR file created

### 4. Configuration Verification
- [ ] JWT secret set to production value (min 32 bytes)
- [ ] Database credentials correct
- [ ] Server port (8001) not in use
- [ ] Logging level set appropriately
- [ ] All required environment variables set

### 5. Security Review
- [ ] JWT secret is strong (minimum 32 bytes)
- [ ] Password hashing rounds set to 12+
- [ ] CORS origins properly restricted (if needed)
- [ ] Public endpoints are only signup/login
- [ ] Authentication filter active
- [ ] No hardcoded credentials in code

### 6. Test Run
```bash
# Run application
mvn spring-boot:run

# Verify startup logs
# Look for: "Started AuthServiceApplication in ... seconds"

# Test signup endpoint (public)
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "full_name": "Test User",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'

# Expected response: 201 Created
```

- [ ] Application starts successfully
- [ ] Signup endpoint returns 201
- [ ] Error handling works properly

### 7. Database Migration Verification
```sql
-- Connect to database
psql -U postgres -d smartvillage

-- Verify tables created
\dt

-- Expected tables:
-- - users
-- - roles
-- - permissions
-- - user_roles
-- - role_permissions
-- - refresh_tokens
-- - audit_logs
```

- [ ] All 7 tables created
- [ ] Constraints properly applied
- [ ] Indexes created
- [ ] System roles inserted
- [ ] System permissions inserted

### 8. Flyway Verification
```sql
-- Check migration history
SELECT * FROM flyway_schema_history;

-- Should show:
-- V1 | create_auth_schema | Success
```

- [ ] Flyway migration completed
- [ ] V1__create_auth_schema.sql applied
- [ ] No migration errors

---

## Deployment Steps

### Docker Deployment (Recommended)

#### 1. Build Docker Image
```dockerfile
FROM openjdk:21-slim

WORKDIR /app

COPY target/auth-service-*.jar auth-service.jar

EXPOSE 8001

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/smartvillage
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV JWT_SECRET=your-production-secret-key-min-32-bytes

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
```

```bash
# Build image
docker build -t auth-service:latest .

# Verify image
docker images | grep auth-service
```

- [ ] Dockerfile created
- [ ] Docker image built successfully
- [ ] Image tag applied

#### 2. Docker Compose Setup
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: smartvillage
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  auth-service:
    image: auth-service:latest
    ports:
      - "8001:8001"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/smartvillage
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      JWT_SECRET: your-production-secret-key-min-32-bytes
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

```bash
# Start services
docker-compose up -d

# Check logs
docker-compose logs -f auth-service

# Verify health
docker-compose ps
```

- [ ] docker-compose.yml created
- [ ] Services start successfully
- [ ] Database is healthy
- [ ] Auth service connects to database

### Kubernetes Deployment (Optional)

#### ConfigMap for Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-service-config
  namespace: default
data:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-service:5432/smartvillage
  SPRING_DATASOURCE_USERNAME: postgres
  JWT_SECRET: your-production-secret-key-min-32-bytes
```

#### Deployment Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: default
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: auth-service:latest
        ports:
        - containerPort: 8001
        envFrom:
        - configMapRef:
            name: auth-service-config
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /api/v1/health
            port: 8001
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: default
spec:
  selector:
    app: auth-service
  type: ClusterIP
  ports:
  - port: 8001
    targetPort: 8001
```

```bash
# Deploy to Kubernetes
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml

# Verify deployment
kubectl get deployment auth-service
kubectl get pods -l app=auth-service
kubectl logs -f deployment/auth-service
```

- [ ] Kubernetes manifests created
- [ ] ConfigMap created
- [ ] Deployment created
- [ ] Service created
- [ ] Replicas running

---

## Post-Deployment Validation

### 1. Health Check
```bash
# Health endpoint (should be available)
curl http://localhost:8001/api/v1/health

# Expected: 200 OK (or endpoint should be created for monitoring)
```

- [ ] Service is accessible
- [ ] Health checks pass

### 2. Database Connectivity
```bash
# Application logs should show:
# - "Started AuthServiceApplication in X.XXX seconds"
# - "HikariPool-1 - Connection is valid"
# - No database connection errors
```

- [ ] Database connection successful
- [ ] Migrations applied automatically
- [ ] No connection pool errors

### 3. API Functionality Tests
```bash
# Test 1: Signup
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "deploy-test@example.com",
    "password": "TestPass123!",
    "full_name": "Deploy Test",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'

# Expected: 201 Created

# Test 2: Login
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "deploy-test@example.com",
    "password": "TestPass123!"
  }'

# Expected: 200 OK with tokens

# Test 3: Protected Endpoint
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {access_token_from_login}"

# Expected: 200 OK with user profile
```

- [ ] Signup endpoint works (201)
- [ ] Login endpoint works (200)
- [ ] Protected endpoint requires token (401 without)
- [ ] Token validation works
- [ ] User data persists

### 4. Audit Logging
```sql
-- Check audit logs
SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10;

-- Should show logs for signup, login operations
```

- [ ] Audit logs created
- [ ] User actions recorded
- [ ] Timestamps accurate

### 5. Security Verification
```bash
# Test 1: Unauthorized access to protected endpoint
curl -X GET http://localhost:8001/api/v1/admin/users
# Expected: 401 Unauthorized

# Test 2: Invalid token
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer invalid-token"
# Expected: 401 Unauthorized

# Test 3: Duplicate email
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "deploy-test@example.com",
    "password": "AnotherPass123!",
    "full_name": "Another User",
    "mobile": "9876543211",
    "aadhar_number": "123456789013"
  }'
# Expected: 409 Conflict
```

- [ ] Unauthorized access denied (401)
- [ ] Invalid tokens rejected
- [ ] Duplicate emails prevented
- [ ] Proper error responses returned

### 6. Performance Baseline
```bash
# Load test with simple requests
ab -n 100 -c 10 http://localhost:8001/api/v1/health

# Record baseline:
# - Requests/sec: ___
# - Avg response time: ___ ms
# - 95th percentile: ___ ms
```

- [ ] Response time acceptable
- [ ] No timeouts under load
- [ ] Error rate < 1%

### 7. Log Aggregation
- [ ] Logs are properly formatted
- [ ] Log levels appropriate (INFO, WARN, ERROR)
- [ ] Sensitive data not logged (passwords, tokens)
- [ ] Structured logging for parsing

---

## Monitoring Setup

### Metrics to Monitor
```
- HTTP request count and latency
- Database connection pool status
- JWT token validation success/failure
- User registration and login counts
- Audit log growth
- Error rates by endpoint
- Permission denial incidents
```

### Alerts to Configure
```
- Application restart/crash
- Database connection failures
- High error rate (>5%)
- Slow response times (>1s)
- JWT validation failures spike
- Failed authentication attempts spike
```

### Health Check Configuration
```
- Create /api/v1/health endpoint
- Check database connectivity
- Check JWT configuration
- Monitor Flyway migration status
```

---

## Rollback Plan

### If Deployment Fails

1. **Immediate Actions**
   - [ ] Rollback application to previous version
   - [ ] Keep database intact (no data loss)
   - [ ] Notify team of incident

2. **Database Recovery**
   ```bash
   # Flyway can handle rollback gracefully
   # Previous version should be compatible
   ```
   - [ ] Database schema compatible
   - [ ] No migration rollback needed (if only app version changed)

3. **Service Recovery**
   ```bash
   # Stop failed service
   docker-compose down auth-service
   
   # Revert to previous image
   docker pull auth-service:previous
   docker-compose up -d auth-service:previous
   ```
   - [ ] Previous version redeployed
   - [ ] Connections restored
   - [ ] Services validated

---

## Go-Live Checklist

### Week Before
- [ ] Final code review completed
- [ ] All tests passed
- [ ] Performance testing completed
- [ ] Security audit completed
- [ ] Database backup strategy confirmed
- [ ] Monitoring and alerting configured
- [ ] Runbook documentation prepared

### Day Before
- [ ] Database backups taken
- [ ] Staging environment validated
- [ ] Team briefed on deployment plan
- [ ] Rollback plan reviewed
- [ ] On-call engineer identified

### Deployment Day
- [ ] Deployment window scheduled
- [ ] Team members online and ready
- [ ] Communication channel active
- [ ] Database backed up again
- [ ] Application deployed
- [ ] All validation tests passed
- [ ] No incidents reported

### Post-Deployment (24 hours)
- [ ] Monitor error logs
- [ ] Check user feedback
- [ ] Verify audit logs
- [ ] Validate performance metrics
- [ ] Confirm no data loss
- [ ] Team debriefing scheduled

---

## Critical Contacts

| Role | Name | Phone | Email |
|------|------|-------|-------|
| DevOps Lead | | | |
| Backend Lead | | | |
| Database Admin | | | |
| On-Call Engineer | | | |
| Team Lead | | | |

---

## Documentation Links

- [API Documentation](API_DOCUMENTATION.md)
- [Implementation Progress](IMPLEMENTATION_PROGRESS.md)
- [Architecture Design](AUTH_SERVICE_DESIGN.md)
- [Phase 6 Completion](PHASE_6_COMPLETION.md)

---

## Sign-Off

- [ ] Code Review: ________ (Date: _______)
- [ ] QA Testing: ________ (Date: _______)
- [ ] Security Review: ________ (Date: _______)
- [ ] Architecture Review: ________ (Date: _______)
- [ ] Deployment Approval: ________ (Date: _______)

---

**Status**: Ready for Deployment  
**Last Updated**: 2024-01-15  
**Version**: 1.0
