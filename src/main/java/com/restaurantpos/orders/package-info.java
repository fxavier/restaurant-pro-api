/**
 * Orders Module
 * 
 * Responsibilities:
 * - Order creation and modification
 * - Order confirmation and consumption tracking
 * - Void operations and discount management
 * - Order transfer between tables
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Orders",
    allowedDependencies = {"identityaccess::api", "catalog::api"}
)
package com.restaurantpos.orders;
