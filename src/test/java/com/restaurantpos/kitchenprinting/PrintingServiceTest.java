package com.restaurantpos.kitchenprinting;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.exception.AuthorizationException;
import com.restaurantpos.kitchenprinting.entity.PrintJob;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrintJobStatus;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;
import com.restaurantpos.kitchenprinting.repository.PrintJobRepository;
import com.restaurantpos.kitchenprinting.repository.PrinterRepository;
import com.restaurantpos.kitchenprinting.service.PrintingService;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.repository.OrderLineRepository;
import com.restaurantpos.orders.repository.OrderRepository;

/**
 * Unit tests for PrintingService.
 * Tests print job creation, reprinting, and processing logic.
 */
@ExtendWith(MockitoExtension.class)
class PrintingServiceTest {
    
    @Mock
    private PrintJobRepository printJobRepository;
    
    @Mock
    private PrinterRepository printerRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderLineRepository orderLineRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private AuthorizationApi authorizationApi;
    
    @InjectMocks
    private PrintingService printingService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID orderId;
    private UUID printerId;
    private UUID itemId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        printerId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }
    
    @Test
    void createPrintJobs_shouldCreateJobsForEachOrderLine() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        OrderLine orderLine = new OrderLine(orderId, itemId, 2, BigDecimal.valueOf(10.00));
        Item item = new Item(tenantId, UUID.randomUUID(), "Pizza", "Delicious pizza", BigDecimal.valueOf(10.00));
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of(orderLine));
        when(printerRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(List.of(printer));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(printJobRepository.existsByTenantIdAndDedupeKey(eq(tenantId), anyString())).thenReturn(false);
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<PrintJob> printJobs = printingService.createPrintJobs(orderId);
        
        // Then
        assertNotNull(printJobs);
        assertEquals(1, printJobs.size());
        verify(printJobRepository, times(1)).save(any(PrintJob.class));
    }
    
    @Test
    void createPrintJobs_shouldNotCreateDuplicateJobs() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        OrderLine orderLine = new OrderLine(orderId, itemId, 2, BigDecimal.valueOf(10.00));
        Item item = new Item(tenantId, UUID.randomUUID(), "Pizza", "Delicious pizza", BigDecimal.valueOf(10.00));
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of(orderLine));
        when(printerRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(List.of(printer));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(printJobRepository.existsByTenantIdAndDedupeKey(eq(tenantId), anyString())).thenReturn(true);
        
        // When
        List<PrintJob> printJobs = printingService.createPrintJobs(orderId);
        
        // Then
        assertNotNull(printJobs);
        assertEquals(0, printJobs.size());
        verify(printJobRepository, never()).save(any(PrintJob.class));
    }
    
    @Test
    void createPrintJobs_shouldReturnEmptyListWhenNoOrderLines() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of());
        
        // When
        List<PrintJob> printJobs = printingService.createPrintJobs(orderId);
        
        // Then
        assertNotNull(printJobs);
        assertTrue(printJobs.isEmpty());
        verify(printJobRepository, never()).save(any(PrintJob.class));
    }
    
    @Test
    void createPrintJobs_shouldReturnEmptyListWhenNoPrinters() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        OrderLine orderLine = new OrderLine(orderId, itemId, 2, BigDecimal.valueOf(10.00));
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of(orderLine));
        when(printerRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(List.of());
        
        // When
        List<PrintJob> printJobs = printingService.createPrintJobs(orderId);
        
        // Then
        assertNotNull(printJobs);
        assertTrue(printJobs.isEmpty());
        verify(printJobRepository, never()).save(any(PrintJob.class));
    }
    
    @Test
    void reprintOrder_shouldRequirePermission() {
        // Given
        doThrow(new AuthorizationException("Permission denied"))
            .when(authorizationApi).requirePermission(userId, AuthorizationApi.PermissionType.REPRINT_DOCUMENT);
        
        // When/Then
        assertThrows(AuthorizationException.class, () -> 
            printingService.reprintOrder(orderId, printerId, userId)
        );
        
        verify(authorizationApi).requirePermission(userId, AuthorizationApi.PermissionType.REPRINT_DOCUMENT);
        verify(printJobRepository, never()).save(any(PrintJob.class));
    }
    
    @Test
    void reprintOrder_shouldCreateNewPrintJobs() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        OrderLine orderLine = new OrderLine(orderId, itemId, 2, BigDecimal.valueOf(10.00));
        Item item = new Item(tenantId, UUID.randomUUID(), "Pizza", "Delicious pizza", BigDecimal.valueOf(10.00));
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        
        doNothing().when(authorizationApi).requirePermission(userId, AuthorizationApi.PermissionType.REPRINT_DOCUMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of(orderLine));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<PrintJob> printJobs = printingService.reprintOrder(orderId, printerId, userId);
        
        // Then
        assertNotNull(printJobs);
        assertEquals(1, printJobs.size());
        verify(authorizationApi).requirePermission(userId, AuthorizationApi.PermissionType.REPRINT_DOCUMENT);
        verify(printJobRepository, times(1)).save(any(PrintJob.class));
    }
    
    @Test
    void processPrintJob_shouldMarkAsSkippedWhenPrinterIsIgnore() {
        // Given
        UUID printJobId = UUID.randomUUID();
        PrintJob printJob = new PrintJob(tenantId, orderId, printerId, "Test content", "dedupe-key");
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.IGNORE);
        
        when(printJobRepository.findById(printJobId)).thenReturn(Optional.of(printJob));
        when(printerRepository.findById(printerId)).thenReturn(Optional.of(printer));
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printingService.processPrintJob(printJobId);
        
        // Then
        assertEquals(PrintJobStatus.SKIPPED, printJob.getStatus());
        verify(printJobRepository).save(printJob);
    }
    
    @Test
    void processPrintJob_shouldRedirectWhenPrinterIsRedirect() {
        // Given
        UUID printJobId = UUID.randomUUID();
        UUID redirectPrinterId = UUID.randomUUID();
        PrintJob printJob = new PrintJob(tenantId, orderId, printerId, "Test content", "dedupe-key");
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.REDIRECT);
        printer.setRedirectToPrinterId(redirectPrinterId);
        
        Printer redirectPrinter = new Printer(tenantId, siteId, "Backup Printer", "192.168.1.101", "kitchen");
        redirectPrinter.setStatus(PrinterStatus.NORMAL);
        
        when(printJobRepository.findById(printJobId)).thenReturn(Optional.of(printJob));
        when(printerRepository.findById(printerId)).thenReturn(Optional.of(printer));
        when(printerRepository.findById(redirectPrinterId)).thenReturn(Optional.of(redirectPrinter));
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printingService.processPrintJob(printJobId);
        
        // Then
        assertEquals(PrintJobStatus.PRINTED, printJob.getStatus());
        verify(printJobRepository).save(printJob);
    }
    
    @Test
    void processPrintJob_shouldSkipWhenRedirectTargetIsIgnore() {
        // Given
        UUID printJobId = UUID.randomUUID();
        UUID redirectPrinterId = UUID.randomUUID();
        PrintJob printJob = new PrintJob(tenantId, orderId, printerId, "Test content", "dedupe-key");
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.REDIRECT);
        printer.setRedirectToPrinterId(redirectPrinterId);
        
        Printer redirectPrinter = new Printer(tenantId, siteId, "Backup Printer", "192.168.1.101", "kitchen");
        redirectPrinter.setStatus(PrinterStatus.IGNORE);
        
        when(printJobRepository.findById(printJobId)).thenReturn(Optional.of(printJob));
        when(printerRepository.findById(printerId)).thenReturn(Optional.of(printer));
        when(printerRepository.findById(redirectPrinterId)).thenReturn(Optional.of(redirectPrinter));
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printingService.processPrintJob(printJobId);
        
        // Then
        assertEquals(PrintJobStatus.SKIPPED, printJob.getStatus());
        verify(printJobRepository).save(printJob);
    }
    
    @Test
    void processPrintJob_shouldMarkAsPrintedWhenNormal() {
        // Given
        UUID printJobId = UUID.randomUUID();
        PrintJob printJob = new PrintJob(tenantId, orderId, printerId, "Test content", "dedupe-key");
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.NORMAL);
        
        when(printJobRepository.findById(printJobId)).thenReturn(Optional.of(printJob));
        when(printerRepository.findById(printerId)).thenReturn(Optional.of(printer));
        when(printJobRepository.save(any(PrintJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printingService.processPrintJob(printJobId);
        
        // Then
        assertEquals(PrintJobStatus.PRINTED, printJob.getStatus());
        verify(printJobRepository).save(printJob);
    }
}
