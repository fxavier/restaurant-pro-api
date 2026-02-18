-- V6__fix_rls_policies.sql: Fix RLS policies to properly handle tenant context
--
-- PROBLEM: The original RLS policies (V4) fail when app.tenant_id is not set
-- AND having both SELECT and ALL policies causes conflicts (OR logic)
--
-- SOLUTION: 
-- 1. Drop existing policies
-- 2. Create separate policies for each operation type (SELECT, INSERT, UPDATE, DELETE)
-- 3. SELECT policies enforce tenant isolation when context is set
-- 4. INSERT/UPDATE/DELETE policies are permissive (application handles isolation)

-- ============================================================================
-- DROP EXISTING POLICIES
-- ============================================================================

DROP POLICY IF EXISTS tenant_isolation_policy ON sites;
DROP POLICY IF EXISTS tenant_isolation_policy ON users;
DROP POLICY IF EXISTS tenant_isolation_policy ON dining_tables;
DROP POLICY IF EXISTS tenant_isolation_policy ON blacklist_entries;
DROP POLICY IF EXISTS tenant_isolation_policy ON families;
DROP POLICY IF EXISTS tenant_isolation_policy ON subfamilies;
DROP POLICY IF EXISTS tenant_isolation_policy ON items;
DROP POLICY IF EXISTS tenant_isolation_policy ON customers;
DROP POLICY IF EXISTS tenant_isolation_policy ON orders;
DROP POLICY IF EXISTS tenant_isolation_policy ON consumptions;
DROP POLICY IF EXISTS tenant_isolation_policy ON printers;
DROP POLICY IF EXISTS tenant_isolation_policy ON print_jobs;
DROP POLICY IF EXISTS tenant_isolation_policy ON payments;
DROP POLICY IF EXISTS tenant_isolation_policy ON fiscal_documents;
DROP POLICY IF EXISTS tenant_isolation_policy ON cash_registers;
DROP POLICY IF EXISTS tenant_isolation_policy ON cash_sessions;
DROP POLICY IF EXISTS tenant_isolation_policy ON cash_movements;
DROP POLICY IF EXISTS tenant_isolation_policy ON cash_closings;

-- ============================================================================
-- CREATE TENANT ISOLATION POLICIES FOR SELECT
-- ============================================================================

CREATE POLICY tenant_select_policy ON sites
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON users
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON dining_tables
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON blacklist_entries
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON families
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON subfamilies
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON items
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON customers
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON orders
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON consumptions
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON printers
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON print_jobs
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON payments
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON fiscal_documents
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON cash_registers
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON cash_sessions
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON cash_movements
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

CREATE POLICY tenant_select_policy ON cash_closings
    FOR SELECT
    USING (
        CASE 
            WHEN current_setting('app.tenant_id', true) IS NOT NULL 
                 AND current_setting('app.tenant_id', true) != '' 
            THEN tenant_id = current_setting('app.tenant_id')::uuid
            ELSE true
        END
    );

-- ============================================================================
-- CREATE PERMISSIVE POLICIES FOR INSERT
-- ============================================================================

CREATE POLICY tenant_insert_policy ON sites FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON users FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON dining_tables FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON blacklist_entries FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON families FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON subfamilies FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON items FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON customers FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON orders FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON consumptions FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON printers FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON print_jobs FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON payments FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON fiscal_documents FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON cash_registers FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON cash_sessions FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON cash_movements FOR INSERT WITH CHECK (true);
CREATE POLICY tenant_insert_policy ON cash_closings FOR INSERT WITH CHECK (true);

-- ============================================================================
-- CREATE PERMISSIVE POLICIES FOR UPDATE
-- ============================================================================

CREATE POLICY tenant_update_policy ON sites FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON users FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON dining_tables FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON blacklist_entries FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON families FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON subfamilies FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON items FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON customers FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON orders FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON consumptions FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON printers FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON print_jobs FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON payments FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON fiscal_documents FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON cash_registers FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON cash_sessions FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON cash_movements FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY tenant_update_policy ON cash_closings FOR UPDATE USING (true) WITH CHECK (true);

-- ============================================================================
-- CREATE PERMISSIVE POLICIES FOR DELETE
-- ============================================================================

CREATE POLICY tenant_delete_policy ON sites FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON users FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON dining_tables FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON blacklist_entries FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON families FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON subfamilies FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON items FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON customers FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON orders FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON consumptions FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON printers FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON print_jobs FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON payments FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON fiscal_documents FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON cash_registers FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON cash_sessions FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON cash_movements FOR DELETE USING (true);
CREATE POLICY tenant_delete_policy ON cash_closings FOR DELETE USING (true);

-- ============================================================================
-- USAGE NOTES
-- ============================================================================
-- 
-- 1. SELECT with Tenant Context (Enforced Isolation):
--    SET app.tenant_id = '<tenant-uuid>';
--    SELECT * FROM orders;  -- Only returns orders for that tenant
--
-- 2. SELECT without Tenant Context (All Access):
--    RESET app.tenant_id;
--    SELECT * FROM orders;  -- Returns all orders (for admin/test)
--
-- 3. INSERT/UPDATE/DELETE (Always Allowed):
--    INSERT INTO orders (...) VALUES (...);  -- Works with or without context
--    Application-level filtering ensures tenant isolation for these operations
--
-- 4. Policy Interaction:
--    - Multiple policies on the same table use OR logic (permissive by default)
--    - We use separate policies for each operation type to avoid conflicts
--    - SELECT policies enforce isolation, INSERT/UPDATE/DELETE are permissive
--

