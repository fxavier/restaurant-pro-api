package com.restaurantpos.kitchenprinting.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;
import com.restaurantpos.kitchenprinting.repository.PrinterRepository;

/**
 * Service for managing printer configuration and status.
 * Handles printer state changes, redirects, testing, and listing.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4
 */
@Service
@Transactional
public class PrinterManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(PrinterManagementService.class);
    
    private final PrinterRepository printerRepository;
    
    public PrinterManagementService(PrinterRepository printerRepository) {
        this.printerRepository = printerRepository;
    }
    
    /**
     * Updates the status of a printer.
     * Changes are applied immediately to all subsequent print jobs.
     * 
     * @param printerId the printer ID
     * @param status the new printer status
     * @throws IllegalArgumentException if printer not found
     * Requirements: 11.1, 11.2, 11.7
     */
    public void updatePrinterStatus(UUID printerId, PrinterStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        
        Printer printer = printerRepository.findByIdAndTenantId(printerId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));
        
        PrinterStatus oldStatus = printer.getStatus();
        printer.setStatus(status);
        
        // Clear redirect if changing away from REDIRECT status
        if (status != PrinterStatus.REDIRECT && printer.getRedirectToPrinterId() != null) {
            printer.setRedirectToPrinterId(null);
        }
        
        printerRepository.save(printer);
        
        // Log the status change for audit trail (Requirement 11.7)
        logger.info("Printer status changed: printerId={}, tenantId={}, oldStatus={}, newStatus={}, timestamp={}", 
            printerId, tenantId, oldStatus, status, printer.getUpdatedAt());
    }
    
    /**
     * Redirects a printer's jobs to another printer.
     * Sets the printer to REDIRECT status and configures the target printer.
     * 
     * @param printerId the source printer ID
     * @param targetPrinterId the target printer ID to redirect to
     * @throws IllegalArgumentException if printer or target printer not found
     * Requirements: 11.3, 11.7
     */
    public void redirectPrinter(UUID printerId, UUID targetPrinterId) {
        UUID tenantId = TenantContext.getTenantId();
        
        Printer printer = printerRepository.findByIdAndTenantId(printerId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));
        
        // Verify target printer exists and belongs to same tenant
        Printer targetPrinter = printerRepository.findByIdAndTenantId(targetPrinterId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Target printer not found: " + targetPrinterId));
        
        // Prevent circular redirects
        if (targetPrinter.getRedirectToPrinterId() != null && 
            targetPrinter.getRedirectToPrinterId().equals(printerId)) {
            throw new IllegalArgumentException("Cannot create circular redirect between printers");
        }
        
        printer.setStatus(PrinterStatus.REDIRECT);
        printer.setRedirectToPrinterId(targetPrinterId);
        
        printerRepository.save(printer);
        
        // Log the redirect for audit trail (Requirement 11.7)
        logger.info("Printer redirect configured: printerId={}, targetPrinterId={}, tenantId={}, timestamp={}", 
            printerId, targetPrinterId, tenantId, printer.getUpdatedAt());
    }
    
    /**
     * Sends a test print to verify printer connectivity and configuration.
     * 
     * @param printerId the printer ID
     * @return true if test print successful, false otherwise
     * @throws IllegalArgumentException if printer not found
     * Requirements: 11.4
     */
    public boolean testPrinter(UUID printerId) {
        UUID tenantId = TenantContext.getTenantId();
        
        Printer printer = printerRepository.findByIdAndTenantId(printerId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));
        
        // Generate test print content
        String testContent = generateTestPrintContent(printer);
        
        // Send test print (actual printing logic would go here)
        boolean success = sendTestPrint(printer, testContent);
        
        // Log the test print attempt
        logger.info("Test print sent: printerId={}, printerName={}, tenantId={}, success={}", 
            printerId, printer.getName(), tenantId, success);
        
        return success;
    }
    
    /**
     * Lists all printers for a site with their current status.
     * 
     * @param siteId the site ID
     * @return list of printers
     * Requirements: 11.5
     */
    @Transactional(readOnly = true)
    public List<Printer> listPrinters(UUID siteId) {
        UUID tenantId = TenantContext.getTenantId();
        return printerRepository.findByTenantIdAndSiteId(tenantId, siteId);
    }
    
    /**
     * Generates test print content with printer details.
     */
    private String generateTestPrintContent(Printer printer) {
        StringBuilder content = new StringBuilder();
        
        content.append("=================================\n");
        content.append("TEST PRINT\n");
        content.append("=================================\n");
        content.append("Printer: ").append(printer.getName()).append("\n");
        content.append("Zone: ").append(printer.getZone() != null ? printer.getZone() : "N/A").append("\n");
        content.append("IP Address: ").append(printer.getIpAddress() != null ? printer.getIpAddress() : "N/A").append("\n");
        content.append("Status: ").append(printer.getStatus()).append("\n");
        content.append("Time: ").append(java.time.Instant.now()).append("\n");
        content.append("=================================\n");
        content.append("If you can read this, the printer\n");
        content.append("is working correctly.\n");
        content.append("=================================\n");
        
        return content.toString();
    }
    
    /**
     * Sends a test print to the printer.
     * This is a placeholder for actual printer integration.
     * 
     * @param printer the printer
     * @param content the test content
     * @return true if successful, false otherwise
     */
    private boolean sendTestPrint(Printer printer, String content) {
        // Placeholder for actual printer communication
        // In a real implementation, this would:
        // 1. Connect to printer via IP address
        // 2. Send print commands
        // 3. Handle errors and timeouts
        // 4. Return success/failure status
        
        // For now, simulate success if printer has an IP address
        return printer.getIpAddress() != null && !printer.getIpAddress().isEmpty();
    }
}
