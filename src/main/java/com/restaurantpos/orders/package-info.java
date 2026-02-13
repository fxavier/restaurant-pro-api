/**
 * Orders Module
 * 
 * Responsibilities:
 * - Order creation and modification
 * - Order confirmation and consumption tracking
 * - Void operations and discount management
 * - Order transfer between tables
 * 
 * Exposed packages:
 * - event: Domain events (OrderConfirmed) for cross-module communication
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Orders",
    allowedDependencies = {"identityaccess::api", "catalog::api"}
)
@org.springframework.modulith.NamedInterface(name = "event", value = "event")
package com.restaurantpos.orders;
