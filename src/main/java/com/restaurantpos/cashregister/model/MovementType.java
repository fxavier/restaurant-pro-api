package com.restaurantpos.cashregister.model;

/**
 * Type of cash movement.
 * 
 * Requirements: 10.3, 10.4
 */
public enum MovementType {
    SALE,
    REFUND,
    DEPOSIT,
    WITHDRAWAL,
    OPENING,
    CLOSING
}
