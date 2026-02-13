package com.restaurantpos.kitchenprinting.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.restaurantpos.kitchenprinting.service.PrintingService;
import com.restaurantpos.orders.event.OrderConfirmed;

/**
 * Event listener for OrderConfirmed events from the orders module.
 * Triggers print job creation when an order is confirmed.
 * 
 * Requirements: 6.1
 */
@Component
public class OrderConfirmedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmedEventListener.class);
    
    private final PrintingService printingService;
    
    public OrderConfirmedEventListener(PrintingService printingService) {
        this.printingService = printingService;
    }
    
    /**
     * Handles OrderConfirmed events by creating print jobs for the confirmed order.
     * 
     * @param event the OrderConfirmed event
     */
    @ApplicationModuleListener
    public void onOrderConfirmed(OrderConfirmed event) {
        logger.info("Received OrderConfirmed event for order: {} (tenant: {})", 
            event.orderId(), event.tenantId());
        
        try {
            printingService.createPrintJobs(event.orderId());
            logger.info("Successfully created print jobs for order: {}", event.orderId());
        } catch (Exception e) {
            logger.error("Failed to create print jobs for order: {}", event.orderId(), e);
            throw e; // Re-throw to ensure transaction rollback if needed
        }
    }
}
