# Superadmin Auto-Initialization Guide

## Overview

The auth service now automatically creates a superadmin user on Spring Boot startup. This ensures that there's always an admin account available to approve users without manual database intervention.

---

## How It Works

When the application starts:

1. **Checks** if a superadmin user with email `superadmin@villageorbit.com` already exists
2. **If not found**: Creates a new superadmin user with:
   - Email: `superadmin@villageorbit.com`
   - Password: `SuperAdmin@123!` (hashed with Bcrypt)
   - Full Name: `Super Administrator`
   - Mobile: `9876543210`
   - Aadhar: `123456789012`
   - Role: `super_admin`
   - Status: `approved` (can login immediately)
3. **If found**: Skips initialization and continues startup

---

## Implementation Details

### Component Location
**File:** `src/main/java/com/smartvillage/authservice/config/DataInitializer.java`

### What It Does
- `@Configuration` class with `@Bean` method
- Implements `ApplicationRunner` for startup execution
- Uses dependency injection for repositories and password encoder
- Includes comprehensive error handling and logging
- Idempotent design (safe to run multiple times)

### Key Features
✅ **Automatic Initialization** - Runs on every startup  
✅ **Idempotent** - Won't duplicate if already exists  
✅ **Pre-Approved** - User is immediately ready to login  
✅ **Full Permissions** - Assigned super_admin role with all permissions  
✅ **Safe** - Proper error handling and logging  
✅ **Password Hashing** - Uses configured PasswordEncoder (Bcrypt 12 rounds)

---

## Testing the Setup

### Step 1: Start the Application

```bash
cd core_api/auth-service
mvn clean install
mvn spring-boot:run
```

### Step 2: Watch for Initialization Message

During startup, you should see:

```
🔧 Initializing superadmin user...
✅ Superadmin created successfully!
   Email: superadmin@villageorbit.com
   Role: super_admin
   Status: approved

⚠️  IMPORTANT:
   Change the default password immediately in production!
   Default credentials are for development only.
```

### Step 3: Login as Superadmin

```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@villageorbit.com",
    "password": "SuperAdmin@123!"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 900,
    "user": {
      "user_id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "superadmin@villageorbit.com",
      "full_name": "Super Administrator",
      "roles": [
        {
          "name": "super_admin"
        }
      ]
    }
  }
}
```

### Step 4: Use Token to Approve Users

With the access token, approve pending users:

```bash
# List pending users
curl -X GET http://localhost:8001/api/v1/admin/users \
  -H "Authorization: Bearer {access_token}"

# Approve a user (replace {userId} with actual UUID)
curl -X POST http://localhost:8001/api/v1/admin/users/{userId}/approve \
  -H "Authorization: Bearer {access_token}"
```

---

## Customizing Credentials

To change the default superadmin credentials, edit [DataInitializer.java](src/main/java/com/smartvillage/authservice/config/DataInitializer.java):

```java
String superadminEmail = "your-email@example.com".toLowerCase().trim();

// And update the password encoding line:
String encodedPassword = passwordEncoder.encode("YourPassword@123!");
```

---

## Database State After Initialization

When the superadmin is created, the database will have:

### Users Table
```sql
SELECT * FROM users WHERE email = 'superadmin@villageorbit.com';
```

| Field | Value |
|-------|-------|
| id | UUID (auto-generated) |
| email | superadmin@villageorbit.com |
| password_hash | Bcrypt hashed |
| full_name | Super Administrator |
| approval_status | **approved** |
| is_active | true |
| created_at | Startup timestamp |

### User Roles Table
```sql
SELECT u.email, r.name FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'superadmin@villageorbit.com';
```

| email | role |
|-------|------|
| superadmin@villageorbit.com | super_admin |

---

## Verification Queries

### Check if superadmin exists
```sql
SELECT id, email, approval_status, is_active FROM users 
WHERE email = 'superadmin@villageorbit.com';
```

### Check superadmin's role
```sql
SELECT r.name FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'superadmin@villageorbit.com';
```

### Check all admin users
```sql
SELECT u.email, r.name, u.approval_status FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.name IN ('super_admin', 'admin', 'sub_admin');
```

---

## Production Considerations

⚠️ **IMPORTANT SECURITY NOTES:**

1. **Change Default Password** - Always change the default password in production
2. **Environment Variables** - Consider using environment variables for credentials:
   ```java
   String superadminEmail = System.getenv("SUPERADMIN_EMAIL", "superadmin@villageorbit.com");
   String superadminPassword = System.getenv("SUPERADMIN_PASSWORD", "SuperAdmin@123!");
   ```

3. **Disable Auto-Initialization** - For production with pre-existing admins:
   ```java
   // Add condition to check environment
   if (!"production".equals(System.getenv("ENVIRONMENT"))) {
       // Only initialize in dev/test
   }
   ```

4. **Log Security** - Ensure password logs are not exposed in production logs

---

## Troubleshooting

### "super_admin role not found" Error

**Cause:** Database migrations didn't run before initialization

**Solution:**
```bash
# Ensure Flyway migrations run before beans are initialized
# Check application.yml has flyway enabled
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### Superadmin Already Exists Message

**Cause:** User exists from previous startup

**Info:** This is expected and normal behavior. The message indicates:
- Initialization was skipped (idempotent design)
- Existing superadmin is still valid
- No duplicate user was created

### Password Encoding Issues

If you get password hashing errors:
```bash
# Verify PasswordEncoder bean is defined in SecurityConfig
# Check password encoder is Bcrypt with 12 rounds
```

---

## Next Steps

1. ✅ Superadmin is created automatically on startup
2. ✅ Login with superadmin credentials
3. ✅ Approve pending users through API
4. ✅ Create additional admin accounts if needed
5. ✅ Configure additional roles and permissions

---

## Files Modified

- ✅ Created: `src/main/java/com/smartvillage/authservice/config/DataInitializer.java`

## Files Unchanged (No Changes Required)

- `application.yml` - Uses default Flyway settings
- `SecurityConfig.java` - PasswordEncoder already configured
- Repositories - findByEmail() already exists

---

## Summary

The superadmin initialization is now **fully automatic**. Every time the application starts, it ensures a superadmin account exists and is ready to approve users. This eliminates manual setup and bootstrapping steps.

**Default Credentials:**
- Email: `superadmin@villageorbit.com`
- Password: `SuperAdmin@123!`
- Role: `super_admin` (Full system access)
- Status: `approved` (Can login immediately)

Safe to restart the application anytime - the initialization is idempotent and won't create duplicates.
