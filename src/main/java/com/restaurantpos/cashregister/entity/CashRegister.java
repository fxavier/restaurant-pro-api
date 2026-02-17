package com.restaurantpos.cashregister.entity;

import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.cashregister.model.CashRegisterStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CashRegister entity representing a physical or logical register device.
 * 
 * Requirements: 10.1
 */
@Entity
@Table(name = "cash_registers")
public class CashRegister {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "site_id", nullable = false)
    private UUID siteId;
    
    @Column(name = "register_number", nullable = false, length = 20)
    private String registerNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashRegisterStatus status = CashRegisterStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected CashRegister() {
        // JPA requires a no-arg constructor
    }
    
    public CashRegister(UUID tenantId, UUID siteId, String registerNumber) {
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.registerNumber = registerNumber;
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
    
    public String getRegisterNumber() {
        return registerNumber;
    }
    
    public CashRegisterStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    
    public void setStatus(CashRegisterStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the register is active.
     */
    public boolean isActive() {
        return status == CashRegisterStatus.ACTIVE;
    }
    
    /**
     * Checks if the register is inactive.
     */
    public boolean isInactive() {
        return status == CashRegisterStatus.INACTIVE;
    }
}
