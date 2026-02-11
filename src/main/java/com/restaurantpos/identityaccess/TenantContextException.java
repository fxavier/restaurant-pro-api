package com.restaurantpos.identityaccess;

/**
 * Exception thrown when tenant context is required but not set.
 * This typically indicates a security issue where a repository operation
 * is attempted without proper tenant isolation.
 * 
 * Requirements: 1.4, 1.7
 */
public class TenantContextException extends RuntimeException {
    
    public TenantContextException(String message) {
        super(message);
    }
    
    public TenantContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
