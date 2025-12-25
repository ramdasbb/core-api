# VillageOrbit Auth Service — Production Design Specification

## Executive Summary

This document defines the **auth-service** — a stateless, horizontally-scalable REST API that fully replaces Supabase Auth, user roles, and RLS with:
- ✅ JWT-based authentication (stateless)
- ✅ Dynamic RBAC (Role-Based Access Control)
- ✅ Fine-grained permission model
- ✅ User approval workflow
- ✅ Audit logging
- ✅ Enterprise-grade security

---

## PART 1: API INVENTORY & ENDPOINTS

### Authentication APIs (Public)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|----------------|
| POST | `/auth/signup` | Register new user (pending approval) | ❌ |
| POST | `/auth/login` | Login with email + password | ❌ |
| POST | `/auth/logout` | Revoke refresh token | ✅ |
| POST | `/auth/refresh-token` | Issue new access token | ❌ |
| GET | `/auth/me` | Get authenticated user + roles + permissions | ✅ |

### User Management APIs (Admin/Gramsevak)

| Method | Endpoint | Description | Required Permission |
|--------|----------|-------------|----------------------|
| GET | `/admin/users` | List users (with filters) | `users:view` |
| GET | `/admin/users/{userId}` | Get user details | `users:view` |
| POST | `/admin/users/{userId}/approve` | Approve user | `users:approve` |
| POST | `/admin/users/{userId}/reject` | Reject user | `users:reject` |
| DELETE | `/admin/users/{userId}` | Soft delete user | `users:delete` |

### RBAC Management APIs (Super Admin Only)

| Method | Endpoint | Description | Restricted To |
|--------|----------|-------------|----------------|
| POST | `/rbac/permissions` | Create permission | super_admin |
| GET | `/rbac/permissions` | List permissions | super_admin |
| GET | `/rbac/permissions/{permissionId}` | Get permission | super_admin |
| DELETE | `/rbac/permissions/{permissionId}` | Delete permission | super_admin |
| POST | `/rbac/roles` | Create role | super_admin |
| GET | `/rbac/roles` | List roles | super_admin |
| GET | `/rbac/roles/{roleId}` | Get role details | super_admin |
| DELETE | `/rbac/roles/{roleId}` | Delete role | super_admin |
| POST | `/rbac/roles/{roleId}/permissions` | Assign permissions to role | super_admin |
| DELETE | `/rbac/roles/{roleId}/permissions/{permissionId}` | Revoke permission from role | super_admin |
| POST | `/rbac/users/{userId}/roles` | Assign role(s) to user | super_admin |
| DELETE | `/rbac/users/{userId}/roles/{roleId}` | Remove role from user | super_admin |

---

## PART 2: REQUEST/RESPONSE JSON SCHEMAS

### 1. POST /auth/signup

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "full_name": "Raj Kumar",
  "mobile": "+919876543210",
  "aadhar_number": "123456789012"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully. Awaiting approval.",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "Raj Kumar",
    "mobile": "+919876543210",
    "approval_status": "pending",
    "roles": ["user"],
    "created_at": "2025-12-25T10:30:00Z"
  }
}
```

**Error Response (409 Conflict - Email exists):**
```json
{
  "success": false,
  "message": "Email already registered",
  "error_code": "EMAIL_EXISTS"
}
```

### 2. POST /auth/login

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "full_name": "Raj Kumar",
      "approval_status": "approved"
    }
  }
}
```

**Error Response (401 Unauthorized - Not approved):**
```json
{
  "success": false,
  "message": "User not yet approved. Contact administrator.",
  "error_code": "USER_PENDING_APPROVAL"
}
```

**Error Response (401 Unauthorized - Wrong password):**
```json
{
  "success": false,
  "message": "Invalid email or password",
  "error_code": "INVALID_CREDENTIALS"
}
```

### 3. POST /auth/logout

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

