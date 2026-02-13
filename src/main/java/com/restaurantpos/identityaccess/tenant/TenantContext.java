package com.restaurantpos.identityaccess.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the current tenant context.
 * This class provides a way to store and retrieve the tenant ID for the current request.
 * 
 * Requirements: 1.4, 1.7
 */
public class TenantContext {
    
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    
    private TenantContext() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Sets the tenant ID for the current thread.
     * 
     * @param tenantId the tenant ID to set
     */
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    /**
     * Gets the tenant ID for the current thread.
     * 
     * @return the tenant ID, or null if not set
     */
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * Clears the tenant ID for the current thread.
     * This should be called at the end of each request to prevent memory leaks.
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
