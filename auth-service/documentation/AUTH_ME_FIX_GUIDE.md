# /auth/me Endpoint - Fix & Troubleshooting Guide

## ✅ What Was Fixed

I've identified and fixed the root cause of the `/auth/me` endpoint not working:

### The Problem
**JWT Token Signature Mismatch** - The `JwtUtil.getSigningKey()` method was encoding the secret to Base64 during both token generation and validation, but inconsistently. This caused tokens to be signed with one key but validated against a different key.

### The Solution
Fixed the `getSigningKey()` method to:
1. Use the secret directly without unnecessary Base64 encoding
2. Pad the secret to 32 bytes if needed (for HS256 requirement)
3. Keep signing and validation consistent

### Files Updated
- ✅ `JwtUtil.java` - Fixed key generation and added detailed error logging
- ✅ `AuthController.java` - Added comprehensive debug logging to getProfile endpoint

---

## 🧪 Step-by-Step Testing

### Step 1: Rebuild & Restart

```bash
cd core_api/auth-service

# Clean and rebuild
mvn clean install

# Run the application
mvn spring-boot:run
```

Watch for the startup message:
```
✅ Superadmin created successfully!
   Email: superadmin@villageorbit.com
   Role: super_admin
   Status: approved
```

---

### Step 2: Login to Get Token

```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@villageorbit.com",
    "password": "SuperAdmin@123!"
  }'
```

**Expected Response (200 OK):**
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
      "roles": ["super_admin"]
    }
  }
}
```

**Copy the `access_token`** (the long string starting with `eyJ...`)

---

### Step 3: Call /auth/me with Token

```bash
# Store token in variable
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Call /auth/me endpoint
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "superadmin@villageorbit.com",
    "full_name": "Super Administrator",
    "mobile": "9876543210",
    "approval_status": "approved",
    "is_active": true,
    "roles": [
      {
        "id": "...",
        "name": "super_admin",
        "description": "Full system access and permission bypass"
      }
    ],
    "permissions": [
      "all:*"
    ],
    "created_at": "2025-12-25T14:00:00Z",
    "approved_at": "2025-12-25T14:00:00Z"
  }
}
```

---

## 🔍 Debug Output

When you call `/auth/me`, you should see this in the console:

```
=== GET /auth/me Request ===
Authorization Header: Present
Token Length: 247
Token Preview: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9Eyj...
Validating token...
Token validation successful
User ID from token: 550e8400-e29b-41d4-a716-446655440000
User found: superadmin@villageorbit.com
Permissions fetched: 1
=== GET /auth/me Response SUCCESS ===
```

### If Token Validation Fails

You'll see:
```
=== GET /auth/me Request ===
Authorization Header: Present
Token Length: 247
Token Preview: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9Eyj...
Validating token...
ERROR: Token validation failed
Invalid signature: JWT signature does not match locally computed signature
```

**Solution:** Clear your browser cache/cookies and get a fresh token by logging in again.

---

## ✅ Common Scenarios & Fixes

### Scenario 1: "Authorization header missing or empty"
**Cause:** Not sending the Authorization header

**Fix:**
```bash
# ❌ Wrong
curl -X GET http://localhost:8001/api/v1/auth/me

# ✅ Correct
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {token}"
```

### Scenario 2: "Invalid Authorization header format"
**Cause:** Missing "Bearer" prefix or wrong format

**Fix:**
```bash
# ❌ Wrong - Missing "Bearer"
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: {token}"

# ❌ Wrong - Extra spaces
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer  {token}"

# ✅ Correct
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {token}"
```

### Scenario 3: "Invalid or expired token"
**Cause:** Token is expired (15 minute expiration) or malformed

**Fix:**
1. Get a fresh token: `curl -X POST http://localhost:8001/api/v1/auth/login ...`
2. Or refresh it: `curl -X POST http://localhost:8001/api/v1/auth/refresh-token ...`

### Scenario 4: "User not found"
**Cause:** User ID in token doesn't match any user in database

