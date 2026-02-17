package com.restaurantpos.cashregister.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.cashregister.model.ClosingType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CashClosing entity representing a financial closing at various levels.
 * 
 * Requirements: 10.7, 10.8
 */
@Entity
@Table(name = "cash_closings")
public class CashClosing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "closing_type", nullable = false, length = 20)
    private ClosingType closingType;
    
    @Column(name = "period_start", nullable = false)
    private Instant periodStart;
    
    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;
    
    @Column(name = "total_sales", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSales;
    
    @Column(name = "total_refunds", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRefunds;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal variance;
    
    @Column(name = "closed_at", nullable = false)
    private Instant closedAt = Instant.now();
    
    @Column(name = "closed_by", nullable = false)
    private UUID closedBy;
    
    protected CashClosing() {
        // JPA requires a no-arg constructor
    }
    
    public CashClosing(UUID tenantId, ClosingType closingType, Instant periodStart, Instant periodEnd,
                       BigDecimal totalSales, BigDecimal totalRefunds, BigDecimal variance, UUID closedBy) {
        this.tenantId = tenantId;
        this.closingType = closingType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalSales = totalSales;
        this.totalRefunds = totalRefunds;
        this.variance = variance;
        this.closedBy = closedBy;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public ClosingType getClosingType() {
        return closingType;
    }
    
    public Instant getPeriodStart() {
        return periodStart;
    }
    
    public Instant getPeriodEnd() {
        return periodEnd;
    }
    
    public BigDecimal getTotalSales() {
        return totalSales;
    }
    
    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }
    
    public BigDecimal getVariance() {
        return variance;
    }
    
    public Instant getClosedAt() {
        return closedAt;
    }
    
    public UUID getClosedBy() {
        return closedBy;
    }
    
    /**
     * Checks if this is a session closing.
     */
    public boolean isSessionClosing() {
        return closingType == ClosingType.SESSION;
    }
    
    /**
     * Checks if this is a register closing.
     */
    public boolean isRegisterClosing() {
        return closingType == ClosingType.REGISTER;
    }
    
    /**
     * Checks if this is a day closing.
     */
    public boolean isDayClosing() {
        return closingType == ClosingType.DAY;
    }
    
    /**
     * Checks if this is a financial period closing.
     */
    public boolean isFinancialPeriodClosing() {
        return closingType == ClosingType.FINANCIAL_PERIOD;
    }
}
