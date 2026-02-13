package com.restaurantpos.kitchenprinting.entity;

import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.kitchenprinting.model.PrinterStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Printer entity representing a physical printing device in a restaurant.
 * Printers belong to a specific site and can be assigned to zones for routing print jobs.
 * 
 * Requirements: 6.1, 6.3
 */
@Entity
@Table(name = "printers")
public class Printer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "site_id", nullable = false)
    private UUID siteId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(length = 100)
    private String zone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrinterStatus status = PrinterStatus.NORMAL;
    
    @Column(name = "redirect_to_printer_id")
    private UUID redirectToPrinterId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected Printer() {
        // JPA requires a no-arg constructor
    }
    
    public Printer(UUID tenantId, UUID siteId, String name, String ipAddress, String zone) {
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.name = name;
        this.ipAddress = ipAddress;
        this.zone = zone;
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
    
    public String getName() {
        return name;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getZone() {
        return zone;
    }
    
    public PrinterStatus getStatus() {
        return status;
    }
    
    public UUID getRedirectToPrinterId() {
        return redirectToPrinterId;
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
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        this.updatedAt = Instant.now();
    }
    
    public void setZone(String zone) {
        this.zone = zone;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(PrinterStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setRedirectToPrinterId(UUID redirectToPrinterId) {
        this.redirectToPrinterId = redirectToPrinterId;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the printer is in normal operating mode.
     */
    public boolean isNormal() {
        return status == PrinterStatus.NORMAL;
    }
    
    /**
     * Checks if the printer is in wait mode.
     */
    public boolean isWait() {
        return status == PrinterStatus.WAIT;
    }
    
    /**
     * Checks if the printer is in ignore mode.
     */
    public boolean isIgnore() {
        return status == PrinterStatus.IGNORE;
    }
    
    /**
     * Checks if the printer is in redirect mode.
     */
    public boolean isRedirect() {
        return status == PrinterStatus.REDIRECT;
    }
}