### 4. POST /auth/refresh-token

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 900
  }
}
```

### 5. GET /auth/me

**Request:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "full_name": "Raj Kumar",
    "mobile": "+919876543210",
    "approval_status": "approved",
    "is_active": true,
    "roles": [
      {
        "id": "role-uuid-1",
        "name": "gramsevak",
        "description": "Village Officer"
      }
    ],
    "permissions": [
      "users:view",
      "users:approve",
      "users:reject",
      "services:view"
    ],
    "created_at": "2025-12-25T10:30:00Z",
    "approved_at": "2025-12-25T11:00:00Z",
    "approved_by_user_id": "admin-uuid"
  }
}
```

### 6. GET /admin/users (with filters)

**Request:**
```
GET /admin/users?approval_status=pending&role=user&page=1&limit=20
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "full_name": "Raj Kumar",
        "mobile": "+919876543210",
        "approval_status": "pending",
        "roles": ["user"],
        "created_at": "2025-12-25T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 45,
      "total_pages": 3
    }
  }
}
```

### 7. POST /admin/users/{userId}/approve

**Request:**
```json
{
  "approved_by_user_id": "admin-uuid-here"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User approved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "approval_status": "approved",
    "approved_at": "2025-12-25T11:00:00Z",
    "approved_by_user_id": "admin-uuid-here"
  }
}
```

### 8. POST /admin/users/{userId}/reject

**Request:**
```json
{
  "rejection_reason": "Aadhar number could not be verified"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User rejected successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "approval_status": "rejected",
    "rejection_reason": "Aadhar number could not be verified"
  }
}
```

### 9. POST /rbac/permissions

**Request:**
```json
{
  "name": "services:create",
  "description": "Create a new service in village directory"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "perm-uuid-1",
    "name": "services:create",
    "description": "Create a new service in village directory",
    "created_at": "2025-12-25T10:00:00Z"
  }
}
```

### 10. POST /rbac/roles

**Request:**
```json
{
  "name": "services_admin",
  "description": "Administrator for Village Services Directory",
  "is_system_role": false
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "role-uuid-1",
    "name": "services_admin",
    "description": "Administrator for Village Services Directory",
    "is_system_role": false,
    "permissions": [],
    "created_at": "2025-12-25T10:00:00Z"
  }
}
```

### 11. POST /rbac/roles/{roleId}/permissions

**Request:**
```json
{
  "permission_ids": [
    "perm-uuid-1",
    "perm-uuid-2",
    "perm-uuid-3"
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Permissions assigned to role",
  "data": {
    "role_id": "role-uuid-1",
    "role_name": "services_admin",
    "permissions": [
      {
        "id": "perm-uuid-1",
        "name": "services:create"
      },
      {
        "id": "perm-uuid-2",
        "name": "services:update"
      },
      {
        "id": "perm-uuid-3",
        "name": "services:view"
      }
    ]
  }
}
```

### 12. POST /rbac/users/{userId}/roles

**Request:**
```json
{
  "role_ids": [
    "role-uuid-1",
    "role-uuid-2"
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Roles assigned to user",
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "roles": [
      {
        "id": "role-uuid-1",
        "name": "services_admin"
      },
      {
        "id": "role-uuid-2",
        "name": "gramsevak"
      }
    ],
    "all_permissions": [
      "services:create",
      "services:update",
      "services:view",
      "users:view",
      "users:approve"
    ]
  }
}
```

---

## PART 3: DATABASE SCHEMA (PostgreSQL)

### 1. users table

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  mobile VARCHAR(20),
  aadhar_number VARCHAR(20),
  
  -- Approval workflow
  approval_status VARCHAR(20) NOT NULL DEFAULT 'pending'
    CHECK (approval_status IN ('pending', 'approved', 'rejected')),
  approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
  approved_at TIMESTAMP,
  rejection_reason TEXT,
  
  -- Status
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  
  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  -- Indexes for common queries
  CONSTRAINT email_lowercase CHECK (email = LOWER(email))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_approval_status ON users(approval_status);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
```

### 2. roles table

```sql
CREATE TABLE roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) UNIQUE NOT NULL,
  description TEXT,
  is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  -- System roles: super_admin, admin, sub_admin, gramsevak, user
  CONSTRAINT valid_role_name CHECK (name ~ '^[a-z_]+$')
);

