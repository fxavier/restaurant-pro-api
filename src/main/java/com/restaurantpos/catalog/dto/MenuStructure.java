package com.restaurantpos.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the complete menu hierarchy.
 * 
 * Requirements: 4.1
 */
public record MenuStructure(
    List<FamilyNode> families
) {
    public record FamilyNode(
        UUID id,
        String name,
        Integer displayOrder,
        Boolean active,
        List<SubfamilyNode> subfamilies
    ) {}
    
    public record SubfamilyNode(
        UUID id,
        String name,
        Integer displayOrder,
        Boolean active,
        List<ItemNode> items
    ) {}
    
    public record ItemNode(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        Boolean available,
        String imageUrl
    ) {}
}
