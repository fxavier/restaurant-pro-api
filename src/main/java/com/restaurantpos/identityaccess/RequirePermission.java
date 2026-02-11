package com.restaurantpos.identityaccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level permission checking.
 * When applied to a method, the method will only execute if the current user
 * has the specified permission.
 * 
 * Usage:
 * <pre>
 * {@code
 * @RequirePermission(Permission.VOID_AFTER_SUBTOTAL)
 * public void voidOrderLine(UUID orderLineId) {
 *     // Method implementation
 * }
 * }
 * </pre>
 * 
 * Requirements: 2.5
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * The permission required to execute the annotated method.
     * 
     * @return the required permission
     */
    Permission value();
}
