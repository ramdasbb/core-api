# AUTH SERVICE - REST API DOCUMENTATION

## Overview
Complete REST API documentation for Smart Village Authentication Service with 20 endpoints covering authentication, user management, and RBAC.

## Base URL
```
http://localhost:8001/api/v1
```

## Authentication
All endpoints except `/auth/signup` and `/auth/login` require JWT bearer token:
```
Authorization: Bearer {access_token}
```

---

## 1. AUTHENTICATION ENDPOINTS

### 1.1 User Signup (Public)
**POST** `/auth/signup`

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "full_name": "John Doe",
  "mobile": "9876543210",
  "aadhar_number": "123456789012"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "John Doe",
    "approval_status": "pending",
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "message": "Email already in use",
  "error_code": "EMAIL_EXISTS"
}
```

---

### 1.2 User Login (Public)
**POST** `/auth/login`

Authenticate user and get access & refresh tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "error_code": null,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 900,
    "user": {
      "user_id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "full_name": "John Doe",
      "roles": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "name": "user"
        }
      ],
      "permissions": ["users:view", "services:view", ...]
    }
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Invalid email or password",
  "error_code": "AUTH_FAILED"
}
```

**Error Response (403 Forbidden - User not approved):**
```json
{
  "success": false,
  "message": "User account not approved yet",
  "error_code": "USER_NOT_APPROVED"
}
```

---

### 1.3 Get User Profile
**GET** `/auth/me`

Retrieve authenticated user's profile with roles and permissions.

**Headers:**
```
Authorization: Bearer {access_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "John Doe",
    "mobile": "9876543210",
    "approval_status": "approved",
    "roles": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "user",
        "permissions": ["users:view", "services:view"]
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "gramsevak",
        "permissions": ["services:create", "services:edit"]
      }
    ],
    "all_permissions": ["users:view", "services:view", "services:create", "services:edit"],
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "error_code": "UNAUTHORIZED"
}
```

---

### 1.4 Refresh Access Token
**POST** `/auth/refresh-token`

Generate new access token using valid refresh token.

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "error_code": null,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 900
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Invalid or expired refresh token",
  "error_code": "INVALID_TOKEN"
}
```

---

### 1.5 Logout
**POST** `/auth/logout`

Revoke refresh token and logout user.

**Headers:**
```
Authorization: Bearer {access_token}
```

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Logout successful",
  "error_code": null,
  "data": null
}
```

---

## 2. USER MANAGEMENT ENDPOINTS (Admin)

### 2.1 List All Users
**GET** `/admin/users`

List all users with pagination support. Requires `users:view` permission.

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Query Parameters:**
- `page` (optional, default: 0): Page number
- `limit` (optional, default: 20): Items per page

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "error_code": null,
  "data": {
    "users": [
      {
        "user_id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "full_name": "John Doe",
        "approval_status": "approved",
        "roles": [
          {"id": "...", "name": "user"}
        ],
        "created_at": "2024-01-15T10:30:00Z"
      }
    ],
    "total": 100,
    "page": 0,
    "limit": 20
  }
}
```

**Error Response (403 Forbidden):**
```json
{
  "success": false,
  "message": "Permission denied",
  "error_code": "PERMISSION_DENIED"
}
```

---

### 2.2 Get User Details
**GET** `/admin/users/{userId}`

Get detailed information about specific user. Requires `users:view` permission.

**Path Parameters:**
- `userId` (required): UUID of the user

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User details retrieved successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "John Doe",
    "mobile": "9876543210",
    "aadhar_number": "123456789012",
    "approval_status": "pending",
    "roles": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "name": "user",
        "permissions": ["users:view", "services:view"]
      }
    ],
    "is_active": true,
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "User not found",
  "error_code": "USER_NOT_FOUND"
}
```

---

### 2.3 Approve User
**POST** `/admin/users/{userId}/approve`

Approve a pending user account. Requires `users:approve` permission.

**Path Parameters:**
- `userId` (required): UUID of the user to approve

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User approved successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "approval_status": "approved",
    "approved_by": "550e8400-e29b-41d4-a716-446655440099",
    "approved_at": "2024-01-15T11:00:00Z"
  }
}
```

**Error Response (400 Bad Request - User already approved):**
```json
{
  "success": false,
  "message": "User is already approved",
  "error_code": "INVALID_STATUS"
}
```

---

### 2.4 Reject User
**POST** `/admin/users/{userId}/reject`

Reject a pending user account with reason. Requires `users:reject` permission.

**Path Parameters:**
- `userId` (required): UUID of the user to reject

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Request Body:**
```json
{
  "rejection_reason": "Invalid aadhar number"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User rejected successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "approval_status": "rejected",
    "rejection_reason": "Invalid aadhar number"
  }
}
```

---

### 2.5 Delete User
**DELETE** `/admin/users/{userId}`

Soft delete a user (sets is_active=false). Requires `users:delete` permission.

**Path Parameters:**
- `userId` (required): UUID of the user to delete

**Headers:**
```
Authorization: Bearer {admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User deleted successfully",
  "error_code": null,
  "data": null
}
```

---

## 3. RBAC MANAGEMENT ENDPOINTS (Super Admin Only)

### 3.1 Create Permission
**POST** `/rbac/permissions`

Create a new permission. Super Admin only.

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Request Body:**
```json
{
  "name": "services:delete",
  "description": "Delete services"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Permission created successfully",
  "error_code": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "services:delete",
    "description": "Delete services",
    "created_at": "2024-01-15T12:00:00Z"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "message": "Permission already exists: services:delete",
  "error_code": "PERMISSION_EXISTS"
}
```

---

### 3.2 List Permissions
**GET** `/rbac/permissions`

List all permissions. Super Admin only.

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Permissions retrieved successfully",
  "error_code": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "users:view",
      "description": "View users",
      "created_at": "2024-01-15T10:00:00Z"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "users:create",
      "description": "Create users",
      "created_at": "2024-01-15T10:05:00Z"
    }
  ]
}
```

