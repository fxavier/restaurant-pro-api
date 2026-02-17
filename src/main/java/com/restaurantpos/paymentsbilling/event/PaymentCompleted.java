package com.restaurantpos.paymentsbilling.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.restaurantpos.paymentsbilling.model.PaymentMethod;

/**
 * Domain event emitted when a payment is successfully completed.
 * This event triggers cash movement creation in the cash-register module.
 * 
 * Requirements: 7.5, 10.4
 */
public record PaymentCompleted(
    UUID paymentId,
    UUID orderId,
    UUID tenantId,
    UUID siteId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    Instant completedAt
) {
    public PaymentCompleted {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (siteId == null) {
            throw new IllegalArgumentException("Site ID is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (completedAt == null) {
            completedAt = Instant.now();
        }
    }
}
