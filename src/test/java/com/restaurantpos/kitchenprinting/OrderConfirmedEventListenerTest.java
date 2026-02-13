package com.restaurantpos.kitchenprinting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.kitchenprinting.listener.OrderConfirmedEventListener;
import com.restaurantpos.kitchenprinting.service.PrintingService;
import com.restaurantpos.orders.event.OrderConfirmed;

/**
 * Unit tests for OrderConfirmedEventListener.
 * Tests event handling and print job creation triggering.
 */
@ExtendWith(MockitoExtension.class)
class OrderConfirmedEventListenerTest {
    
    @Mock
    private PrintingService printingService;
    
    @InjectMocks
    private OrderConfirmedEventListener eventListener;
    
    private UUID orderId;
    private UUID tenantId;
    private UUID siteId;
    
    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
    }
    
    @Test
    void onOrderConfirmed_shouldCallCreatePrintJobs() {
        // Given
        OrderConfirmed event = new OrderConfirmed(orderId, tenantId, siteId, Instant.now());
        when(printingService.createPrintJobs(orderId)).thenReturn(List.of());
        
        // When
        eventListener.onOrderConfirmed(event);
        
        // Then
        verify(printingService, times(1)).createPrintJobs(orderId);
    }
    
    @Test
    void onOrderConfirmed_shouldPropagateExceptions() {
        // Given
        OrderConfirmed event = new OrderConfirmed(orderId, tenantId, siteId, Instant.now());
        RuntimeException exception = new RuntimeException("Print job creation failed");
        doThrow(exception).when(printingService).createPrintJobs(orderId);
        
        // When/Then
        assertThrows(RuntimeException.class, () -> eventListener.onOrderConfirmed(event));
        verify(printingService, times(1)).createPrintJobs(orderId);
    }
    
    @Test
    void onOrderConfirmed_shouldHandleMultipleEvents() {
        // Given
        OrderConfirmed event1 = new OrderConfirmed(orderId, tenantId, siteId, Instant.now());
        UUID orderId2 = UUID.randomUUID();
        OrderConfirmed event2 = new OrderConfirmed(orderId2, tenantId, siteId, Instant.now());
        
        when(printingService.createPrintJobs(any(UUID.class))).thenReturn(List.of());
        
        // When
        eventListener.onOrderConfirmed(event1);
        eventListener.onOrderConfirmed(event2);
        
        // Then
        verify(printingService, times(1)).createPrintJobs(orderId);
        verify(printingService, times(1)).createPrintJobs(orderId2);
    }
}
