package com.restaurantpos.diningroom.model;

/**
 * Enumeration of possible table states.
 * 
 * Requirement: 3.2
 */
public enum TableStatus {
    /**
     * Table is available for seating.
     */
    AVAILABLE,
    
    /**
     * Table is currently occupied with customers.
     */
    OCCUPIED,
    
    /**
     * Table has a reservation.
     */
    RESERVED,
    
    /**
     * Table is blocked and cannot be used.
     */
    BLOCKED
}