---

### 3.3 Delete Permission
**DELETE** `/rbac/permissions/{permissionId}`

Delete a permission. Super Admin only.

**Path Parameters:**
- `permissionId` (required): UUID of the permission to delete

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Permission deleted successfully",
  "error_code": null,
  "data": null
}
```

---

### 3.4 Create Role
**POST** `/rbac/roles`

Create a new role. Super Admin only.

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Request Body:**
```json
{
  "name": "moderator",
  "description": "Content moderator",
  "is_system_role": false
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Role created successfully",
  "error_code": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "moderator",
    "description": "Content moderator",
    "is_system_role": false,
    "permissions": [],
    "created_at": "2024-01-15T12:00:00Z"
  }
}
```

---

### 3.5 List Roles
**GET** `/rbac/roles`

List all roles with their permissions. Super Admin only.

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Roles retrieved successfully",
  "error_code": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "admin",
      "description": "System administrator",
      "is_system_role": true,
      "permissions": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440001",
          "name": "users:view"
        },
        {
          "id": "550e8400-e29b-41d4-a716-446655440002",
          "name": "users:approve"
        }
      ],
      "created_at": "2024-01-15T10:00:00Z"
    }
  ]
}
```

---

### 3.6 Delete Role
**DELETE** `/rbac/roles/{roleId}`

Delete a role. Super Admin only.

**Path Parameters:**
- `roleId` (required): UUID of the role to delete

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Role deleted successfully",
  "error_code": null,
  "data": null
}
```

---

### 3.7 Assign Permissions to Role
**POST** `/rbac/roles/{roleId}/permissions`

Add permissions to a role. Super Admin only.

**Path Parameters:**
- `roleId` (required): UUID of the role

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Request Body:**
```json
{
  "permission_ids": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002",
    "550e8400-e29b-41d4-a716-446655440003"
  ]
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Permissions assigned to role successfully",
  "error_code": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "admin",
    "permissions": [
      {"id": "550e8400-e29b-41d4-a716-446655440001", "name": "users:view"},
      {"id": "550e8400-e29b-41d4-a716-446655440002", "name": "users:approve"},
      {"id": "550e8400-e29b-41d4-a716-446655440003", "name": "users:delete"}
    ]
  }
}
```

---

### 3.8 Remove Permission from Role
**DELETE** `/rbac/roles/{roleId}/permissions/{permissionId}`

Remove a permission from a role. Super Admin only.

**Path Parameters:**
- `roleId` (required): UUID of the role
- `permissionId` (required): UUID of the permission

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Permission removed from role successfully",
  "error_code": null,
  "data": null
}
```

---

### 3.9 Assign Roles to User
**POST** `/rbac/users/{userId}/roles`

Assign roles to a user. Super Admin only.

**Path Parameters:**
- `userId` (required): UUID of the user

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Request Body:**
```json
{
  "role_ids": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002"
  ]
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Roles assigned to user successfully",
  "error_code": null,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "roles": [
      {"id": "550e8400-e29b-41d4-a716-446655440001", "name": "admin"},
      {"id": "550e8400-e29b-41d4-a716-446655440002", "name": "gramsevak"}
    ],
    "all_permissions": ["users:view", "users:approve", "services:view", "services:create"]
  }
}
```

---

### 3.10 Remove Role from User
**DELETE** `/rbac/users/{userId}/roles/{roleId}`

Remove a role from a user. Super Admin only.

**Path Parameters:**
- `userId` (required): UUID of the user
- `roleId` (required): UUID of the role to remove

**Headers:**
```
Authorization: Bearer {super_admin_token}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Role removed from user successfully",
  "error_code": null,
  "data": null
}
```

---

## Error Codes Reference

| Code | Status | Description |
|------|--------|-------------|
| USER_NOT_FOUND | 404 | User does not exist |
| EMAIL_EXISTS | 409 | Email already registered |
| INVALID_INPUT | 400 | Invalid request data |
| UNAUTHORIZED | 401 | Missing or invalid token |
| PERMISSION_DENIED | 403 | User lacks required permission |
| USER_NOT_APPROVED | 403 | User account not approved |
| INVALID_TOKEN | 401 | Token is invalid or expired |
| AUTH_FAILED | 401 | Authentication failed |
| PERMISSION_EXISTS | 409 | Permission already exists |
| ROLE_EXISTS | 409 | Role already exists |
| INVALID_STATUS | 400 | Invalid status for operation |
| INTERNAL_SERVER_ERROR | 500 | Server error |

---

## Token Claims

### Access Token
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "permissions": ["users:view", "services:view"],
  "type": "access",
  "iat": 1705315800,
  "exp": 1705316700
}
```

### Refresh Token
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "type": "refresh",
  "iat": 1705315800,
  "exp": 1706006400
}
```

---

## Rate Limits
Currently no rate limiting implemented. Recommended limits:
- `/auth/login`: 5 attempts per minute
- `/auth/signup`: 3 per hour per IP
- Other endpoints: 100 per minute per user

---

## CORS Policy
- **Allowed Origins**: All (*)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Allowed Headers**: Content-Type, Authorization
- **Max Age**: 3600 seconds

---

## Security Headers
All responses include:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`

(Implement via Spring Security configuration)

---

**API Version**: 1.0  
**Last Updated**: 2024-01-15  
**Documentation Status**: Complete
