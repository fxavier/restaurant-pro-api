package com.restaurantpos.customers.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Customer entity representing a customer for delivery orders.
 * Customers belong to a specific tenant and contain contact information
 * for order delivery and history tracking.
 * 
 * Requirements: 8.1, 8.5
 */
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, length = 20)
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Customer() {
        // JPA requires a no-arg constructor
    }
    
    public Customer(UUID tenantId, String name, String phone, String address, String deliveryNotes) {
        this.tenantId = tenantId;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.deliveryNotes = deliveryNotes;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getDeliveryNotes() {
        return deliveryNotes;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = Instant.now();
    }
    
    public void setAddress(String address) {
        this.address = address;
        this.updatedAt = Instant.now();
    }
    
    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
        this.updatedAt = Instant.now();
    }
}