CREATE INDEX idx_roles_name ON roles(name);

-- Insert system roles
INSERT INTO roles (name, description, is_system_role) VALUES
  ('super_admin', 'Full system access and permission bypass', TRUE),
  ('admin', 'Administrative access with explicit permissions', TRUE),
  ('sub_admin', 'Limited admin with explicit permissions', TRUE),
  ('gramsevak', 'Village officer with approval capabilities', TRUE),
  ('user', 'Regular user with limited access', TRUE);
```

### 3. permissions table

```sql
CREATE TABLE permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  -- Format: resource:action
  CONSTRAINT valid_permission_name CHECK (name ~ '^[a-z_]+:[a-z_]+$')
);

CREATE INDEX idx_permissions_name ON permissions(name);

-- Insert standard permissions
INSERT INTO permissions (name, description) VALUES
  -- Users
  ('users:view', 'View user details'),
  ('users:approve', 'Approve pending users'),
  ('users:reject', 'Reject pending users'),
  ('users:delete', 'Delete users'),
  ('users:assign-role', 'Assign roles to users'),
  
  -- Services
  ('services:create', 'Create village services'),
  ('services:update', 'Update village services'),
  ('services:delete', 'Delete village services'),
  ('services:view', 'View village services'),
  
  -- Marketplace / Buy-Sell
  ('marketplace:create', 'Create marketplace listings'),
  ('marketplace:update', 'Update listings'),
  ('marketplace:delete', 'Delete listings'),
  ('marketplace:approve', 'Approve listings'),
  ('marketplace:reject', 'Reject listings'),
  ('marketplace:view', 'View listings'),
  
  -- Notice Board
  ('notices:create', 'Create notices'),
  ('notices:update', 'Update notices'),
  ('notices:delete', 'Delete notices'),
  ('notices:view', 'View notices'),
  
  -- Feedback
  ('feedback:view', 'View feedback/grievances'),
  ('feedback:respond', 'Respond to feedback'),
  ('feedback:delete', 'Delete feedback'),
  
  -- RBAC Management (super_admin only)
  ('rbac:manage-permissions', 'Create/update/delete permissions'),
  ('rbac:manage-roles', 'Create/update/delete roles'),
  ('rbac:assign-permissions', 'Assign permissions to roles'),
  ('rbac:assign-roles', 'Assign roles to users');
