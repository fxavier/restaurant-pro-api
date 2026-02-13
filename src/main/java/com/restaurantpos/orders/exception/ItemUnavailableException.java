package com.restaurantpos.orders.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to order an unavailable item.
 * 
 * Requirements: 4.3
 */
public class ItemUnavailableException extends OrderException {
    
    private final UUID itemId;
    
    public ItemUnavailableException(UUID itemId, String itemName) {
        super(String.format("Item '%s' (ID: %s) is not available for ordering", itemName, itemId));
        this.itemId = itemId;
    }
    
    public UUID getItemId() {
        return itemId;
    }
}
