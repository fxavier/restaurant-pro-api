package com.restaurantpos.identityaccess;

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
 * User entity representing a system user.
 * Users belong to a tenant and have a role that determines their permissions.
 * 
 * Requirement: 2.1
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(length = 255)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Version
    @Column(nullable = false)
    private Integer version = 0;
    
    protected User() {
        // JPA requires a no-arg constructor
    }
    
    public User(UUID tenantId, String username, String passwordHash, String email, Role role) {
        this.tenantId = tenantId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getEmail() {
        return email;
    }
    
    public Role getRole() {
        return role;
    }
    
    public UserStatus getStatus() {
        return status;
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
    
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
    }
    
    public void setRole(Role role) {
        this.role = role;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Checks if the user account is active.
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
