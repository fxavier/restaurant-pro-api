package com.restaurantpos.identityaccess;

/**
 * User roles in the system.
 * Each role has different permissions for accessing system features.
 * 
 * Requirement: 2.4
 */
public enum Role {
    /**
     * System administrator with full access to all features.
     */
    ADMIN,
    
    /**
     * Restaurant manager with access to management features.
     */
    MANAGER,
    
    /**
     * Cashier with access to payment and billing features.
     */
    CASHIER,
    
    /**
     * Waiter with access to table and order management.
     */
    WAITER,
    
    /**
     * Kitchen staff with access to order viewing and printing.
     */
    KITCHEN_STAFF
}
