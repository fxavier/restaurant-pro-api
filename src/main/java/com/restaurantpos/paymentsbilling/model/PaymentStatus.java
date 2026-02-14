package com.restaurantpos.paymentsbilling.model;

/**
 * Payment status enumeration.
 * Tracks the lifecycle of a payment transaction.
 * 
 * Requirements: 7.1
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    VOIDED
}
