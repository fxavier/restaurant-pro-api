package com.restaurantpos.diningroom.entity;

import com.restaurantpos.diningroom.model.EntityType;
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
 * BlacklistEntry entity representing a blocked table or payment card.
 * Blacklisted entities cannot be used for operations.
 * 
 * Requirements: 3.6
 */
@Entity
@Table(name = "blacklist_entries")
public class BlacklistEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;
    
    @Column(name = "entity_value", nullable = false, length = 255)
    private String entityValue;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt = Instant.now();
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    protected BlacklistEntry() {
        // JPA requires a no-arg constructor
    }
    
    public BlacklistEntry(UUID tenantId, EntityType entityType, String entityValue, String reason, UUID createdBy) {
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.entityValue = entityValue;
        this.reason = reason;
        this.createdBy = createdBy;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public String getEntityValue() {
        return entityValue;
    }
    
    public String getReason() {
        return reason;
    }
    
    public Instant getBlockedAt() {
        return blockedAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    // Setters
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
