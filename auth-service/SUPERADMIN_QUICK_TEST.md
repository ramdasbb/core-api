# Superadmin Auto-Setup - Quick Test Guide

## What Was Created

A new `DataInitializer` component automatically creates a superadmin user when Spring Boot starts.

**File:** `src/main/java/com/smartvillage/authservice/config/DataInitializer.java`

---

## Quick Start

### 1. Build the Project
```bash
cd core_api/auth-service
mvn clean install
```

### 2. Run the Application
```bash
mvn spring-boot:run
```

### 3. Watch for Startup Message

Look for this output during startup:

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

---

## Test Steps

### Test 1: Login as Superadmin

```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@villageorbit.com",
    "password": "SuperAdmin@123!"
  }'
```

**Expected:** Status 200, access_token in response

### Test 2: Get Your Profile

```bash
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {access_token_from_test_1}"
```

**Expected:** Your superadmin profile with super_admin role

### Test 3: Register a Test User

```bash
curl -X POST http://localhost:8001/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPass@123!",
    "full_name": "Test User",
    "mobile": "9876543210",
    "aadhar_number": "123456789012"
  }'
```

**Expected:** Status 201, user created with "pending" status

### Test 4: List Users (as Superadmin)

```bash
curl -X GET http://localhost:8001/api/v1/admin/users \
  -H "Authorization: Bearer {access_token_from_test_1}"
```

**Expected:** List of users including the test user in "pending" status

### Test 5: Approve the Test User

```bash
curl -X POST http://localhost:8001/api/v1/admin/users/{testUserId}/approve \
  -H "Authorization: Bearer {access_token_from_test_1}"
```

**Expected:** Status 200, user now has "approved" status

### Test 6: Test User Can Now Login

```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPass@123!"
  }'
```

**Expected:** Status 200, access_token for the test user

---

## Verification in Database

### Check Superadmin Exists
```sql
psql -U postgres -d smartvillage

SELECT id, email, approval_status FROM users 
WHERE email = 'superadmin@villageorbit.com';
```

**Expected:** One row with approval_status = 'approved'

### Check Superadmin Role
```sql
SELECT u.email, r.name FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'superadmin@villageorbit.com';
```

**Expected:** One row with role = 'super_admin'

---

## Key Features

✅ **Automatic** - Creates on every startup  
✅ **Idempotent** - Won't duplicate if already exists  
✅ **Pre-Approved** - Can login immediately  
✅ **Full Access** - Has super_admin role  
✅ **Safe** - Won't interfere with existing data  

---

## Credentials

- **Email:** `superadmin@villageorbit.com`
- **Password:** `SuperAdmin@123!`
- **Role:** `super_admin`
- **Status:** `approved` (ready to use immediately)

---

## Implementation Flow

```
Application Startup
         ↓
Spring Boots Up
         ↓
DataInitializer Bean Created
         ↓
ApplicationRunner.run() Executes
         ↓
Check: Superadmin exists? ─→ YES → Skip & Continue
         ├→ NO → Create superadmin
                    ├ Create user entity
                    ├ Hash password (Bcrypt)
                    ├ Set approval status: "approved"
                    ├ Assign "super_admin" role
                    ├ Save to database
                    └ Print success message
         ↓
Application Ready
```

---

## Troubleshooting

### Issue: "super_admin role not found"
**Solution:** Ensure database migrations ran. Check:
```bash
# Restart with fresh database
dropdb smartvillage
createdb smartvillage
mvn spring-boot:run
```

### Issue: Superadmin login fails with wrong password
**Solution:** Try the exact default password: `SuperAdmin@123!`

### Issue: Message says "already exists" on every restart
**Cause:** This is NORMAL! Means the idempotent check is working.

### Issue: Want to change default credentials
**Solution:** Edit [DataInitializer.java](src/main/java/com/smartvillage/authservice/config/DataInitializer.java) lines 32 and 54

---

## Next Steps After Setup

1. ✅ Superadmin created and ready
2. ✅ Register test users via signup
3. ✅ Approve users via superadmin
4. ✅ Users can now login
5. ✅ Create additional admin accounts as needed

---

## Production Checklist

Before deploying to production:

- [ ] Change default superadmin password
- [ ] Consider using environment variables for credentials
- [ ] Review SecurityConfig.java password encoding
- [ ] Test login/approval workflow
- [ ] Verify audit logs are created
- [ ] Check database backups
- [ ] Update documentation with actual credentials

---

For detailed information, see [SUPERADMIN_SETUP.md](SUPERADMIN_SETUP.md)
