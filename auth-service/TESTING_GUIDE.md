# Auth Service - Testing Guide

Complete testing documentation covering unit tests, integration tests, and manual testing.

## Test Setup

### Prerequisites
```bash
# Install dependencies
mvn clean install

# Run tests
mvn test

# Run tests with coverage
mvn test jacoco:report
```

### Test Configuration
Tests use `application-test.yml` with:
- H2 in-memory database (fast)
- Flyway migrations enabled
- Debug logging enabled
- Test-specific JWT secret

## Unit Tests

### Running Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testRegisterUserSuccess

# Run with verbose output
mvn test -X
```

### Test Classes

#### 1. UserServiceTest (10 tests)
Location: `src/test/java/com/smartvillage/authservice/service/UserServiceTest.java`

Tests:
- ✅ Register user with valid data
- ✅ Register user with duplicate email (exception)
- ✅ Find user by email
- ✅ User not found scenario
- ✅ Approve pending user
- ✅ Reject user
- ✅ Soft delete user
- ✅ Assign roles to user
- ✅ Exception handling for missing approver
- ✅ Exception handling for missing user

```bash
mvn test -Dtest=UserServiceTest
```

#### 2. RBACServiceTest (10 tests)
Location: `src/test/java/com/smartvillage/authservice/service/RBACServiceTest.java`

Tests:
- ✅ Create permission successfully
- ✅ Create permission with duplicate (exception)
- ✅ Get permissions for regular user
- ✅ Get permissions for super admin (all permissions)
- ✅ Create role successfully
- ✅ Create role with duplicate (exception)
- ✅ Assign permissions to role
- ✅ Check user has permission (true)
- ✅ Check user has permission (false)
- ✅ Identify super admin correctly
- ✅ Identify non-super admin correctly
- ✅ Remove permission from role

```bash
mvn test -Dtest=RBACServiceTest
```

### Expected Unit Test Results

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 5.234 s
BUILD SUCCESS
```

## Integration Tests

### Running Integration Tests

```bash
# Run all integration tests
mvn test -Dtest=*IntegrationTest

# Run specific integration test class
mvn test -Dtest=AuthControllerIntegrationTest

# Run specific test
mvn test -Dtest=AuthControllerIntegrationTest#testSignupSuccess
```

### Test Classes

#### 1. AuthControllerIntegrationTest (6 tests)
Location: `src/test/java/com/smartvillage/authservice/controller/AuthControllerIntegrationTest.java`

Tests:
- ✅ POST /auth/signup - Register user successfully
- ✅ POST /auth/signup - Duplicate email rejection
- ✅ POST /auth/login - Login with valid credentials
- ✅ POST /auth/login - Reject unapproved user
- ✅ GET /auth/me - Get profile with valid token
- ✅ GET /auth/me - Reject without token
- ✅ POST /auth/logout - Revoke refresh token

```bash
mvn test -Dtest=AuthControllerIntegrationTest
```

### Expected Integration Test Results

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 12.456 s
BUILD SUCCESS
```

## Test Coverage

### Generate Coverage Report

```bash
# Run tests with coverage
mvn test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Targets
- **Service Layer**: 85%+ coverage
- **Controller Layer**: 80%+ coverage
- **Repository Layer**: 70%+ coverage (mostly data access)
- **Overall**: 80%+ coverage

## Manual Testing

### 1. Local Testing

#### Start Application
```bash
# Terminal 1: Start PostgreSQL
docker run --name smartvillage-postgres \
  -e POSTGRES_DB=smartvillage \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Terminal 2: Run application
mvn spring-boot:run
```

#### Test Signup
```bash
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@example.com",
    "password": "SecurePass123!",
    "full_name": "Test User 1",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'

# Expected: 201 Created
```

#### Test Login (Should Fail - Not Approved)
```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@example.com",
    "password": "SecurePass123!"
  }'

# Expected: 403 Forbidden (USER_NOT_APPROVED)
```

#### Approve User in Database
```bash
# Connect to database
psql postgresql://postgres:postgres@localhost:5432/smartvillage

# Approve user
UPDATE users SET approval_status = 'approved' 
WHERE email = 'user1@example.com';

# Exit psql
\q
```

#### Test Login Again
```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@example.com",
    "password": "SecurePass123!"
  }'

# Expected: 200 OK with tokens
# Response includes:
# - access_token (15 min expiry)
# - refresh_token (7 day expiry)
# - user profile
```

#### Test Get Profile
```bash
# Replace with actual token from login response
export TOKEN="eyJhbGciOiJIUzI1NiIs..."

curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Expected: 200 OK with user profile
```

### 2. Docker Testing

#### Build Docker Image
```bash
docker build -f core_api/auth-service/Dockerfile \
  -t auth-service:latest \
  core_api/auth-service/
```

