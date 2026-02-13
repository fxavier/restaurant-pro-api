package com.restaurantpos.orders.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.restaurantpos.orders.model.OrderLineStatus;

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
 * OrderLine entity representing an individual item within an order.
 * Order lines track quantity, pricing, modifiers, and status.
 * 
 * Requirements: 5.1, 5.2, 5.9
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "item_id", nullable = false)
    private UUID itemId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> modifiers;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderLineStatus status = OrderLineStatus.PENDING;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected OrderLine() {
        // JPA requires a no-arg constructor
    }
    
    public OrderLine(UUID orderId, UUID itemId, Integer quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public UUID getItemId() {
        return itemId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public Map<String, Object> getModifiers() {
        return modifiers;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public OrderLineStatus getStatus() {
        return status;
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
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.updatedAt = Instant.now();
    }
    
    public void setModifiers(Map<String, Object> modifiers) {
        this.modifiers = modifiers;
        this.updatedAt = Instant.now();
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(OrderLineStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the order line is pending confirmation.
     */
    public boolean isPending() {
        return status == OrderLineStatus.PENDING;
    }
    
    /**
     * Checks if the order line has been confirmed.
     */
    public boolean isConfirmed() {
        return status == OrderLineStatus.CONFIRMED;
    }
    
    /**
     * Checks if the order line has been voided.
     */
    public boolean isVoided() {
        return status == OrderLineStatus.VOIDED;
    }
    
    /**
     * Calculates the total price for this order line.
     */
    public BigDecimal calculateTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
