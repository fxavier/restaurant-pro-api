package com.restaurantpos.kitchenprinting.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.kitchenprinting.entity.PrintJob;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrintJobStatus;
import com.restaurantpos.kitchenprinting.repository.PrintJobRepository;
import com.restaurantpos.kitchenprinting.repository.PrinterRepository;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.repository.OrderLineRepository;
import com.restaurantpos.orders.repository.OrderRepository;

/**
 * Service for managing print jobs and printer operations.
 * Handles print job creation, routing, and printer state management.
 * 
 * Requirements: 6.1, 6.2, 6.4, 6.5, 6.6
 */
@Service
@Transactional
public class PrintingService {
    
    private final PrintJobRepository printJobRepository;
    private final PrinterRepository printerRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ItemRepository itemRepository;
    private final AuthorizationApi authorizationApi;
    
    public PrintingService(
            PrintJobRepository printJobRepository,
            PrinterRepository printerRepository,
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            ItemRepository itemRepository,
            AuthorizationApi authorizationApi) {
        this.printJobRepository = printJobRepository;
        this.printerRepository = printerRepository;
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.itemRepository = itemRepository;
        this.authorizationApi = authorizationApi;
    }
    
    /**
     * Creates print jobs for a confirmed order.
     * Generates jobs based on item categories and zones, routing to appropriate printers.
     * 
     * @param orderId the order ID
     * @return list of created print jobs
     * @throws IllegalArgumentException if order not found
     * Requirements: 6.1, 6.2
     */
    public List<PrintJob> createPrintJobs(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        List<OrderLine> orderLines = orderLineRepository.findByOrderId(orderId);
        if (orderLines.isEmpty()) {
            return List.of();
        }
        
        // Get all printers for the site
        List<Printer> printers = printerRepository.findByTenantIdAndSiteId(
            order.getTenantId(), 
            order.getSiteId()
        );
        
        if (printers.isEmpty()) {
            return List.of();
        }
        
        List<PrintJob> printJobs = new ArrayList<>();
        
        // For each order line, create print jobs based on printer zones
        for (OrderLine orderLine : orderLines) {
            Item item = itemRepository.findById(orderLine.getItemId())
                .orElse(null);
            
            if (item == null) {
                continue;
            }
            
            // Route to printers based on zones
            // For now, we'll route to all printers (simplified routing)
            // In a real implementation, this would use item categories and zone mappings
            for (Printer printer : printers) {
                String dedupeKey = generateDedupeKey(orderId, orderLine.getId(), printer.getId());
                
                // Check if print job already exists (idempotency)
                if (printJobRepository.existsByTenantIdAndDedupeKey(order.getTenantId(), dedupeKey)) {
                    continue;
                }
                
                String content = formatPrintJobContent(order, orderLine, item);
                
                PrintJob printJob = new PrintJob(
                    order.getTenantId(),
                    orderId,
                    printer.getId(),
                    content,
                    dedupeKey
                );
                
                printJobs.add(printJobRepository.save(printJob));
            }
        }
        
        return printJobs;
    }
    
    /**
     * Manually reprints an order to a specific printer.
     * Requires REPRINT_DOCUMENT permission.
     * 
     * @param orderId the order ID
     * @param printerId the printer ID
     * @param userId the user requesting the reprint
     * @return list of created print jobs
     * @throws IllegalArgumentException if order or printer not found
     * Requirements: 6.6
     */
    public List<PrintJob> reprintOrder(UUID orderId, UUID printerId, UUID userId) {
        // Check permission
        authorizationApi.requirePermission(userId, AuthorizationApi.PermissionType.REPRINT_DOCUMENT);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        Printer printer = printerRepository.findByIdAndTenantId(printerId, order.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));
        
        List<OrderLine> orderLines = orderLineRepository.findByOrderId(orderId);
        if (orderLines.isEmpty()) {
            return List.of();
        }
        
        List<PrintJob> printJobs = new ArrayList<>();
        
        for (OrderLine orderLine : orderLines) {
            Item item = itemRepository.findById(orderLine.getItemId())
                .orElse(null);
            
            if (item == null) {
                continue;
            }
            
            // Generate unique dedupe key for reprint (include timestamp)
            String dedupeKey = generateReprintDedupeKey(orderId, orderLine.getId(), printerId);
            
            String content = formatPrintJobContent(order, orderLine, item);
            
            PrintJob printJob = new PrintJob(
                order.getTenantId(),
                orderId,
                printerId,
                content,
                dedupeKey
            );
            
            printJobs.add(printJobRepository.save(printJob));
        }
        
