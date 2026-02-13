package com.restaurantpos.diningroom.model;

/**
 * Enumeration of entity types that can be blacklisted.
 * 
 * Requirement: 3.6
 */
public enum EntityType {
    /**
     * A dining table.
     */
    TABLE,
    
    /**
     * A payment card (last four digits).
     */
    CARD
}
