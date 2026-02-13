package com.restaurantpos.catalog.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Catalog module.
 * This interface is exposed to other modules for catalog operations.
 * 
 * Requirements: 4.1, 4.3
 */
public interface CatalogService {
    
    /**
     * Gets an item by ID with tenant isolation.
     * 
     * @param itemId the item ID
     * @param tenantId the tenant ID
     * @return the item info if found
     */
    Optional<ItemInfo> getItem(UUID itemId, UUID tenantId);
    
    /**
     * Item information DTO for cross-module communication.
     */
    record ItemInfo(
        UUID id,
        UUID tenantId,
        String name,
        BigDecimal basePrice,
        boolean available
    ) {}
}
