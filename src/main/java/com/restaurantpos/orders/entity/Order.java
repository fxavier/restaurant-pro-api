package com.restaurantpos.orders.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;

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
 * Order entity representing a customer order.
 * Orders belong to a specific site and can be associated with a table or customer.
 * 
 * Requirements: 5.1, 5.9
 */
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "site_id", nullable = false)
    private UUID siteId;
    
    @Column(name = "table_id")
    private UUID tableId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected Order() {
        // JPA requires a no-arg constructor
    }
    
    public Order(UUID tenantId, UUID siteId, UUID tableId, UUID customerId, OrderType orderType) {
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.tableId = tableId;
        this.customerId = customerId;
        this.orderType = orderType;
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
    
    public UUID getTableId() {
        return tableId;
    }
    
    public UUID getCustomerId() {
        return customerId;
    }
    
    public OrderType getOrderType() {
        return orderType;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
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
    
    public UUID getUpdatedBy() {
        return updatedBy;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    // Setters
    
    public void setTableId(UUID tableId) {
        this.tableId = tableId;
        this.updatedAt = Instant.now();
    }
    
    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.updatedAt = Instant.now();
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the order is open and can be modified.
     */
    public boolean isOpen() {
        return status == OrderStatus.OPEN;
    }
    
    /**
     * Checks if the order has been confirmed.
     */
    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }
    
    /**
     * Checks if the order has been paid.
     */
    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }
    
    /**
     * Checks if the order is closed.
     */
    public boolean isClosed() {
        return status == OrderStatus.CLOSED;
    }
    
    /**
     * Checks if the order has been voided.
     */
    public boolean isVoided() {
        return status == OrderStatus.VOIDED;
    }
}
