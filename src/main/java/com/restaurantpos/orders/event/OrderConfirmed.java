package com.restaurantpos.orders.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when an order is confirmed.
 * This event triggers print job creation in the kitchen-printing module.
 * 
 * Requirements: 5.3, 6.1
 */
public record OrderConfirmed(
    UUID orderId,
    UUID tenantId,
    UUID siteId,
    Instant confirmedAt
) {
    public OrderConfirmed {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (siteId == null) {
            throw new IllegalArgumentException("Site ID is required");
        }
        if (confirmedAt == null) {
            confirmedAt = Instant.now();
        }
    }
}
