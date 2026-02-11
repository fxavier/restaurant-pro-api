package com.restaurantpos.identityaccess;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token entity for managing JWT refresh tokens.
 * Stores hashed tokens to prevent token theft.
 * 
 * Requirement: 2.2, 2.3, 2.8
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(nullable = false)
    private boolean revoked = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    protected RefreshToken() {
        // JPA requires a no-arg constructor
    }
    
    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getTokenHash() {
        return tokenHash;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public boolean isRevoked() {
        return revoked;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    // Methods
    
    public void revoke() {
        this.revoked = true;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
