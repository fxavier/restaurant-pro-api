package com.restaurantpos.tenantprovisioning;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tenant entity representing a restaurant organization using the system.
 * Each tenant has isolated data and configuration.
 * 
 * Requirements: 1.1, 1.8
 */
@Entity
@Table(name = "tenants")
public class Tenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Tenant() {
        // JPA requires a no-arg constructor
    }
    
    public Tenant(String name, String subscriptionPlan) {
        this.name = name;
        this.subscriptionPlan = subscriptionPlan;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }
    
    public TenantStatus getStatus() {
        return status;
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
    
    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(TenantStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the tenant is active.
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }
}
