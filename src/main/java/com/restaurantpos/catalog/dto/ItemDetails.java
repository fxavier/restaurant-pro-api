package com.restaurantpos.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating or updating an item.
 * 
 * Requirements: 4.1
 */
public record ItemDetails(
    UUID subfamilyId,
    String name,
    String description,
    BigDecimal basePrice,
    String imageUrl
) {
    public ItemDetails {
        if (subfamilyId == null) {
            throw new IllegalArgumentException("Subfamily ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be null or blank");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price must be non-negative");
        }
    }
}
