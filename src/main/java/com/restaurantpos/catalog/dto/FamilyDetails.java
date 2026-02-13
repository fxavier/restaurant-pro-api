package com.restaurantpos.catalog.dto;

/**
 * DTO for creating or updating a family.
 * 
 * Requirements: 4.1
 */
public record FamilyDetails(
    String name,
    Integer displayOrder
) {
    public FamilyDetails {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Family name cannot be null or blank");
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