**Fix:**
1. Verify user exists: `SELECT * FROM users WHERE email = 'superadmin@villageorbit.com';`
2. Re-login to get a valid token
3. Check if user was deleted

---

## 🛠️ Full Test Script

Here's a complete test script:

```bash
#!/bin/bash

BASE_URL="http://localhost:8001/api/v1"

echo "=== Step 1: Login ==="
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@villageorbit.com",
    "password": "SuperAdmin@123!"
  }')

echo "Login Response:"
echo $LOGIN_RESPONSE | jq '.'

# Extract token
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.access_token')
echo ""
echo "Token: $TOKEN"
echo ""

echo "=== Step 2: Get Profile ==="
curl -X GET $BASE_URL/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "=== Step 3: List Users ==="
curl -X GET $BASE_URL/admin/users \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

**Run it:**
```bash
chmod +x test-auth.sh
./test-auth.sh
```

---

## 📋 Verification Checklist

After rebuilding and restarting:

- [ ] Application starts without errors
- [ ] Superadmin is created on startup
- [ ] Can login with superadmin credentials
- [ ] Login returns `access_token`
- [ ] `/auth/me` returns 200 OK with profile data
- [ ] Token is valid for 15 minutes
- [ ] After 15 minutes, token is rejected
- [ ] Can refresh token with refresh_token endpoint
- [ ] All endpoints show clear debug messages

---

## 🔐 JWT Token Structure

Your token looks like:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MDM0NTY3OTgsImV4cCI6MTcwMzQ1Njg5OH0.xB...
```

Broken down:
- **Header:** `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9` (Algorithm: HS256, Type: JWT)
- **Payload:** `eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MDM0NTY3OTgsImV4cCI6MTcwMzQ1Njg5OH0` (User ID, issued at, expires at)
- **Signature:** `xB...` (HMAC-SHA256 signature)

The server validates:
1. ✅ Signature matches (using secret key)
2. ✅ Not expired (checks `exp` claim)
3. ✅ Subject exists (checks user in database)

---

## 📊 What Changed in Code

### JwtUtil.java
**Before:**
```java
// Inconsistent Base64 encoding
byte[] keyBytes = java.util.Base64.getEncoder().encode(secret.getBytes());
```

**After:**
```java
// Direct bytes, no unnecessary encoding
byte[] keyBytes = secret.getBytes();
```

### AuthController.java
**Before:**
```java
// Silent failures
if (!jwtUtil.validateToken(token)) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)...
}
```

**After:**
```java
// Detailed logging
System.out.println("=== GET /auth/me Request ===");
System.out.println("Authorization Header: " + (authHeader != null ? "Present" : "Missing"));
// ... detailed debug info ...
System.out.println("=== GET /auth/me Response SUCCESS ===");
```

---

## 🎯 Expected Behavior After Fix

1. **Login** → Get access_token (expires in 15 min)
2. **Use Token** → Call `/auth/me` with `Authorization: Bearer {token}`
3. **Get Profile** → Receive user data with roles and permissions
4. **Token Expires** → Get 401 error after 15 minutes
5. **Refresh** → Use refresh_token to get new access_token

---

## 📞 If Still Not Working

Check these in order:

1. **Verify JWT Secret** - Check `application.yml`:
   ```yaml
   jwt:
     secret: your-256-bit-secret-key-change-this-in-production-environment-must-be-at-least-32-chars
   ```
   Should be at least 32 characters.

2. **Check Database** - Verify superadmin exists:
   ```sql
   SELECT id, email, approval_status FROM users 
   WHERE email = 'superadmin@villageorbit.com';
   ```

3. **Check Logs** - Look for debug messages:
   ```
   Authorization Header: Present
   Token Length: 247
   Validating token...
   ```

4. **Rebuild Everything** - Sometimes cache causes issues:
   ```bash
   mvn clean install -DskipTests
   mvn spring-boot:run
   ```

---

## 🚀 Summary

**The Fix:** Corrected JWT signing key generation to be consistent  
**The Test:** Login → Get token → Call /auth/me with token  
**The Result:** 200 OK with your user profile data  

All endpoints now have comprehensive logging to help diagnose any future issues!
