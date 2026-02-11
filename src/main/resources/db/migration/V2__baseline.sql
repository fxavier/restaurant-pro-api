-- V2__baseline.sql: Create all core tables for Restaurant POS SaaS
-- Multi-tenant architecture with tenant_id on all domain tables
-- Includes audit columns, unique constraints, and CHECK constraints for enums

-- ============================================================================
-- TENANT AND SITE MANAGEMENT
-- ============================================================================

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    timezone VARCHAR(50),
    settings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sites_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_sites_tenant ON sites(tenant_id);

-- ============================================================================
-- IDENTITY AND ACCESS MANAGEMENT
-- ============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER', 'WAITER', 'KITCHEN_STAFF')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username)
);

CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_revoked_expires ON refresh_tokens(user_id, revoked, expires_at);

-- ============================================================================
-- DINING ROOM MANAGEMENT
-- ============================================================================

CREATE TABLE dining_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    table_number VARCHAR(20) NOT NULL,
    area VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'BLOCKED')),
    capacity INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_dining_tables_tenant_site_number UNIQUE (tenant_id, site_id, table_number)
);

CREATE INDEX idx_dining_tables_tenant_site_status ON dining_tables(tenant_id, site_id, status);

CREATE TABLE blacklist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('TABLE', 'CARD')),
    entity_value VARCHAR(255) NOT NULL,
    reason TEXT,
    blocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    CONSTRAINT uq_blacklist_tenant_type_value UNIQUE (tenant_id, entity_type, entity_value)
);

CREATE INDEX idx_blacklist_tenant_type ON blacklist_entries(tenant_id, entity_type);

-- ============================================================================
-- CATALOG MANAGEMENT
-- ============================================================================

CREATE TABLE families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_families_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_families_tenant ON families(tenant_id);

CREATE TABLE subfamilies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_subfamilies_tenant_family_name UNIQUE (tenant_id, family_id, name)
);

CREATE INDEX idx_subfamilies_tenant_family ON subfamilies(tenant_id, family_id);

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subfamily_id UUID NOT NULL REFERENCES subfamilies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_items_tenant_subfamily_available ON items(tenant_id, subfamily_id, available);

-- ============================================================================
-- ORDER MANAGEMENT
-- ============================================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    delivery_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_tenant_phone ON customers(tenant_id, phone);
CREATE INDEX idx_customers_phone_suffix ON customers(tenant_id, phone varchar_pattern_ops);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    table_id UUID REFERENCES dining_tables(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('DINE_IN', 'DELIVERY', 'TAKEOUT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CONFIRMED', 'PAID', 'CLOSED', 'VOIDED')),
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_tenant_site_status ON orders(tenant_id, site_id, status);
CREATE INDEX idx_orders_tenant_table_status ON orders(tenant_id, table_id, status);

CREATE TABLE order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    modifiers JSONB,
    notes TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'VOIDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_lines_order_status ON order_lines(order_id, status);

CREATE TABLE consumptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_line_id UUID NOT NULL REFERENCES order_lines(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    confirmed_at TIMESTAMP NOT NULL,
    voided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id)
);

CREATE INDEX idx_consumptions_tenant_order_line ON consumptions(tenant_id, order_line_id);

CREATE TABLE discounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    order_line_id UUID REFERENCES order_lines(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    amount DECIMAL(10,2) NOT NULL,
    reason TEXT,
    applied_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_discounts_order ON discounts(order_id);

-- ============================================================================
-- KITCHEN AND PRINTING
-- ============================================================================

CREATE TABLE printers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50),
    zone VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NORMAL', 'WAIT', 'IGNORE', 'REDIRECT')),
    redirect_to_printer_id UUID REFERENCES printers(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_printers_tenant_site_name UNIQUE (tenant_id, site_id, name)
);

CREATE INDEX idx_printers_tenant_site ON printers(tenant_id, site_id);

CREATE TABLE print_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    printer_id UUID NOT NULL REFERENCES printers(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PRINTED', 'FAILED', 'SKIPPED')),
    dedupe_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_print_jobs_tenant_dedupe UNIQUE (tenant_id, dedupe_key)
);

