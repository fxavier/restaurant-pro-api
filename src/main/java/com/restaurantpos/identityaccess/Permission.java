package com.restaurantpos.identityaccess;

/**
 * System permissions that control access to sensitive operations.
 * Permissions are assigned to roles to implement role-based access control (RBAC).
 * 
 * Requirements: 2.4, 2.6
 */
public enum Permission {
    /**
     * Permission to void order lines after subtotal has been printed.
     * This is a sensitive operation that requires manager approval.
     */
    VOID_AFTER_SUBTOTAL,
    
    /**
     * Permission to apply discounts to orders or order lines.
     * Discounts affect revenue and require authorization.
     */
    APPLY_DISCOUNT,
    
    /**
     * Permission to reprint fiscal documents (invoices, receipts).
     * Reprinting documents must be audited for compliance.
     */
    REPRINT_DOCUMENT,
    
    /**
     * Permission to redirect printer output to different printers.
     * This affects kitchen operations and order routing.
     */
    REDIRECT_PRINTER,
    
    /**
     * Permission to close cash sessions and perform cash register closings.
     * This is a critical financial operation requiring authorization.
     */
    CLOSE_CASH,
    
    /**
     * Permission to void fiscal invoices.
     * Voiding invoices has legal and tax implications.
     */
    VOID_INVOICE
}