#### Run with Docker Compose
```bash
docker-compose up -d

# Wait for services to start
sleep 10

# Test health check
curl http://localhost:8001/api/v1/health

# Test signup
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "docker-test@example.com",
    "password": "DockerPass123!",
    "full_name": "Docker Test",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'

# Stop services
docker-compose down
```

### 3. API Testing with Postman

#### Import Collection
```json
{
  "info": {
    "name": "Auth Service API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Signup",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/auth/signup",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"{{$randomEmail}}\",\n  \"password\": \"Pass123!\",\n  \"full_name\": \"Test User\",\n  \"mobile\": \"9876543210\",\n  \"aadhar_number\": \"123456789012\"\n}"
        }
      }
    },
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/auth/login",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"user@example.com\",\n  \"password\": \"Pass123!\"\n}"
        }
      }
    }
  ]
}
```

## Performance Testing

### 1. Load Testing with Apache Bench

```bash
# Signup endpoint (10 concurrent requests, 100 total)
ab -n 100 -c 10 \
  -p signup_data.json \
  -T application/json \
  http://localhost:8001/api/v1/auth/signup

# Expected: ~1s response time, <1% failure rate
```

### 2. Load Testing with JMeter

```bash
# Create test plan with:
# - 10 threads
# - 100 requests per thread
# - 5 second ramp-up time

jmeter -n -t auth_service_load_test.jmx \
  -l results.jtl \
  -j jmeter.log
```

### Performance Targets
- Average response time: < 500ms
- 95th percentile: < 1s
- Error rate: < 1%
- Throughput: > 100 requests/sec

## Security Testing

### 1. Authentication Tests

```bash
# Test 1: No token provided
curl -X GET http://localhost:8001/api/v1/admin/users
# Expected: 401 Unauthorized

# Test 2: Invalid token format
curl -X GET http://localhost:8001/api/v1/admin/users \
  -H "Authorization: InvalidToken"
# Expected: 401 Unauthorized

# Test 3: Expired token
curl -X GET http://localhost:8001/api/v1/admin/users \
  -H "Authorization: Bearer expired.token.here"
# Expected: 401 Unauthorized

# Test 4: Valid token
curl -X GET http://localhost:8001/api/v1/admin/users \
  -H "Authorization: Bearer valid.token.here"
# Expected: 200 OK or 403 Forbidden (based on permissions)
```

### 2. Authorization Tests

```bash
# Test 1: User without "users:view" permission
# Create user with 'user' role (no admin permissions)
# Try to access admin endpoint
# Expected: 403 Forbidden

# Test 2: User with "users:view" but not "users:approve"
# Create user with limited admin role
# Try to approve endpoint
# Expected: 403 Forbidden

# Test 3: Super admin can access all
# Create super admin user
# Access any endpoint
# Expected: 200 OK
```

### 3. Input Validation Tests

```bash
# Test 1: Invalid email format
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "Pass123!",
    "full_name": "Test",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'
# Expected: 400 Bad Request

# Test 2: Weak password
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "weak",
    "full_name": "Test",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'
# Expected: 400 Bad Request

# Test 3: Missing required fields
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
# Expected: 400 Bad Request
```

## Regression Testing Checklist

- [ ] All 20 endpoints respond correctly
- [ ] JWT tokens are valid and contain permissions
- [ ] Database migrations run successfully
- [ ] Audit logs are created for all operations
- [ ] Soft deletion preserves data
- [ ] Case-insensitive email handling works
- [ ] Role assignments are functional
- [ ] Permission checks are enforced
- [ ] Super admin bypass works
- [ ] CORS headers are present
- [ ] Error responses have correct format
- [ ] HTTP status codes are correct
- [ ] Password hashing is secure
- [ ] Refresh token mechanism works
- [ ] Token expiration is enforced

## Test Execution Summary

```bash
# Run all tests with coverage report
mvn clean test jacoco:report

# View results
# Unit tests: target/surefire-reports/
# Coverage report: target/site/jacoco/index.html
```

### Expected Results
```
Total Tests: 27
- Unit Tests: 20
- Integration Tests: 7
Success Rate: 100%
Code Coverage: 80%+
Time to Run: ~30 seconds
```

## CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/test.yml`:
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: smartvillage_test
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'openjdk'
      
      - name: Run tests
        run: mvn clean test
```

## Troubleshooting Tests

### Problem: Tests timeout
```bash
# Increase timeout
mvn test -DargLine="-Djunit.jupiter.execution.timeout.default=10s"
```

### Problem: Database connection errors
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check database exists
psql -U postgres -l | grep smartvillage_test
```

### Problem: Tests fail locally but pass in CI
```bash
# Run tests in isolated environment
mvn clean test -U
```

---

**Test Status**: ✅ Ready for Production  
**Coverage Target**: 80%+  
**Last Updated**: 2024-12-25
