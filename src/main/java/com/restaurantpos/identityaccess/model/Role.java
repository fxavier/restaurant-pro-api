package com.restaurantpos.identityaccess.model;

import java.util.Set;

/**
 * User roles in the system.
 * Each role has different permissions for accessing system features.
 * 
 * Requirements: 2.4, 2.6
 */
public enum Role {
    /**
     * Super administrator with system-wide access across all tenants.
     * Can create tenants and has all permissions in the system.
     * Not bound to a specific tenant.
     */
    SUPER_ADMIN(Set.of(
        Permission.VOID_AFTER_SUBTOTAL,
        Permission.APPLY_DISCOUNT,
        Permission.REPRINT_DOCUMENT,
        Permission.REDIRECT_PRINTER,
        Permission.CLOSE_CASH,
        Permission.VOID_INVOICE
    )),
    
    /**
     * System administrator with full access to all features within a tenant.
     * Has all permissions in the system for their tenant.
     */
    ADMIN(Set.of(
        Permission.VOID_AFTER_SUBTOTAL,
        Permission.APPLY_DISCOUNT,
        Permission.REPRINT_DOCUMENT,
        Permission.REDIRECT_PRINTER,
        Permission.CLOSE_CASH,
        Permission.VOID_INVOICE
    )),
    
    /**
     * Restaurant manager with access to management features.
     * Can perform most sensitive operations except system administration.
     */
    MANAGER(Set.of(
        Permission.VOID_AFTER_SUBTOTAL,
        Permission.APPLY_DISCOUNT,
        Permission.REPRINT_DOCUMENT,
        Permission.REDIRECT_PRINTER,
        Permission.CLOSE_CASH,
        Permission.VOID_INVOICE
    )),
    
    /**
     * Cashier with access to payment and billing features.
     * Can handle payments, close cash sessions, and reprint documents.
     */
    CASHIER(Set.of(
        Permission.REPRINT_DOCUMENT,
        Permission.CLOSE_CASH
    )),
    
    /**
     * Waiter with access to table and order management.
     * Limited permissions for basic order operations.
     */
    WAITER(Set.of()),
    
    /**
     * Kitchen staff with access to order viewing and printing.
     * Can redirect printers for kitchen operations.
     */
    KITCHEN_STAFF(Set.of(
        Permission.REDIRECT_PRINTER
    ));
    
    private final Set<Permission> permissions;
    
    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * Get all permissions assigned to this role.
     * 
     * @return immutable set of permissions
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    /**
     * Check if this role has a specific permission.
     * 
     * @param permission the permission to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Check if this role is a super admin with system-wide access.
     * 
     * @return true if the role is SUPER_ADMIN, false otherwise
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
