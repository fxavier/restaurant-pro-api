-- V3__indexes.sql: Hot path optimization indexes
-- This migration adds additional indexes for performance optimization
-- Note: Core indexes were already created in V2__baseline.sql

-- ============================================================================
-- HOT PATH INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================

-- The following indexes are already created in V2__baseline.sql:
-- - idx_dining_tables_tenant_site_status (tables by tenant+site+status)
-- - idx_orders_tenant_table_status (orders by tenant+table+status)
-- - idx_customers_tenant_phone (customers by tenant+phone)
-- - idx_customers_phone_suffix (customers by tenant+phone with varchar_pattern_ops for suffix search)
-- - idx_cash_sessions_tenant_register_status (cash_sessions by tenant+register+status)
-- - idx_print_jobs_tenant_printer_status (print_jobs by tenant+printer+status)
-- - uq_payments_tenant_idempotency (payments by tenant+idempotency_key - unique index)

-- ============================================================================
-- ADDITIONAL PERFORMANCE INDEXES
-- ============================================================================

-- Index for order line queries by item (useful for catalog reports)
CREATE INDEX IF NOT EXISTS idx_order_lines_item ON order_lines(item_id);

-- Index for consumptions by tenant and date (useful for sales reports)
CREATE INDEX IF NOT EXISTS idx_consumptions_tenant_confirmed ON consumptions(tenant_id, confirmed_at DESC);

-- Index for payments by creation date (useful for financial reports)
CREATE INDEX IF NOT EXISTS idx_payments_tenant_created ON payments(tenant_id, created_at DESC);

-- Index for fiscal documents by order (useful for document lookup)
CREATE INDEX IF NOT EXISTS idx_fiscal_docs_order ON fiscal_documents(order_id);

-- Index for cash movements by payment (useful for reconciliation)
CREATE INDEX IF NOT EXISTS idx_cash_movements_payment ON cash_movements(payment_id) WHERE payment_id IS NOT NULL;

-- Index for refresh tokens cleanup (useful for token expiry jobs)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE NOT revoked;

-- Index for blacklist lookups by entity value (useful for quick blacklist checks)
CREATE INDEX IF NOT EXISTS idx_blacklist_entity_value ON blacklist_entries(tenant_id, entity_value);

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON INDEX idx_order_lines_item IS 'Supports catalog reports and item popularity queries';
COMMENT ON INDEX idx_consumptions_tenant_confirmed IS 'Supports sales reports and analytics by date';
COMMENT ON INDEX idx_payments_tenant_created IS 'Supports financial reports and payment history queries';
COMMENT ON INDEX idx_fiscal_docs_order IS 'Supports document lookup by order';
COMMENT ON INDEX idx_cash_movements_payment IS 'Supports payment reconciliation queries';
COMMENT ON INDEX idx_refresh_tokens_expires IS 'Supports token cleanup jobs';
COMMENT ON INDEX idx_blacklist_entity_value IS 'Supports fast blacklist validation';

