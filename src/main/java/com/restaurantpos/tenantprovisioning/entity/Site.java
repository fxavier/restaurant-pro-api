package com.restaurantpos.tenantprovisioning.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Site entity representing a physical restaurant location within a tenant.
 * A tenant can have multiple sites (multi-location support).
 * 
 * Requirements: 1.1, 1.8
 */
@Entity
@Table(name = "sites")
public class Site {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 50)
    private String timezone;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String settings;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Site() {
        // JPA requires a no-arg constructor
    }
    
    public Site(UUID tenantId, String name, String address, String timezone) {
        this.tenantId = tenantId;
        this.name = name;
        this.address = address;
        this.timezone = timezone;
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
    
    public String getAddress() {
        return address;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public String getSettings() {
        return settings;
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
    
    public void setAddress(String address) {
        this.address = address;
        this.updatedAt = Instant.now();
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.updatedAt = Instant.now();
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
        this.updatedAt = Instant.now();
    }
}
