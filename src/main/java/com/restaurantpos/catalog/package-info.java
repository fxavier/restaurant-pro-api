/**
 * Catalog Module
 * 
 * Responsibilities:
 * - Menu structure management (families, subfamilies, items)
 * - Item availability and pricing
 * - Quick pages for frequently ordered items
 * 
 * Public API: catalog.api package
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = "identityaccess"
)
package com.restaurantpos.catalog;
