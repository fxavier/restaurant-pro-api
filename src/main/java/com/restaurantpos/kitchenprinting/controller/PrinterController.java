package com.restaurantpos.kitchenprinting.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.kitchenprinting.entity.PrintJob;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrintJobStatus;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;
import com.restaurantpos.kitchenprinting.service.PrinterManagementService;
import com.restaurantpos.kitchenprinting.service.PrintingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for printer management and print job operations.
 * Provides endpoints for listing printers, updating printer status, redirecting printers,
 * testing printers, and reprinting jobs.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4
 */
@RestController
@RequestMapping("/api")
public class PrinterController {
    
    private static final Logger logger = LoggerFactory.getLogger(PrinterController.class);
    
    private final PrinterManagementService printerManagementService;
    private final PrintingService printingService;
    
    public PrinterController(
            PrinterManagementService printerManagementService,
            PrintingService printingService) {
        this.printerManagementService = printerManagementService;
        this.printingService = printingService;
    }
    
    /**
     * Lists all printers for a site with their current status.
     * 
     * GET /api/printers?siteId={siteId}
     * 
     * Requirements: 11.5
     */
    @GetMapping("/printers")
    public ResponseEntity<List<PrinterResponse>> listPrinters(@RequestParam @NotNull UUID siteId) {
        try {
            logger.debug("Fetching printers for site: {}", siteId);
            
            List<Printer> printers = printerManagementService.listPrinters(siteId);
            List<PrinterResponse> response = printers.stream()
                .map(this::toPrinterResponse)
                .toList();
            
            logger.debug("Retrieved {} printers for site {}", response.size(), siteId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to list printers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error listing printers: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Updates the status of a printer.
     * Changes are applied immediately to all subsequent print jobs.
     * 
     * PUT /api/printers/{id}/status
     * 
     * Requirements: 11.1, 11.2
     */
    @PutMapping("/printers/{id}/status")
    public ResponseEntity<Void> updatePrinterStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrinterStatusRequest request) {
        try {
            logger.info("Updating printer status: printerId={}, newStatus={}", id, request.status());
            
            printerManagementService.updatePrinterStatus(id, request.status());
            
            logger.info("Printer status updated successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update printer status: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating printer status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Redirects a printer's jobs to another printer.
     * Sets the printer to REDIRECT status and configures the target printer.
     * 
     * POST /api/printers/{id}/redirect
     * 
     * Requirements: 11.3
     */
    @PostMapping("/printers/{id}/redirect")
    public ResponseEntity<Void> redirectPrinter(
            @PathVariable UUID id,
            @Valid @RequestBody RedirectPrinterRequest request) {
        try {
            logger.info("Redirecting printer: printerId={}, targetPrinterId={}", 
                id, request.targetPrinterId());
            
            printerManagementService.redirectPrinter(id, request.targetPrinterId());
            
            logger.info("Printer redirect configured successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to redirect printer: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error redirecting printer: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Sends a test print to verify printer connectivity and configuration.
     * 
     * POST /api/printers/{id}/test
     * 
     * Requirements: 11.4
     */
    @PostMapping("/printers/{id}/test")
    public ResponseEntity<TestPrinterResponse> testPrinter(@PathVariable UUID id) {
        try {
            logger.info("Testing printer: {}", id);
            
            boolean success = printerManagementService.testPrinter(id);
            
            TestPrinterResponse response = new TestPrinterResponse(success);
            logger.info("Test print completed: printerId={}, success={}", id, success);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to test printer: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error testing printer: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manually reprints an order to a specific printer.
     * Requires REPRINT_DOCUMENT permission.
     * 
     * POST /api/print-jobs/{id}/reprint
     * 
     * Requirements: 6.6
     */
    @PostMapping("/print-jobs/{id}/reprint")
    public ResponseEntity<List<PrintJobResponse>> reprintJob(
            @PathVariable UUID id,
            @Valid @RequestBody ReprintJobRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Reprinting order: orderId={}, printerId={}, userId={}", 
                id, request.printerId(), userId);
            
            List<PrintJob> printJobs = printingService.reprintOrder(id, request.printerId(), userId);
            List<PrintJobResponse> response = printJobs.stream()
                .map(this::toPrintJobResponse)
                .toList();
            
            logger.info("Order reprinted successfully: {} print jobs created", printJobs.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to reprint job: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException e) {
            logger.warn("Permission denied for reprint: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error reprinting job: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    /**
     * Extracts the user ID from the current authentication context.
     */
    private UUID extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Invalid user ID in JWT: " + subject, e);
                }
            }
        }
        
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
    
    private PrinterResponse toPrinterResponse(Printer printer) {
        return new PrinterResponse(
            printer.getId(),
            printer.getTenantId(),
            printer.getSiteId(),
            printer.getName(),
            printer.getIpAddress(),
            printer.getZone(),
            printer.getStatus(),
            printer.getRedirectToPrinterId(),
            printer.getCreatedAt(),
            printer.getUpdatedAt()
        );
    }
    
    private PrintJobResponse toPrintJobResponse(PrintJob printJob) {
        return new PrintJobResponse(
            printJob.getId(),
            printJob.getTenantId(),
            printJob.getOrderId(),
            printJob.getPrinterId(),
            printJob.getStatus(),
            printJob.getDedupeKey(),
            printJob.getCreatedAt(),
            printJob.getUpdatedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for updating printer status.
     */
    public record UpdatePrinterStatusRequest(
        @NotNull(message = "Printer status is required")
        PrinterStatus status
    ) {}
    
    /**
     * Request DTO for redirecting a printer.
     */
    public record RedirectPrinterRequest(
        @NotNull(message = "Target printer ID is required")
        UUID targetPrinterId
    ) {}
    
    /**
     * Request DTO for reprinting a job.
     */
    public record ReprintJobRequest(
        @NotNull(message = "Printer ID is required")
        UUID printerId
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for printer details.
     */
    public record PrinterResponse(
        UUID id,
        UUID tenantId,
        UUID siteId,
        String name,
        String ipAddress,
        String zone,
        PrinterStatus status,
        UUID redirectToPrinterId,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    /**
     * Response DTO for test printer result.
     */
    public record TestPrinterResponse(
        boolean success
    ) {}
    
    /**
     * Response DTO for print job details.
     */
    public record PrintJobResponse(
        UUID id,
        UUID tenantId,
        UUID orderId,
        UUID printerId,
        PrintJobStatus status,
        String dedupeKey,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
