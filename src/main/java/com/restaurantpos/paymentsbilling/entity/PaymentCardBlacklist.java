package com.restaurantpos.paymentsbilling.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PaymentCardBlacklist entity representing a blocked payment card.
 * Cards in the blacklist cannot be used for payment transactions.
 * 
 * Requirements: 7.10
 */
@Entity
@Table(name = "payment_card_blacklist")
public class PaymentCardBlacklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt = Instant.now();
    
    protected PaymentCardBlacklist() {
        // JPA requires a no-arg constructor
    }
    
    public PaymentCardBlacklist(UUID tenantId, String cardLastFour, String reason) {
        this.tenantId = tenantId;
        this.cardLastFour = cardLastFour;
        this.reason = reason;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public String getCardLastFour() {
        return cardLastFour;
    }
    
    public String getReason() {
        return reason;
    }
    
    public Instant getBlockedAt() {
        return blockedAt;
    }
    
    // Setters
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
