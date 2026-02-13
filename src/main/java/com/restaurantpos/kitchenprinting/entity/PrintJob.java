package com.restaurantpos.kitchenprinting.entity;

import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.kitchenprinting.model.PrintJobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PrintJob entity representing a task to print order items to a specific printer.
 * Print jobs are created when orders are confirmed and routed based on printer/zone assignments.
 * 
 * Requirements: 6.1, 6.7
 */
@Entity
@Table(name = "print_jobs")
public class PrintJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "printer_id", nullable = false)
    private UUID printerId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrintJobStatus status = PrintJobStatus.PENDING;
    
    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    protected PrintJob() {
        // JPA requires a no-arg constructor
    }
    
    public PrintJob(UUID tenantId, UUID orderId, UUID printerId, String content, String dedupeKey) {
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.printerId = printerId;
        this.content = content;
        this.dedupeKey = dedupeKey;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public UUID getPrinterId() {
        return printerId;
    }
    
    public String getContent() {
        return content;
    }
    
    public PrintJobStatus getStatus() {
        return status;
    }
    
    public String getDedupeKey() {
        return dedupeKey;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    
    public void setStatus(PrintJobStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the print job is pending.
     */
    public boolean isPending() {
        return status == PrintJobStatus.PENDING;
    }
    
    /**
     * Checks if the print job has been printed.
     */
    public boolean isPrinted() {
        return status == PrintJobStatus.PRINTED;
    }
    
    /**
     * Checks if the print job failed.
     */
    public boolean isFailed() {
        return status == PrintJobStatus.FAILED;
    }
    
    /**
     * Checks if the print job was skipped.
     */
    public boolean isSkipped() {
        return status == PrintJobStatus.SKIPPED;
    }
}
