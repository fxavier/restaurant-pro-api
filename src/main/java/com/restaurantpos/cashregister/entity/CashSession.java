package com.restaurantpos.cashregister.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.cashregister.model.CashSessionStatus;

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
 * CashSession entity representing a work period for a specific employee on a register.
 * 
 * Requirements: 10.2, 10.6
 */
@Entity
@Table(name = "cash_sessions")
public class CashSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "register_id", nullable = false)
    private UUID registerId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    @Column(name = "opening_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingAmount;
    
    @Column(name = "expected_close", precision = 10, scale = 2)
    private BigDecimal expectedClose;
    
    @Column(name = "actual_close", precision = 10, scale = 2)
    private BigDecimal actualClose;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal variance;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashSessionStatus status = CashSessionStatus.OPEN;
    
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();
    
    @Column(name = "closed_at")
    private Instant closedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected CashSession() {
        // JPA requires a no-arg constructor
    }
    
    public CashSession(UUID tenantId, UUID registerId, UUID employeeId, BigDecimal openingAmount) {
        this.tenantId = tenantId;
        this.registerId = registerId;
        this.employeeId = employeeId;
        this.openingAmount = openingAmount;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getRegisterId() {
        return registerId;
    }
    
    public UUID getEmployeeId() {
        return employeeId;
    }
    
    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }
    
    public BigDecimal getExpectedClose() {
        return expectedClose;
    }
    
    public BigDecimal getActualClose() {
        return actualClose;
    }
    
    public BigDecimal getVariance() {
        return variance;
    }
    
    public CashSessionStatus getStatus() {
        return status;
    }
    
    public Instant getOpenedAt() {
        return openedAt;
    }
    
    public Instant getClosedAt() {
        return closedAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    // Setters
    
    public void setExpectedClose(BigDecimal expectedClose) {
        this.expectedClose = expectedClose;
        this.updatedAt = Instant.now();
    }
    
    public void setActualClose(BigDecimal actualClose) {
        this.actualClose = actualClose;
        this.updatedAt = Instant.now();
    }
    
    public void setVariance(BigDecimal variance) {
        this.variance = variance;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(CashSessionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the session is open.
     */
    public boolean isOpen() {
        return status == CashSessionStatus.OPEN;
    }
    
    /**
     * Checks if the session is closed.
     */
    public boolean isClosed() {
        return status == CashSessionStatus.CLOSED;
    }
}
