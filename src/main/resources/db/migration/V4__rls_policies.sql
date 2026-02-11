-- V4__rls_policies.sql: PostgreSQL Row Level Security (RLS) policies for tenant isolation
-- This migration is OPTIONAL and can be applied when database-level tenant isolation is required
-- RLS provides an additional security layer beyond application-level filtering
--
-- IMPORTANT: Before each request, the application must set the tenant context:
--   SET app.tenant_id = '<tenant-uuid>';
--
-- This ensures that RLS policies can enforce tenant isolation at the database level

-- ============================================================================
-- ENABLE ROW LEVEL SECURITY ON ALL DOMAIN TABLES
-- ============================================================================

-- Tenant and Site Management
ALTER TABLE sites ENABLE ROW LEVEL SECURITY;

-- Identity and Access Management
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Dining Room Management
ALTER TABLE dining_tables ENABLE ROW LEVEL SECURITY;
ALTER TABLE blacklist_entries ENABLE ROW LEVEL SECURITY;

-- Catalog Management
ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE subfamilies ENABLE ROW LEVEL SECURITY;
ALTER TABLE items ENABLE ROW LEVEL SECURITY;

-- Customer Management
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

-- Order Management
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE consumptions ENABLE ROW LEVEL SECURITY;

-- Kitchen and Printing
ALTER TABLE printers ENABLE ROW LEVEL SECURITY;
ALTER TABLE print_jobs ENABLE ROW LEVEL SECURITY;

-- Payments and Billing
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE fiscal_documents ENABLE ROW LEVEL SECURITY;

-- Cash Register Management
ALTER TABLE cash_registers ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_closings ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- CREATE TENANT ISOLATION POLICIES
-- ============================================================================
-- Each policy restricts access to rows where tenant_id matches the session variable
-- The current_setting function retrieves the tenant_id set by the application

-- Tenant and Site Management
CREATE POLICY tenant_isolation_policy ON sites
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Identity and Access Management
CREATE POLICY tenant_isolation_policy ON users
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Dining Room Management
CREATE POLICY tenant_isolation_policy ON dining_tables
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON blacklist_entries
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Catalog Management
CREATE POLICY tenant_isolation_policy ON families
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON subfamilies
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON items
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Customer Management
CREATE POLICY tenant_isolation_policy ON customers
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Order Management
CREATE POLICY tenant_isolation_policy ON orders
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON consumptions
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Kitchen and Printing
CREATE POLICY tenant_isolation_policy ON printers
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON print_jobs
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Payments and Billing
CREATE POLICY tenant_isolation_policy ON payments
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON fiscal_documents
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- Cash Register Management
CREATE POLICY tenant_isolation_policy ON cash_registers
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON cash_sessions
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON cash_movements
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON cash_closings
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON POLICY tenant_isolation_policy ON sites IS 
    'Restricts access to sites belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON users IS 
    'Restricts access to users belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON dining_tables IS 
    'Restricts access to tables belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON blacklist_entries IS 
    'Restricts access to blacklist entries belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON families IS 
    'Restricts access to catalog families belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON subfamilies IS 
    'Restricts access to catalog subfamilies belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON items IS 
    'Restricts access to catalog items belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON customers IS 
    'Restricts access to customers belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON orders IS 
    'Restricts access to orders belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON consumptions IS 
    'Restricts access to consumptions belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON printers IS 
    'Restricts access to printers belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON print_jobs IS 
    'Restricts access to print jobs belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON payments IS 
    'Restricts access to payments belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON fiscal_documents IS 
    'Restricts access to fiscal documents belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON cash_registers IS 
    'Restricts access to cash registers belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON cash_sessions IS 
    'Restricts access to cash sessions belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON cash_movements IS 
    'Restricts access to cash movements belonging to the current tenant (app.tenant_id)';

COMMENT ON POLICY tenant_isolation_policy ON cash_closings IS 
    'Restricts access to cash closings belonging to the current tenant (app.tenant_id)';

-- ============================================================================
-- USAGE NOTES
-- ============================================================================
-- 
-- 1. Application Setup:
--    Before executing any queries, the application must set the tenant context:
--    
--    jdbcTemplate.execute("SET app.tenant_id = '" + tenantId + "'");
--    
--    Or using a connection wrapper/filter that sets this automatically.
--
-- 2. Testing RLS:
--    To test RLS policies, connect to the database and run:
--    
--    SET app.tenant_id = '<some-tenant-uuid>';
--    SELECT * FROM orders;  -- Will only return orders for that tenant
--    
--    SET app.tenant_id = '<different-tenant-uuid>';
--    SELECT * FROM orders;  -- Will only return orders for the different tenant
--
-- 3. Disabling RLS (for admin operations):
--    If you need to bypass RLS for administrative queries:
--    
--    SET SESSION AUTHORIZATION postgres;  -- Or another superuser
--    -- RLS is bypassed for superusers
--
-- 4. Performance Considerations:
--    - RLS policies add a WHERE clause to every query
--    - Ensure tenant_id columns are indexed (already done in V3__indexes.sql)
--    - The performance impact is minimal with proper indexing
--
-- 5. Security Benefits:
--    - Defense in depth: Even if application-level filtering fails, RLS prevents cross-tenant data access
--    - Protection against SQL injection that bypasses application filters
--    - Compliance: Demonstrates strong tenant isolation for regulatory requirements
--
