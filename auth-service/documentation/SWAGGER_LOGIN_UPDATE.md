# Swagger Login API - Updated Documentation

## ✅ What Was Updated

Updated the login API endpoint with proper Swagger documentation that clearly shows it only accepts **email** and **password**.

### Files Modified:
1. ✅ `AuthController.java` - Added comprehensive Swagger annotations
2. ✅ `LoginRequest.java` - Created new DTO with Swagger schema annotations

---

## 📋 Login API Swagger Documentation

### Endpoint Details

**URL:** `POST /api/v1/auth/login`

**Tag:** Authentication

**Description:** Authenticate a user with email and password. Returns access token and refresh token on successful authentication.

---

## 📥 Request Body

The login endpoint **only accepts**:

```json
{
  "email": "superadmin@villageorbit.com",
  "password": "SuperAdmin@123!"
}
```

### Required Fields:
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `email` | String | User email address | `superadmin@villageorbit.com` |
| `password` | String | User password | `SuperAdmin@123!` |

**Note:** Only these two fields should be sent. Any other fields like `full_name`, `mobile`, `aadhar_number` will be ignored.

---

## 📤 Response Examples

### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Login successful",
  "error_code": null,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MDM0NTY3OTgsImV4cCI6MTcwMzQ1Njg5OH0.xB...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MDM0NTY3OTgsImV4cCI6MTcwNDM0NDU5OH0.yC...",
    "expires_in": 900,
    "user": {
      "user_id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "superadmin@villageorbit.com",
      "full_name": "Super Administrator",
      "roles": ["super_admin"],
      "permissions": ["all:*"]
    }
  }
}
```

### Error Response (401 Unauthorized)

**Invalid credentials:**
```json
{
  "success": false,
  "message": "Invalid email or password",
  "error_code": "INVALID_CREDENTIALS",
  "data": null
}
```

**User not approved:**
```json
{
  "success": false,
  "message": "User account not approved yet",
  "error_code": "USER_NOT_APPROVED",
  "data": null
}
```

### Error Response (400 Bad Request)

**Missing email:**
```json
{
  "success": false,
  "message": "Email is required",
  "error_code": "INVALID_INPUT",
  "data": null
}
```

**Missing password:**
```json
{
  "success": false,
  "message": "Password is required",
  "error_code": "INVALID_INPUT",
  "data": null
}
```

---

## 🧪 How to Test in Swagger UI

1. **Navigate to Swagger:** `http://localhost:8001/swagger-ui.html`
2. **Find:** Authentication → User Login (POST /api/v1/auth/login)
3. **Click:** "Try it out" button
4. **Enter Request Body:**
   ```json
   {
     "email": "superadmin@villageorbit.com",
     "password": "SuperAdmin@123!"
   }
   ```
5. **Click:** "Execute" button
6. **Receive:** Access token in response

---

## 🧪 How to Test with cURL

```bash
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "superadmin@villageorbit.com",
    "password": "SuperAdmin@123!"
  }'
```

---

## 📊 Response Fields Explained

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Request success status |
| `message` | String | Human-readable message |
| `error_code` | String | Error code (null on success) |
| `data` | Object | Response data containing tokens |
| `data.access_token` | String | JWT access token (valid for 15 minutes) |
| `data.refresh_token` | String | Refresh token (valid for 7 days) |
| `data.expires_in` | Integer | Access token expiration in seconds (900 = 15 min) |
| `data.user` | Object | User information |
| `data.user.user_id` | String | User UUID |
| `data.user.email` | String | User email |
| `data.user.full_name` | String | Full name |
| `data.user.roles` | Array | List of role names |
| `data.user.permissions` | Array | List of permission strings |

---

## 🔑 Using the Access Token

After login, use the `access_token` in subsequent requests:

```bash
curl -X GET http://localhost:8001/api/v1/auth/me \
  -H "Authorization: Bearer {access_token}"
```

**Important:** Include `Bearer ` prefix (with space) before the token.

---

## ⏰ Token Expiration

- **Access Token:** Expires in 900 seconds (15 minutes)
- **Refresh Token:** Expires in 604800 seconds (7 days)

After access token expires, use the refresh token to get a new one:

```bash
curl -X POST http://localhost:8001/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "{refresh_token_from_login}"
  }'
```

---

## 📝 Summary of Changes

### Before
- No clear Swagger documentation for login endpoint
- Mixed DTOs with extra fields in request
- No clear examples in Swagger UI

### After
✅ Proper `LoginRequest` DTO with only `email` and `password`  
✅ Comprehensive Swagger annotations with examples  
✅ Clear error codes and status codes  
✅ Example JSON in Swagger UI  
✅ Controller tagged as "Authentication"  
✅ Detailed response schema  

---

## 🎯 Next Steps

1. **Rebuild** the application:
   ```bash
   cd core_api/auth-service
   mvn clean install
   ```

2. **Restart** the application:
   ```bash
   mvn spring-boot:run
   ```

3. **Open Swagger UI:**
   ```
   http://localhost:8001/swagger-ui.html
   ```

4. **Test the login endpoint** with the documented examples

---

## ✨ What You'll See in Swagger

In Swagger UI, the login endpoint now shows:
- ✅ Clear description
- ✅ Example request body with only email and password
- ✅ Success response with 200 status
- ✅ Error responses with 400, 401, 500 statuses
- ✅ Field descriptions and examples
- ✅ Type information for each field

---

All documentation is now **accurate** and **in sync** with the actual API implementation! 🚀
