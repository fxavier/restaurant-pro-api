-- V7__add_super_admin_role.sql: Add SUPER_ADMIN role and allow NULL tenant_id for super admins

-- Drop the existing role constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add new constraint with SUPER_ADMIN role
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CASHIER', 'WAITER', 'KITCHEN_STAFF'));

-- Allow NULL tenant_id for super admins only
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_tenant_id_fkey;

-- Add conditional foreign key: tenant_id must exist if not null
ALTER TABLE users ADD CONSTRAINT users_tenant_id_fkey 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Add check constraint: tenant_id can only be NULL for SUPER_ADMIN
ALTER TABLE users ADD CONSTRAINT users_tenant_id_null_check
    CHECK ((role = 'SUPER_ADMIN' AND tenant_id IS NULL) OR (role != 'SUPER_ADMIN' AND tenant_id IS NOT NULL));

-- Update unique constraint to handle NULL tenant_id
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_tenant_username;

-- For super admins, username must be unique globally
-- For other users, username must be unique per tenant
CREATE UNIQUE INDEX uq_users_super_admin_username ON users(username) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_users_tenant_username ON users(tenant_id, username) WHERE tenant_id IS NOT NULL;
