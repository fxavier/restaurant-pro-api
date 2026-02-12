package com.restaurantpos.identityaccess.tenant;

import java.util.UUID;

/**
 * Interface for authentication principals that carry tenant information.
 * Implementations of this interface can provide tenant ID to the TenantContextFilter.
 * 
 * Requirements: 1.4, 1.7
 */
public interface TenantAware {
    
    /**
     * Returns the tenant ID associated with this principal.
     * 
     * @return the tenant ID, or null if not available
     */
    UUID getTenantId();
}
