package com.restaurantpos.catalog.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Subfamily entity representing a second-level menu category.
 * Subfamilies belong to a family and further organize menu items.
 * 
 * Requirements: 4.1, 4.2
 */
@Entity
@Table(name = "subfamilies")
public class Subfamily {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Subfamily() {
        // JPA requires a no-arg constructor
    }
    
    public Subfamily(UUID tenantId, UUID familyId, String name, Integer displayOrder) {
        this.tenantId = tenantId;
        this.familyId = familyId;
        this.name = name;
        this.displayOrder = displayOrder;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getFamilyId() {
        return familyId;
    }
    
    public String getName() {
        return name;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public Boolean getActive() {
        return active;
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
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = Instant.now();
    }
    
    public void setActive(Boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the subfamily is active.
     */
    public boolean isActive() {
        return active != null && active;
    }
}
