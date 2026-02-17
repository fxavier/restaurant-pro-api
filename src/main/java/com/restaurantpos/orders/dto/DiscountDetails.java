package com.restaurantpos.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.restaurantpos.orders.model.DiscountType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO for discount application details.
 * 
 * Requirements: 5.7
 */
public record DiscountDetails(
    UUID orderLineId,  // null for order-level discount
    
    @NotNull(message = "Discount type is required")
    DiscountType type,
    
    @NotNull(message = "Discount amount is required")
    @Positive(message = "Discount amount must be positive")
    BigDecimal amount,
    
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason
) {
    public boolean isOrderDiscount() {
        return orderLineId == null;
    }
    
    public boolean isLineDiscount() {
        return orderLineId != null;
    }
}
