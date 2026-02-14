package com.restaurantpos.paymentsbilling.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.paymentsbilling.model.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * FiscalDocument entity representing a fiscal document (invoice, receipt, credit note).
 * Fiscal documents have sequential numbering per tenant and site for compliance.
 * 
 * Requirements: 7.6
 */
@Entity
@Table(name = "fiscal_documents")
public class FiscalDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "site_id", nullable = false)
    private UUID siteId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;
    
    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "customer_nif", length = 20)
    private String customerNif;
    
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();
    
    @Column(name = "voided_at")
    private Instant voidedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    protected FiscalDocument() {
        // JPA requires a no-arg constructor
    }
    
    public FiscalDocument(UUID tenantId, UUID siteId, DocumentType documentType, String documentNumber, 
                          UUID orderId, BigDecimal amount, String customerNif) {
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.orderId = orderId;
        this.amount = amount;
        this.customerNif = customerNif;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getSiteId() {
        return siteId;
    }
    
    public DocumentType getDocumentType() {
        return documentType;
    }
    
    public String getDocumentNumber() {
        return documentNumber;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCustomerNif() {
        return customerNif;
    }
    
    public Instant getIssuedAt() {
        return issuedAt;
    }
    
    public Instant getVoidedAt() {
        return voidedAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    // Setters
    
    public void setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Checks if the fiscal document has been voided.
     */
    public boolean isVoided() {
        return voidedAt != null;
    }
    
    /**
     * Checks if this is an invoice document.
     */
    public boolean isInvoice() {
        return documentType == DocumentType.INVOICE;
    }
    
    /**
     * Checks if this is a receipt document.
     */
    public boolean isReceipt() {
        return documentType == DocumentType.RECEIPT;
    }
    
    /**
     * Checks if this is a credit note document.
     */
    public boolean isCreditNote() {
        return documentType == DocumentType.CREDIT_NOTE;
    }
}
