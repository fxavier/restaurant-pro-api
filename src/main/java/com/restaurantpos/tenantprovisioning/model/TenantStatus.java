package com.restaurantpos.tenantprovisioning.model;

/**
 * Status of a tenant in the system.
 * 
 * Requirements: 1.1
 */
public enum TenantStatus {
    /**
     * Tenant is active and can use the system.
     */
    ACTIVE,
    
    /**
     * Tenant is temporarily suspended (e.g., payment issues).
     */
    SUSPENDED,
    
    /**
     * Tenant subscription has been cancelled.
     */
    CANCELLED
}
