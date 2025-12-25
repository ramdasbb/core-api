-- V1__create_auth_schema.sql
-- Initial Auth Service schema

-- Users table
CREATE TABLE IF NOT EXISTS users (
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
    
    -- Constraint for email lowercase
    CONSTRAINT email_lowercase CHECK (email = LOWER(email))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_approval_status ON users(approval_status);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_approval_is_active ON users(approval_status, is_active);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_role_name CHECK (name ~ '^[a-z_]+$')
);

CREATE INDEX idx_roles_name ON roles(name);

-- Insert system roles
INSERT INTO roles (name, description, is_system_role) VALUES
    ('super_admin', 'Full system access and permission bypass', TRUE),
    ('admin', 'Administrative access with explicit permissions', TRUE),
    ('sub_admin', 'Limited admin with explicit permissions', TRUE),
    ('gramsevak', 'Village officer with approval capabilities', TRUE),
    ('user', 'Regular user with limited access', TRUE)
ON CONFLICT DO NOTHING;

-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_permission_name CHECK (name ~ '^[a-z_]+:[a-z_-]+$')
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
    ('rbac:assign-roles', 'Assign roles to users')
ON CONFLICT DO NOTHING;

-- Role-Permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- User-Roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_user_id_role_id ON user_roles(user_id, role_id);

-- Refresh Tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
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

-- Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
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