        return printJobs;
    }
    
    /**
     * Processes a print job by sending it to the printer.
     * Handles REDIRECT and IGNORE printer states.
     * 
     * @param printJobId the print job ID
     * @throws IllegalArgumentException if print job not found
     * Requirements: 6.4, 6.5
     */
    public void processPrintJob(UUID printJobId) {
        PrintJob printJob = printJobRepository.findById(printJobId)
            .orElseThrow(() -> new IllegalArgumentException("Print job not found: " + printJobId));
        
        if (!printJob.isPending()) {
            // Already processed
            return;
        }
        
        Printer printer = printerRepository.findById(printJob.getPrinterId())
            .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printJob.getPrinterId()));
        
        // Handle printer states
        if (printer.isIgnore()) {
            // IGNORE: Skip printing
            printJob.setStatus(PrintJobStatus.SKIPPED);
            printJobRepository.save(printJob);
            return;
        }
        
        final UUID targetPrinterId;
        
        if (printer.isRedirect() && printer.getRedirectToPrinterId() != null) {
            // REDIRECT: Route to alternate printer
            targetPrinterId = printer.getRedirectToPrinterId();
            
            Printer targetPrinter = printerRepository.findById(targetPrinterId)
                .orElseThrow(() -> new IllegalArgumentException("Redirect target printer not found: " + targetPrinterId));
            
            // Check if target printer is also in IGNORE mode
            if (targetPrinter.isIgnore()) {
                printJob.setStatus(PrintJobStatus.SKIPPED);
                printJobRepository.save(printJob);
                return;
            }
        } else {
            targetPrinterId = printJob.getPrinterId();
        }
        
        // Send to printer (actual printing logic would go here)
        // For now, we'll just mark it as printed
        boolean printSuccess = sendToPrinter(targetPrinterId, printJob.getContent());
        
        if (printSuccess) {
            printJob.setStatus(PrintJobStatus.PRINTED);
        } else {
            printJob.setStatus(PrintJobStatus.FAILED);
        }
        
        printJobRepository.save(printJob);
    }
    
    /**
     * Generates a dedupe key for a print job.
     * Format: order-{orderId}-line-{lineId}-printer-{printerId}
     */
    private String generateDedupeKey(UUID orderId, UUID orderLineId, UUID printerId) {
        return String.format("order-%s-line-%s-printer-%s", orderId, orderLineId, printerId);
    }
    
    /**
     * Generates a dedupe key for a reprint job.
     * Includes timestamp to allow multiple reprints.
     * Format: reprint-{orderId}-line-{lineId}-printer-{printerId}-{timestamp}
     */
    private String generateReprintDedupeKey(UUID orderId, UUID orderLineId, UUID printerId) {
        return String.format("reprint-%s-line-%s-printer-%s-%d", 
            orderId, orderLineId, printerId, Instant.now().toEpochMilli());
    }
    
    /**
     * Formats the print job content with order details.
     * Includes table number, item name, quantity, modifiers, and timestamp.
     * 
     * Requirements: 6.8
     */
    private String formatPrintJobContent(Order order, OrderLine orderLine, Item item) {
        StringBuilder content = new StringBuilder();
        
        content.append("=================================\n");
        content.append("ORDER TICKET\n");
        content.append("=================================\n");
        
        // Table number
        if (order.getTableId() != null) {
            content.append("Table: ").append(order.getTableId()).append("\n");
        }
        
        content.append("Order: ").append(order.getId()).append("\n");
        content.append("Time: ").append(Instant.now()).append("\n");
        content.append("---------------------------------\n");
        
        // Item details
        content.append("Item: ").append(item.getName()).append("\n");
        content.append("Quantity: ").append(orderLine.getQuantity()).append("\n");
        
        // Modifiers
        if (orderLine.getModifiers() != null && !orderLine.getModifiers().isEmpty()) {
            content.append("Modifiers:\n");
            orderLine.getModifiers().forEach((key, value) -> 
                content.append("  - ").append(key).append(": ").append(value).append("\n")
            );
        }
        
        // Notes
        if (orderLine.getNotes() != null && !orderLine.getNotes().isEmpty()) {
            content.append("Notes: ").append(orderLine.getNotes()).append("\n");
        }
        
        content.append("=================================\n");
        
        return content.toString();
    }
    
    /**
     * Sends content to a printer.
     * This is a placeholder for actual printer integration.
     * 
     * @param printerId the printer ID
     * @param content the content to print
     * @return true if successful, false otherwise
     */
    private boolean sendToPrinter(UUID printerId, String content) {
        // Placeholder for actual printer communication
        // In a real implementation, this would:
        // 1. Look up printer IP address
        // 2. Connect to printer via network
        // 3. Send print commands
        // 4. Handle errors and timeouts
        
        // For now, simulate success
        return true;
    }
}