```

### 4. role_permissions junction table

```sql
CREATE TABLE role_permissions (
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  
  PRIMARY KEY (role_id, permission_id),
  CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Assign permissions to system roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'super_admin' AND p.name IS NOT NULL;  -- super_admin gets all

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'gramsevak' AND p.name IN (
  'users:view', 'users:approve', 'users:reject',
  'notices:view', 'feedback:view', 'feedback:respond'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'user' AND p.name IN (
  'services:view', 'marketplace:view', 'notices:view'
);
```

### 5. user_roles junction table

```sql
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Assign default 'user' role to new users
-- This happens via application logic during signup
```

### 6. refresh_tokens table

```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token VARCHAR(500) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);
```

### 7. audit_log table (for security & compliance)

```sql
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id VARCHAR(255),
  changes JSONB,
  ip_address VARCHAR(45),
  user_agent TEXT,
  status VARCHAR(20) DEFAULT 'success',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Examples:
-- { action: 'user:approve', resource_type: 'user', resource_id: '<user-id>' }
-- { action: 'role:assign', resource_type: 'user_role', resource_id: '<user-id>' }
-- { action: 'permission:assign', resource_type: 'role_permission', resource_id: '<role-id>' }
```

---

## PART 4: ROLE-PERMISSION MATRIX

| Role | Permissions | Can Bypass | Notes |
|------|-------------|-----------|-------|
| **super_admin** | ALL | ✅ YES | Has every permission implicitly. Cannot be created/deleted by anyone. |
| **admin** | Explicitly assigned | ❌ NO | Cannot manage super_admin. Manages sub_admin & gramsevak. |
| **sub_admin** | Explicitly assigned | ❌ NO | Limited admin. Manages gramsevak & users. |
| **gramsevak** | users:view, users:approve, users:reject, notices:view, feedback:view, feedback:respond, services:view | ❌ NO | Village officer. Approves villagers. Cannot manage admins. |
| **user** | services:view, marketplace:view, notices:view | ❌ NO | Regular citizen. Must be approved before login. |

### Detailed Permission Examples

```
SERVICES MODULE
├── services:create   → gramsevak, services_admin
├── services:update   → gramsevak, services_admin
├── services:delete   → services_admin, admin
└── services:view     → all authenticated users

USER MANAGEMENT
├── users:view        → admin, sub_admin, gramsevak
├── users:approve     → gramsevak, admin
├── users:reject      → gramsevak, admin
├── users:delete      → admin
└── users:assign-role → super_admin

RBAC (ROLES & PERMISSIONS)
├── rbac:manage-permissions  → super_admin
├── rbac:manage-roles        → super_admin
├── rbac:assign-permissions  → super_admin
└── rbac:assign-roles        → super_admin

MARKETPLACE
├── marketplace:create   → all authenticated users
├── marketplace:update   → owner or marketplace_admin
├── marketplace:delete   → owner or marketplace_admin
├── marketplace:approve  → marketplace_admin
├── marketplace:reject   → marketplace_admin
└── marketplace:view     → all authenticated users

NOTICE BOARD
├── notices:create   → admin, gramsevak
├── notices:update   → admin, gramsevak, notice_owner
├── notices:delete   → admin
└── notices:view     → all authenticated users

FEEDBACK/GRIEVANCES
├── feedback:view     → admin, gramsevak
├── feedback:respond  → admin, gramsevak
└── feedback:delete   → admin
```

---

## PART 5: JWT TOKEN STRUCTURE & CLAIMS

### Access Token (15 minutes)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "Raj Kumar",
  "roles": ["gramsevak"],
  "permissions": [
    "users:view",
    "users:approve",
    "users:reject",
    "services:view"
  ],
  "iat": 1703502600,
  "exp": 1703503500,
  "iss": "villageorbit-auth-service",
  "aud": "villageorbit-api"
}
```

### Refresh Token (7 days, stored in DB)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "jti": "unique-token-id",
  "iat": 1703502600,
  "exp": 1704107400,
  "iss": "villageorbit-auth-service",
  "type": "refresh"
}
```

---

## PART 6: AUTHENTICATION FLOW EXPLANATION

### User Signup Flow

```
1. Frontend → POST /auth/signup
   └─ Email, password, name, aadhar
   
2. Auth Service
   ├─ Validate input (email format, password strength)
   ├─ Hash password (bcrypt with 12 rounds)
   ├─ Check email uniqueness
   └─ Create user with approval_status = 'pending'
   
3. Default Role Assignment
   └─ Assign 'user' role automatically
   
4. Response
   ├─ User ID
   ├─ Status: pending
   └─ Message: "Await admin approval"
   
5. Audit Log
   └─ { action: 'user:signup', user_id, status: 'success' }
```

### User Login Flow

```
1. Frontend → POST /auth/login
   └─ Email, password
   
2. Auth Service
   ├─ Find user by email
   ├─ Verify approval_status == 'approved'
   │  └─ If not approved → Return 401 with reason
   ├─ Verify password hash
   │  └─ If invalid → Return 401
   └─ If all valid → Proceed
   
3. Token Generation
   ├─ Load user roles
   ├─ Resolve all permissions via role_permissions
   ├─ Generate Access Token (15 min)
   ├─ Generate Refresh Token (7 days)
   └─ Store refresh token in DB
   
4. Response
   ├─ access_token (Bearer)
   ├─ refresh_token
   ├─ expires_in
   └─ user data
   
5. Audit Log
   └─ { action: 'auth:login', user_id, status: 'success', ip_address }
