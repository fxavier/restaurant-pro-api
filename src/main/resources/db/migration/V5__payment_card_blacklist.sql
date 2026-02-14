-- V5__payment_card_blacklist.sql: Add payment card blacklist table

CREATE TABLE payment_card_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    card_last_four VARCHAR(4) NOT NULL,
    reason TEXT,
    blocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payment_card_blacklist_tenant_card UNIQUE (tenant_id, card_last_four)
);

CREATE INDEX idx_payment_card_blacklist_tenant_card
    ON payment_card_blacklist(tenant_id, card_last_four);
