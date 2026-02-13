package com.restaurantpos.identityaccess.api;

import java.util.UUID;

/**
 * Public API for authorization and permission checks.
 * This interface is exposed to other modules.
 * 
 * Requirements: 2.5
 */
public interface AuthorizationApi {
    
    /**
     * Checks if a user has a specific permission.
     * 
     * @param userId the user ID to check
     * @param permission the permission to verify
     * @return true if the user has the permission, false otherwise
     */
    boolean hasPermission(UUID userId, PermissionType permission);
    
    /**
     * Requires that the current user has a specific permission.
     * Throws an exception if the permission is not granted.
     * 
     * @param userId the user ID to check
     * @param permission the required permission
     * @throws RuntimeException if the user does not have the permission
     */
    void requirePermission(UUID userId, PermissionType permission);
    
    /**
     * Gets the current tenant ID from the tenant context.
     * 
     * @return the current tenant ID
     * @throws RuntimeException if no tenant context is set
     */
    UUID getTenantContext();
    
    /**
     * Permission types for cross-module communication.
     */
    enum PermissionType {
        VOID_AFTER_SUBTOTAL,
        APPLY_DISCOUNT,
        REPRINT_DOCUMENT,
        REDIRECT_PRINTER,
        CLOSE_CASH,
        VOID_INVOICE
    }
}
