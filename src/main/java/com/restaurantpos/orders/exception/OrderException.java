package com.restaurantpos.orders.exception;

/**
 * Base exception for order-related errors.
 */
public class OrderException extends RuntimeException {
    
    public OrderException(String message) {
        super(message);
    }
    
    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
