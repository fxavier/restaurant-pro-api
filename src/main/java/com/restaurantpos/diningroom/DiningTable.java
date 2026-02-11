package com.restaurantpos.diningroom;

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
import jakarta.persistence.Version;

/**
 * DiningTable entity representing a table in a restaurant dining room.
 * Tables belong to a specific site within a tenant and have a status
 * indicating their current availability.
 * 
 * Requirements: 3.2
 */
@Entity
@Table(name = "dining_tables")
public class DiningTable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "site_id", nullable = false)
    private UUID siteId;
    
    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;
    
    @Column(length = 100)
    private String area;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;
    
    @Column
    private Integer capacity;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected DiningTable() {
        // JPA requires a no-arg constructor
    }
    
    public DiningTable(UUID tenantId, UUID siteId, String tableNumber, String area, Integer capacity) {
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.tableNumber = tableNumber;
        this.area = area;
        this.capacity = capacity;
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
    
    public String getTableNumber() {
        return tableNumber;
    }
    
    public String getArea() {
        return area;
    }
    
    public TableStatus getStatus() {
        return status;
    }
    
    public Integer getCapacity() {
        return capacity;
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
    
    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
        this.updatedAt = Instant.now();
    }
    
    public void setArea(String area) {
        this.area = area;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(TableStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the table is available for seating.
     */
    public boolean isAvailable() {
        return status == TableStatus.AVAILABLE;
    }
    
    /**
     * Checks if the table is currently occupied.
     */
    public boolean isOccupied() {
        return status == TableStatus.OCCUPIED;
    }
    
    /**
     * Checks if the table is blocked.
     */
    public boolean isBlocked() {
        return status == TableStatus.BLOCKED;
    }
}
