package com.restaurantpos.orders.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Consumption entity representing the confirmed state of an order line.
 * Consumptions are created when an order is confirmed and track sales for reporting.
 * 
 * Requirements: 5.4
 */
@Entity
@Table(name = "consumptions")
public class Consumption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "order_line_id", nullable = false)
    private UUID orderLineId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;
    
    @Column(name = "voided_at")
    private Instant voidedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    protected Consumption() {
        // JPA requires a no-arg constructor
    }
    
    public Consumption(UUID tenantId, UUID orderLineId, Integer quantity, Instant confirmedAt) {
        this.tenantId = tenantId;
        this.orderLineId = orderLineId;
        this.quantity = quantity;
        this.confirmedAt = confirmedAt;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getOrderLineId() {
        return orderLineId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public Instant getConfirmedAt() {
        return confirmedAt;
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
     * Checks if this consumption has been voided.
     */
    public boolean isVoided() {
        return voidedAt != null;
    }
}
