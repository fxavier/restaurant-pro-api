package com.restaurantpos.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.restaurantpos.orders.model.DiscountType;

/**
 * DTO for discount application details.
 * 
 * Requirements: 5.7
 */
public record DiscountDetails(
    UUID orderLineId,  // null for order-level discount
    DiscountType type,
    BigDecimal amount,
    String reason
) {
    public DiscountDetails {
        if (type == null) {
            throw new IllegalArgumentException("Discount type is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount amount must be positive");
        }
    }
    
    public boolean isOrderDiscount() {
        return orderLineId == null;
    }
    
    public boolean isLineDiscount() {
        return orderLineId != null;
    }
}
