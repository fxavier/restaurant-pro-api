package com.restaurantpos.orders.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.orders.model.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Discount entity representing a price reduction applied to an order or order line.
 * Discounts can be percentage-based or fixed amounts and require authorization.
 * 
 * Requirements: 5.7
 */
@Entity
@Table(name = "discounts")
public class Discount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "order_line_id")
    private UUID orderLineId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "applied_by", nullable = false)
    private UUID appliedBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    protected Discount() {
        // JPA requires a no-arg constructor
    }
    
    public Discount(UUID orderId, UUID orderLineId, DiscountType type, BigDecimal amount, String reason, UUID appliedBy) {
        this.orderId = orderId;
        this.orderLineId = orderLineId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.appliedBy = appliedBy;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public UUID getOrderLineId() {
        return orderLineId;
    }
    
    public DiscountType getType() {
        return type;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public UUID getAppliedBy() {
        return appliedBy;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Checks if this discount applies to the entire order.
     */
    public boolean isOrderDiscount() {
        return orderLineId == null;
    }
    
    /**
     * Checks if this discount applies to a specific order line.
     */
    public boolean isLineDiscount() {
        return orderLineId != null;
    }
    
    /**
     * Checks if this is a percentage discount.
     */
    public boolean isPercentage() {
        return type == DiscountType.PERCENTAGE;
    }
    
    /**
     * Checks if this is a fixed amount discount.
     */
    public boolean isFixedAmount() {
        return type == DiscountType.FIXED_AMOUNT;
    }
}