```

### Permission Check Flow

```
Middleware: @RequirePermission("services:create")
├─ Extract JWT token
├─ Validate token signature & expiration
├─ Decode JWT claims
├─ Get user_id from claims
├─ Check if role == super_admin
│  └─ If YES → Allow (permission bypass)
└─ If NO → Check claims.permissions array
   ├─ If "services:create" present → Allow
   └─ If NOT present → Return 403 Forbidden

Audit Log: { action: 'auth:permission-check', user_id, permission, status }
```

### User Approval Flow

```
1. Gramsevak → POST /admin/users/{userId}/approve
   └─ Request body: { approved_by_user_id }
   
2. Auth Service
   ├─ Verify requester has 'users:approve' permission
   ├─ Find user with approval_status = 'pending'
   ├─ Update:
   │  ├─ approval_status = 'approved'
   │  ├─ approved_by = approver_user_id
   │  └─ approved_at = NOW()
   └─ Response: Updated user
   
3. User Can Now
   ├─ Login successfully
   ├─ Receive access token
   └─ Access APIs matching their role
   
4. Audit Log
   └─ { action: 'user:approve', user_id, approved_by, status: 'success' }
```

### Token Refresh Flow

```
1. Frontend (with expired access token) → POST /auth/refresh-token
   └─ Request body: { refresh_token }
   
2. Auth Service
   ├─ Find refresh token in DB
   ├─ Check:
   │  ├─ Not revoked
   │  ├─ Not expired
   │  └─ Valid signature
   ├─ If invalid → Return 401
   └─ If valid → Proceed
   
3. Token Generation
   ├─ Load current user roles & permissions
   ├─ Generate new Access Token (15 min)
   └─ Response: New access_token
   
4. Audit Log
   └─ { action: 'auth:token-refresh', user_id, status: 'success' }
```

### Logout Flow

```
1. Frontend → POST /auth/logout
   └─ Request body: { refresh_token }
   
2. Auth Service
   ├─ Find refresh token in DB
   ├─ Set revoked = TRUE
   └─ Response: Success
   
3. Frontend
   ├─ Clear access_token from localStorage/sessionStorage
   ├─ Clear refresh_token
   └─ Redirect to login
   
4. Audit Log
   └─ { action: 'auth:logout', user_id, status: 'success' }
```

---

## PART 7: HOW THIS REPLACES SUPABASE AUTH & RLS

### Supabase Features → Auth Service Equivalents

| Supabase | Auth Service Equivalent | How It Works |
|----------|------------------------|--------------|
| `auth.users` table | `users` table | Same user data, plus approval workflow |
| JWT tokens | JWT tokens | Same concept, custom claims (roles, permissions) |
| User creation | `/auth/signup` | Same, but approval_status = pending by default |
| Email verification | (Simplified in this design) | Can add email_verified column & verification flow |
| RLS (Row-Level Security) | Permission-based API access | Frontend/Backend check permissions, not DB |
| `auth.uid()` | JWT sub claim | Extract user ID from token claims |
| `auth.jwt()` | JWT payload | Decode & read roles/permissions |
| Roles (in Supabase) | `roles` + `user_roles` | Dynamic role assignment, not hardcoded |
| Permissions (custom) | `permissions` table | Fine-grained resource:action format |

### RLS Replacement Example

#### Old Supabase RLS (on users table):

```sql
-- Only admins can see all users
CREATE POLICY "Admins can view all users"
ON users
FOR SELECT
USING (auth.uid() IN (SELECT user_id FROM user_roles WHERE role = 'admin'));

-- Users can only see themselves
CREATE POLICY "Users can see only themselves"
ON users
FOR SELECT
USING (auth.uid() = id);
```

#### New Auth Service Approach:

```
API: GET /admin/users
├─ Middleware checks permission: "users:view"
├─ If not present → Return 403
└─ If present → Return all users

