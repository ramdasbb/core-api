-- Add reset password fields to users table
ALTER TABLE users
ADD COLUMN reset_password_token VARCHAR(255),
ADD COLUMN reset_password_expiry TIMESTAMP;
