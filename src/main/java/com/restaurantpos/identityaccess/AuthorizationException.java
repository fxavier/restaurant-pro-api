package com.restaurantpos.identityaccess;

/**
 * Exception thrown when a user does not have the required permission
 * to perform an operation.
 * 
 * Requirements: 2.5
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