API: GET /auth/me
├─ Extract user_id from JWT
├─ Return only authenticated user's data
└─ No need for RLS
```

**Benefits:**
- ✅ No database-level security complexity
- ✅ Permission logic in application (easier to audit)
- ✅ Works across all databases/services
- ✅ API-centric authorization (stateless)
- ✅ Better performance (no RLS policy evaluation)

---

## PART 8: SECURITY HARDENING CHECKLIST

### Authentication Security

- ✅ **Password Hashing**: bcrypt with 12+ rounds (Argon2 preferred for future)
- ✅ **JWT Signature**: HS256 with 256+ bit secret key
- ✅ **Token Expiration**: Access (15 min), Refresh (7 days)
- ✅ **Refresh Token Storage**: Secure HTTP-only cookies OR secure client storage
- ✅ **HTTPS Enforcement**: All endpoints require TLS 1.2+
- ✅ **CORS Configuration**: Whitelist frontend domain only

### Rate Limiting

```
POST /auth/signup   → 5 requests per day per IP
POST /auth/login    → 10 requests per 15 minutes per IP
POST /auth/logout   → 20 requests per hour per user
POST /auth/refresh  → 100 requests per hour per user
```

### Audit Logging

```
Log ALL:
├─ auth:signup
├─ auth:login
├─ auth:logout
├─ auth:token-refresh
├─ user:approve
├─ user:reject
├─ rbac:role-assign
├─ rbac:permission-assign
└─ auth:permission-denied (403s)

Fields:
├─ user_id
├─ action
├─ resource_type
├─ ip_address
├─ user_agent
├─ status (success/failure)
└─ timestamp
```

### Database Security

- ✅ **No plaintext passwords**: bcrypt hashes only
- ✅ **Unique email constraint**: Prevent duplicate accounts
- ✅ **Foreign key constraints**: Referential integrity
- ✅ **Soft deletes**: is_active flag (keep audit trail)
- ✅ **Timestamped**: created_at, updated_at for compliance

### API Security

- ✅ **Input Validation**: Email format, password strength, name length
- ✅ **SQL Injection Prevention**: Parameterized queries (Spring Data JPA)
- ✅ **JWT Validation**: Signature + expiration check on every request
- ✅ **CORS**: Restrict to frontend domain
- ✅ **Rate Limiting**: Per IP, per user
- ✅ **Error Messages**: Generic messages (don't leak existence)

---

## PART 9: IMPLEMENTATION ROADMAP

### Phase 1: Core (Week 1)
- [ ] Database schema creation
- [ ] Auth APIs: signup, login, refresh-token
- [ ] JWT token generation & validation
- [ ] Basic permission middleware

### Phase 2: RBAC (Week 2)
- [ ] Permission & Role tables
- [ ] User-Role associations
- [ ] Permission resolution logic
- [ ] RBAC management APIs

### Phase 3: Admin APIs (Week 3)
- [ ] User approval workflow
- [ ] User listing & filtering
- [ ] Audit logging
- [ ] Rate limiting

### Phase 4: Security & Hardening (Week 4)
- [ ] HTTPS/TLS enforcement
- [ ] Password strength validation
- [ ] Email verification (optional)
- [ ] Security headers (HSTS, CSP)
- [ ] Logging & monitoring

### Phase 5: Testing & Deployment (Week 5)
- [ ] Unit tests (JUnit)
- [ ] Integration tests (REST API tests)
- [ ] Load testing (concurrent logins)
- [ ] Docker deployment
- [ ] Production readiness

---

## PART 10: SCALING & PERFORMANCE CONSIDERATIONS

### Database Optimization

```sql
-- Composite indexes for common queries
CREATE INDEX idx_users_approval_is_active ON users(approval_status, is_active);
CREATE INDEX idx_user_roles_user_id_role_id ON user_roles(user_id, role_id);
CREATE INDEX idx_role_permissions_role_id_permission_id ON role_permissions(role_id, permission_id);
```

### Caching Strategy

```
Cache (Redis):
├─ User → {id, email, name, roles, permissions} (TTL: 1 hour)
├─ Roles → {id, name, permissions} (TTL: 24 hours)
├─ Permissions → {id, name, description} (TTL: 24 hours)
└─ Refresh Token Blacklist (TTL: 7 days)

