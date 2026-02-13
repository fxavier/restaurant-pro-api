/**
 * Kitchen and Printing Module
 * 
 * Responsibilities:
 * - Print job generation for confirmed orders
 * - Printer management and routing logic
 * - Printer state management (NORMAL, WAIT, IGNORE, REDIRECT)
 * - Listens to OrderConfirmed events from orders module
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Kitchen and Printing",
    allowedDependencies = {"identityaccess::api", "orders::event"}
)
package com.restaurantpos.kitchenprinting;
