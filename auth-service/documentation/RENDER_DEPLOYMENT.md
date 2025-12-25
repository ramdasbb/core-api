# Render.com Deployment Guide

Complete step-by-step guide to deploy Auth Service to Render.com.

## Prerequisites

1. **Render.com Account** - Sign up at https://render.com
2. **GitHub Repository** - Code must be in a GitHub repository
3. **Git Credentials** - GitHub personal access token or SSH key
4. **Domain (Optional)** - For custom domain

## Step 1: Prepare GitHub Repository

### 1.1 Create GitHub Repository Structure
```bash
# Repository structure required by Render
your-org/lokseva/
├── core_api/auth-service/
│   ├── Dockerfile
│   ├── Dockerfile.postgres
│   ├── render.yaml
│   ├── pom.xml
│   ├── src/
│   └── ...
└── README.md
```

### 1.2 Push Code to GitHub
```bash
cd ~/lokseva
git init
git add .
git commit -m "Initial commit: Auth Service with Render deployment config"
git remote add origin https://github.com/your-org/lokseva.git
git push -u origin main
```

## Step 2: Create Render Services

### 2.1 Login to Render Dashboard
- Go to https://dashboard.render.com
- Sign up or login with GitHub account
- Authorize Render to access your GitHub account

### 2.2 Create PostgreSQL Database Service

**Option A: Using Render Dashboard UI**

1. Click "New +" → "PostgreSQL"
2. Fill in details:
   - **Name**: `smartvillage-postgres`
   - **Database**: `smartvillage`
   - **User**: `postgres`
   - **Region**: Select closest region (e.g., Ohio)
   - **PostgreSQL Version**: `15`
   - **Plan**: Starter ($7/month)

3. Click "Create Database"
4. **Important**: Copy and save:
   - Database URL
   - Username
   - Password
   - Host

**Output will be:**
```
Database: smartvillage
Username: postgres
Password: [generated-password]
Host: smartvillage-postgres.render.com
Port: 5432
```

### 2.3 Create Auth Service Web Service

1. Click "New +" → "Web Service"
2. Select GitHub repository:
   - Choose `your-org/lokseva`
   - Branch: `main`
3. Fill in details:
   - **Name**: `auth-service`
   - **Environment**: `Docker`
   - **Region**: Same as database (Ohio)
   - **Dockerfile path**: `core_api/auth-service/Dockerfile`
   - **Plan**: Starter ($7/month)

4. Set Environment Variables:
   - Click "Environment"
   - Add the following variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://smartvillage-postgres:5432/smartvillage
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=[database-password-from-step-2]
JWT_SECRET=[generate-secure-32-byte-key]
SPRING_ENVIRONMENT=production
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
SERVER_PORT=8001
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics
```

**Generate JWT Secret:**
```bash
# Using openssl
openssl rand -base64 32

# Or using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

5. Click "Create Web Service"
6. Wait for build and deployment (5-10 minutes)

## Step 3: Build Configuration

### 3.1 Build Logs
- Render will automatically detect Docker and build the image
- Watch the build progress in the "Logs" tab
- Expected output:
  ```
  Building image...
  Step 1/X : FROM maven:3.8.7-openjdk-21 AS builder
  ...
  Building complete
  Pushing to registry...
  Build and push complete
  Starting service...
  ```

### 3.2 Deployment Health Check
- Render will perform health checks
- Expected response from `/api/v1/health`:
  ```json
  {
    "success": true,
    "message": "Health check passed"
  }
  ```

## Step 4: Database Initialization

### 4.1 Manual Migration (If Needed)

If Flyway migrations don't run automatically:

1. Get the database connection string from Render dashboard
2. Connect using psql:
   ```bash
   psql postgresql://postgres:[password]@smartvillage-postgres.render.com:5432/smartvillage
   ```

3. Apply migrations manually:
   ```sql
   -- Execute contents of V1__create_auth_schema.sql
   ```

### 4.2 Verify Database Tables

```bash
# Connect to database
psql postgresql://postgres:[password]@smartvillage-postgres.render.com:5432/smartvillage

# List tables
\dt

# Should show:
# - audit_logs
# - permissions
# - refresh_tokens
# - role_permissions
# - roles
# - user_roles
# - users
```

## Step 5: Test the Service

### 5.1 Get Service URL

From Render Dashboard:
- Navigate to "auth-service"
- Copy the "Service URL" (e.g., `https://auth-service-xxx.onrender.com`)

### 5.2 Test API Endpoints

```bash
# Test signup
curl -X POST https://auth-service-xxx.onrender.com/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "full_name": "Test User",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'

# Expected: 201 Created

# Test login
curl -X POST https://auth-service-xxx.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'

# Expected: 403 Forbidden (user not approved yet)
```

