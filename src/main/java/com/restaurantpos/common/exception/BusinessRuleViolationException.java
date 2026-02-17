package com.restaurantpos.common.exception;

/**
 * Exception thrown when a business rule is violated.
 * Maps to HTTP 422 Unprocessable Entity.
 * 
 * Examples: attempting to void an already paid order, insufficient permissions for operation.
 */
public class BusinessRuleViolationException extends RuntimeException {
    
    public BusinessRuleViolationException(String message) {
        super(message);
    }
    
    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