CREATE INDEX idx_print_jobs_tenant_printer_status ON print_jobs(tenant_id, printer_id, status);

-- ============================================================================
-- PAYMENTS AND BILLING
-- ============================================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'MOBILE', 'VOUCHER', 'MIXED')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'VOIDED')),
    idempotency_key VARCHAR(255) NOT NULL,
    terminal_transaction_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_payments_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_payments_tenant_order_status ON payments(tenant_id, order_id, status);

CREATE TABLE fiscal_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('RECEIPT', 'INVOICE', 'CREDIT_NOTE')),
    document_number VARCHAR(50) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    customer_nif VARCHAR(20),
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    voided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    CONSTRAINT uq_fiscal_docs_tenant_site_type_number UNIQUE (tenant_id, site_id, document_type, document_number)
);

CREATE INDEX idx_fiscal_docs_tenant_site_issued ON fiscal_documents(tenant_id, site_id, issued_at);

-- ============================================================================
-- CASH REGISTER MANAGEMENT
-- ============================================================================

CREATE TABLE cash_registers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    register_number VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cash_registers_tenant_site_number UNIQUE (tenant_id, site_id, register_number)
);

CREATE INDEX idx_cash_registers_tenant_site ON cash_registers(tenant_id, site_id);

CREATE TABLE cash_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    register_id UUID NOT NULL REFERENCES cash_registers(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES users(id),
    opening_amount DECIMAL(10,2) NOT NULL,
    expected_close DECIMAL(10,2),
    actual_close DECIMAL(10,2),
    variance DECIMAL(10,2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cash_sessions_tenant_register_status ON cash_sessions(tenant_id, register_id, status);
CREATE INDEX idx_cash_sessions_tenant_employee_opened ON cash_sessions(tenant_id, employee_id, opened_at);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_id UUID NOT NULL REFERENCES cash_sessions(id) ON DELETE CASCADE,
    movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('SALE', 'REFUND', 'DEPOSIT', 'WITHDRAWAL', 'OPENING', 'CLOSING')),
    amount DECIMAL(10,2) NOT NULL,
    reason TEXT,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id)
);

CREATE INDEX idx_cash_movements_tenant_session_type ON cash_movements(tenant_id, session_id, movement_type);

CREATE TABLE cash_closings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    closing_type VARCHAR(20) NOT NULL CHECK (closing_type IN ('SESSION', 'REGISTER', 'DAY', 'FINANCIAL_PERIOD')),
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    total_sales DECIMAL(10,2) NOT NULL,
    total_refunds DECIMAL(10,2) NOT NULL,
    variance DECIMAL(10,2) NOT NULL,
    closed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_by UUID NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_cash_closings_tenant_type_period ON cash_closings(tenant_id, closing_type, period_start);

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE tenants IS 'Multi-tenant SaaS: Each tenant represents a restaurant organization';
COMMENT ON TABLE sites IS 'Physical restaurant locations within a tenant (multi-location support)';
COMMENT ON TABLE users IS 'User accounts with role-based access control';
COMMENT ON TABLE dining_tables IS 'Restaurant tables with real-time status tracking';
COMMENT ON TABLE orders IS 'Customer orders with optimistic locking for concurrency control';
COMMENT ON TABLE order_lines IS 'Individual items within an order';
COMMENT ON TABLE consumptions IS 'Confirmed order lines after "Pedir" (order confirmation)';
COMMENT ON TABLE print_jobs IS 'Kitchen printing tasks with deduplication support';
COMMENT ON TABLE payments IS 'Payment transactions with idempotency key for duplicate prevention';
COMMENT ON TABLE fiscal_documents IS 'Legal invoices and receipts with sequential numbering';
COMMENT ON TABLE cash_sessions IS 'Cash register work periods with variance tracking';
COMMENT ON TABLE cash_movements IS 'All cash transactions and adjustments';
COMMENT ON TABLE cash_closings IS 'Financial closing reports at various levels';