### 5.3 Check Logs

In Render Dashboard:
1. Click "Logs" tab
2. Should see:
   - Build logs
   - Application startup logs
   - Request logs

## Step 6: Custom Domain (Optional)

### 6.1 Add Custom Domain

1. In Auth Service settings:
   - Click "Settings"
   - Scroll to "Custom Domains"
   - Click "Add Custom Domain"
   - Enter your domain (e.g., `api.smartvillage.com`)

2. Update DNS records:
   ```
   Type: CNAME
   Name: api.smartvillage.com
   Value: auth-service-xxx.onrender.com
   ```

3. Verify DNS propagation (can take 24 hours)

## Step 7: Monitoring & Maintenance

### 7.1 View Metrics

In Render Dashboard:
- **Metrics** tab shows:
  - CPU usage
  - Memory usage
  - Network traffic
  - Request count

### 7.2 View Logs

Real-time logs for:
- Build output
- Application logs
- Error tracking

### 7.3 Auto-Deploy on Push

- Enable in "Settings" → "Auto-Deploy"
- Select "Yes" for automatic redeployment on git push
- No need to manually redeploy!

## Step 8: Scaling

### 8.1 Increase Resources (If Needed)

If service is slow:
1. Go to "Settings"
2. Change "Plan" to Standard ($12/month)
3. Render will automatically upgrade

### 8.2 Multiple Instances

For high availability:
1. Settings → "Max Instances"
2. Set to 2-3 instances
3. Render will load balance automatically

## Troubleshooting

### Problem: Build Fails
**Solution:**
```bash
# Check Dockerfile syntax
docker build -f core_api/auth-service/Dockerfile .

# Check for missing files
ls -la core_api/auth-service/src/
ls -la core_api/auth-service/pom.xml
```

### Problem: Service Won't Start
**Solution:**
```bash
# Check application logs in Render dashboard
# Look for:
# - Java version mismatch
# - Missing environment variables
# - Database connection errors

# Verify environment variables are set correctly
```

### Problem: Database Connection Error
**Solution:**
1. Verify connection string format:
   ```
   jdbc:postgresql://host:5432/database
   ```
2. Confirm credentials match
3. Check if database service is running:
   - Render Dashboard → Database → Status

### Problem: Migrations Not Applied
**Solution:**
1. Check Flyway is enabled in `application.yml`
2. Verify migration files exist in `src/main/resources/db/migration/`
3. Check application logs for Flyway errors

## Production Checklist

- [ ] Database is running and healthy
- [ ] Auth service is deployed and healthy
- [ ] Health check endpoint responds with 200
- [ ] Can signup new user
- [ ] Can login to existing user
- [ ] JWT tokens are generated correctly
- [ ] Permissions are enforced
- [ ] Audit logs are created
- [ ] Logs are accessible in Render dashboard
- [ ] Custom domain is configured (optional)
- [ ] Monitoring and alerts are set up
- [ ] Backup strategy is in place

## Cost Estimate

| Service | Plan | Cost |
|---------|------|------|
| PostgreSQL Database | Starter | $7/month |
| Auth Service | Starter | $7/month |
| **Total** | - | **$14/month** |

Upgrade to Standard ($12 + $24 = $36/month) for 2x resources.

## Useful Render Commands

### View Deployment Status
```bash
# Using Render CLI (if installed)
render status auth-service
```

### View Recent Deployments
```bash
render deployments auth-service
```

### Trigger Manual Deploy
In Render Dashboard:
- Services → auth-service
- Click "Manual Deploy" button

## Additional Resources

- [Render.com Docs](https://render.com/docs)
- [Render Docker Guide](https://render.com/docs/docker)
- [Render PostgreSQL Guide](https://render.com/docs/databases)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)

## Support

For issues:
1. Check Render dashboard logs
2. Review application error logs
3. Contact Render support: support@render.com
4. Check Render status page: status.render.com

## Post-Deployment

### 1. Setup Monitoring
- Configure uptime monitoring
- Set up error alerts
- Enable access logs

### 2. Backup Strategy
- Enable automatic database backups
- Store backups securely
- Test backup restoration

### 3. Security
- Enable HTTPS (automatic via Render)
- Configure CORS for frontend domains
- Rotate JWT secret periodically
- Monitor access logs for anomalies

### 4. Performance Optimization
- Monitor response times
- Optimize database queries
- Enable caching where possible
- Use CDN for static assets (if needed)

---

**Estimated Time**: 15-30 minutes  
**Difficulty**: Intermediate  
**Cost**: $14/month (starter plan)

For detailed API documentation, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
