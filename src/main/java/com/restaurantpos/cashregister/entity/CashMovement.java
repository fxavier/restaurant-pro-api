package com.restaurantpos.cashregister.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.cashregister.model.MovementType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CashMovement entity representing a cash transaction within a session.
 * 
 * Requirements: 10.3, 10.4
 */
@Entity
@Table(name = "cash_movements")
public class CashMovement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "payment_id")
    private UUID paymentId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    protected CashMovement() {
        // JPA requires a no-arg constructor
    }
    
    public CashMovement(UUID tenantId, UUID sessionId, MovementType movementType, BigDecimal amount) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.movementType = movementType;
        this.amount = amount;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public MovementType getMovementType() {
        return movementType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public UUID getPaymentId() {
        return paymentId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    // Setters
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Checks if this is a sale movement.
     */
    public boolean isSale() {
        return movementType == MovementType.SALE;
    }
    
    /**
     * Checks if this is a refund movement.
     */
    public boolean isRefund() {
        return movementType == MovementType.REFUND;
    }
    
    /**
     * Checks if this is a deposit movement.
     */
    public boolean isDeposit() {
        return movementType == MovementType.DEPOSIT;
    }
    
    /**
     * Checks if this is a withdrawal movement.
     */
    public boolean isWithdrawal() {
        return movementType == MovementType.WITHDRAWAL;
    }
}
