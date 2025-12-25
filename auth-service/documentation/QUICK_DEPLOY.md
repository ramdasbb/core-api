# 🚀 Quick Deployment Reference Card

## Render.com Deployment (5 Minutes)

### Prerequisites
```bash
GitHub account
Render.com account (free signup)
```

### Step 1: Push Code
```bash
cd ~/lokseva
git add .
git commit -m "Auth Service - Phase 7-8 Complete"
git push origin main
```

### Step 2: Create PostgreSQL Database
1. Go to https://dashboard.render.com
2. Click "New +" → "PostgreSQL"
3. Name: `smartvillage-postgres`
4. Click "Create Database"
5. Copy connection details

### Step 3: Create Web Service
1. Click "New +" → "Web Service"
2. Select repository: `your-org/lokseva`
3. Branch: `main`
4. Name: `auth-service`
5. Environment: `Docker`
6. Dockerfile path: `core_api/auth-service/Dockerfile`
7. Region: `ohio` (or closest)
8. Plan: `Starter`

### Step 4: Set Environment Variables
```
SPRING_DATASOURCE_URL=jdbc:postgresql://smartvillage-postgres:5432/smartvillage
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=[copy from db created above]
JWT_SECRET=[generate: openssl rand -base64 32]
SPRING_ENVIRONMENT=production
SERVER_PORT=8001
```

### Step 5: Deploy
Click "Create Web Service" and wait for build (5-10 minutes)

### Step 6: Test
```bash
# Get URL from Render dashboard
export API_URL="https://auth-service-xxx.onrender.com"

# Test signup
curl -X POST $API_URL/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "full_name": "Test User",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'
```

---

## Local Testing (2 Minutes)

### Run All Tests
```bash
cd core_api/auth-service
mvn clean test
```

### Expected Output
```
Tests run: 27, Failures: 0, Errors: 0
BUILD SUCCESS
```

### Run Specific Test
```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=AuthControllerIntegrationTest
```

### Generate Coverage Report
```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## Docker Local Testing (3 Minutes)

```bash
# Build image
docker build -f core_api/auth-service/Dockerfile \
  -t auth-service:latest \
  core_api/auth-service/

# Run with Docker Compose
cd core_api/auth-service
docker-compose up -d

# Test endpoint
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{...}'

# Stop
docker-compose down
```

---

## Troubleshooting

### Build Fails
```bash
# Check Docker file syntax
docker build --no-cache -t auth-service:latest core_api/auth-service/

# Check dependencies
mvn dependency:tree
```

### Service Won't Connect to Database
```bash
# Verify connection string
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database

# Check credentials
psql postgresql://postgres:password@host:5432/database
```

### Tests Failing
```bash
# Run with verbose output
mvn test -X

# Run single test class
mvn test -Dtest=UserServiceTest -DfailIfNoTests=false
```

---

## Monitoring (After Deployment)

### View Logs
- Render Dashboard → auth-service → Logs

### Check Health
```bash
curl https://auth-service-xxx.onrender.com/api/v1/health
```

### View Metrics
- Render Dashboard → auth-service → Metrics
- Shows: CPU, Memory, Network, Requests

### Set Up Alerts
- Render Dashboard → Settings → Notifications
- Select email or Slack

---

## Cost Summary

| Service | Plan | Cost |
|---------|------|------|
| PostgreSQL | Starter | $7/mo |
| Web Service | Starter | $7/mo |
| **Total** | - | **$14/mo** |

Upgrade to Standard: $24 + $12 = **$36/mo** (2x resources)

---

## Key Files

| File | Purpose |
|------|---------|
| `Dockerfile` | Container image definition |
| `Dockerfile.postgres` | PostgreSQL service |
| `render.yaml` | Render infrastructure config |
| `RENDER_DEPLOYMENT.md` | Detailed deployment guide |
| `TESTING_GUIDE.md` | Complete testing procedures |
| `API_DOCUMENTATION.md` | 20 API endpoints reference |

---

## Important Commands

```bash
# Build locally
mvn clean install

# Run locally
mvn spring-boot:run

# Run tests
mvn test

# Build Docker image
docker build -f Dockerfile -t auth-service:latest .

# Run Docker container
docker run -p 8001:8001 auth-service:latest

# Push to GitHub
git add . && git commit -m "message" && git push

# Generate JWT secret
openssl rand -base64 32

# Test API
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {token}"
```

---

## Quick Links

- **Render Dashboard**: https://dashboard.render.com
- **GitHub**: https://github.com/your-org/lokseva
- **API Docs**: See `API_DOCUMENTATION.md`
- **Testing Guide**: See `TESTING_GUIDE.md`
- **Deployment Guide**: See `RENDER_DEPLOYMENT.md`

---

## Status

✅ Code Complete  
✅ Tests: 27/27 Passing  
✅ Docker Ready  
✅ Render Configured  
🚀 Ready for Production

---

**Version**: 1.0.0  
**Last Updated**: December 25, 2025  
**Estimated Deploy Time**: 10 minutes  
**Difficulty**: Easy  
