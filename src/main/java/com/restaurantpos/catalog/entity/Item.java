package com.restaurantpos.catalog.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Item entity representing a menu item that can be ordered.
 * Items belong to a subfamily and have pricing and availability information.
 * 
 * Requirements: 4.1, 4.2
 */
@Entity
@Table(name = "items")
public class Item {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "subfamily_id", nullable = false)
    private UUID subfamilyId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Column(nullable = false)
    private Boolean available = true;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Item() {
        // JPA requires a no-arg constructor
    }
    
    public Item(UUID tenantId, UUID subfamilyId, String name, String description, BigDecimal basePrice) {
        this.tenantId = tenantId;
        this.subfamilyId = subfamilyId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getSubfamilyId() {
        return subfamilyId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public Boolean getAvailable() {
        return available;
    }
    
    public String getImageUrl() {
        return imageUrl;
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
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
        this.updatedAt = Instant.now();
    }
    
    public void setAvailable(Boolean available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the item is available for ordering.
     */
    public boolean isAvailable() {
        return available != null && available;
    }
}
