package com.restaurantpos.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for creating or updating a family.
 * 
 * Requirements: 4.1
 */
public record FamilyDetails(
    @NotBlank(message = "Family name is required")
    String name,
    
    @PositiveOrZero(message = "Display order must be zero or positive")
    Integer displayOrder
) {}
