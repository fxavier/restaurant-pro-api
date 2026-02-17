package com.restaurantpos.common.exception;

/**
 * Exception thrown when a request conflicts with the current state of the resource.
 * Maps to HTTP 409 Conflict.
 * 
 * Examples: duplicate key violations, optimistic locking conflicts.
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
