package com.restaurantpos.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating an item.
 * 
 * Requirements: 4.1
 */
public record ItemDetails(
    @NotNull(message = "Subfamily ID is required")
    UUID subfamilyId,
    
    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    String name,
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,
    
    @NotNull(message = "Base price is required")
    @PositiveOrZero(message = "Base price must be zero or positive")
    BigDecimal basePrice,
    
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    String imageUrl
) {}