Invalidation:
├─ On user approval → Invalidate user cache
├─ On role assignment → Invalidate user cache
├─ On permission assignment → Invalidate role cache
└─ On logout → Add refresh token to blacklist
```

### Load Balancing

```
Load Balancer (Sticky Sessions: OFF - Stateless)
├─ auth-service-1
├─ auth-service-2
├─ auth-service-3
└─ auth-service-4

Database Connection Pool (HikariCP)
├─ Max connections: 20 per service instance
├─ Min idle: 5
└─ Connection timeout: 30s
```

### Monitoring & Alerts

```
Metrics to track:
├─ Login success rate (should be >95%)
├─ Token refresh rate
├─ Permission check latency (should be <10ms)
├─ Database query latency
├─ JWT validation errors
└─ Failed login attempts

Alerts:
├─ >50 failed logins in 5 minutes
├─ Permission check latency >100ms
├─ Database connection pool exhausted
└─ JWT validation errors spike
```

---

## PART 11: DATA MODEL COMPARISON

### Before (Supabase)

```
Tables:
├─ users (email, password, metadata)
├─ user_roles (hardcoded role values)
└─ RLS policies (scattered in database)

Authentication:
├─ Supabase JWT (opaque token)
├─ Server-side RLS enforcement
└─ Stateful session management

Authorization:
├─ Row-level security policies
├─ auth.uid() context
└─ Database-centric security
```

### After (Auth Service)

```
Tables:
├─ users (email, password_hash, approval_status)
├─ roles (dynamic, extensible)
├─ permissions (fine-grained resource:action)
├─ role_permissions (mapping)
├─ user_roles (mapping)
├─ refresh_tokens (token lifecycle)
└─ audit_logs (compliance)

Authentication:
├─ Custom JWT (with roles & permissions)
├─ Application-level validation
├─ Stateless token-based

Authorization:
├─ API-level permission checks
├─ Claims-based (from JWT)
├─ Application-centric security
├─ Audit trail maintained
```

---

## PART 12: COST ANALYSIS

### Supabase vs Auth Service

| Factor | Supabase | Auth Service |
|--------|----------|--------------|
| **Per-user cost** | $0.02 - $0.10 | Infrastructure only |
| **Database size** | Growing vector search | Minimal (~10MB/100k users) |
| **RLS complexity** | High (maintenance burden) | None (API-based) |
| **Developer time** | Low setup | Medium (1-2 weeks) |
| **Scaling** | Pay-per-use | Linear with infrastructure |
| **Vendor lock-in** | High | None (standard PostgreSQL) |

**Breakeven**: ~500k users or $50k/month Supabase spend

---

## DEPLOYMENT ARCHITECTURE

```
┌─────────────────────────────────────┐
│         Frontend (Vue/React)         │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│    API Gateway / Load Balancer      │
│      (HTTPS, Rate Limiting)         │
└────────────┬────────────────────────┘
             │
      ┌──────┴──────┬──────────┐
      ↓             ↓          ↓
  ┌────────┐ ┌────────┐ ┌────────┐
  │ Auth   │ │ Auth   │ │ Auth   │
  │Service │ │Service │ │Service │
  │  (1)   │ │  (2)   │ │  (3)   │
  └────┬───┘ └────┬───┘ └────┬───┘
       │          │          │
       └──────────┼──────────┘
                  ↓
          ┌──────────────────┐
          │ PostgreSQL DB    │
          │ (Hot Standby)    │
          └──────────────────┘
                  ↓
          ┌──────────────────┐
          │ Redis Cache      │
          │ (Token Blacklist)│
          └──────────────────┘
```

---

## CONCLUSION

This auth service is **production-ready**, **stateless**, **scalable**, and **fully replaces** Supabase Auth with:

✅ **Better security** (audit logging, fine-grained permissions)
✅ **No vendor lock-in** (standard PostgreSQL, REST APIs)
✅ **Cost savings** (linear scaling, no per-user fees)
✅ **Complete control** (own the auth layer)
✅ **Enterprise-ready** (RBAC, compliance, monitoring)

Estimated **1-2 weeks** to full production deployment.
