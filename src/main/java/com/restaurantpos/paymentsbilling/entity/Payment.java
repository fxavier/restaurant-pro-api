package com.restaurantpos.paymentsbilling.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Payment entity representing a payment transaction for an order.
 * Supports multiple payment methods and idempotency for safe retries.
 * 
 * Requirements: 7.1, 7.2
 */
@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;
    
    @Column(name = "terminal_transaction_id", length = 255)
    private String terminalTransactionId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected Payment() {
        // JPA requires a no-arg constructor
    }
    
    public Payment(UUID tenantId, UUID orderId, BigDecimal amount, PaymentMethod paymentMethod, String idempotencyKey) {
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public String getTerminalTransactionId() {
        return terminalTransactionId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    // Setters
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setTerminalTransactionId(String terminalTransactionId) {
        this.terminalTransactionId = terminalTransactionId;
        this.updatedAt = Instant.now();
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Checks if the payment is pending.
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    /**
     * Checks if the payment has been completed.
     */
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    /**
     * Checks if the payment has failed.
     */
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
    
    /**
     * Checks if the payment has been voided.
     */
    public boolean isVoided() {
        return status == PaymentStatus.VOIDED;
    }
}
