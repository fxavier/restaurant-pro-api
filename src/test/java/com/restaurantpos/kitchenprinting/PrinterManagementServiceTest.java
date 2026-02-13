package com.restaurantpos.kitchenprinting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;
import com.restaurantpos.kitchenprinting.repository.PrinterRepository;
import com.restaurantpos.kitchenprinting.service.PrinterManagementService;

/**
 * Unit tests for PrinterManagementService.
 * Tests printer status updates, redirects, test printing, and listing.
 */
@ExtendWith(MockitoExtension.class)
class PrinterManagementServiceTest {
    
    @Mock
    private PrinterRepository printerRepository;
    
    @InjectMocks
    private PrinterManagementService printerManagementService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID printerId;
    private UUID targetPrinterId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        printerId = UUID.randomUUID();
        targetPrinterId = UUID.randomUUID();
        
        // Set tenant context
        TenantContext.setTenantId(tenantId);
    }
    
    @Test
    void updatePrinterStatus_shouldUpdateStatusSuccessfully() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.NORMAL);
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(printerRepository.save(any(Printer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printerManagementService.updatePrinterStatus(printerId, PrinterStatus.WAIT);
        
        // Then
        assertEquals(PrinterStatus.WAIT, printer.getStatus());
        verify(printerRepository).save(printer);
    }
    
    @Test
    void updatePrinterStatus_shouldClearRedirectWhenChangingFromRedirect() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        printer.setStatus(PrinterStatus.REDIRECT);
        printer.setRedirectToPrinterId(targetPrinterId);
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(printerRepository.save(any(Printer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printerManagementService.updatePrinterStatus(printerId, PrinterStatus.NORMAL);
        
        // Then
        assertEquals(PrinterStatus.NORMAL, printer.getStatus());
        assertNull(printer.getRedirectToPrinterId());
        verify(printerRepository).save(printer);
    }
    
    @Test
    void updatePrinterStatus_shouldThrowExceptionWhenPrinterNotFound() {
        // Given
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.empty());
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            printerManagementService.updatePrinterStatus(printerId, PrinterStatus.IGNORE)
        );
        
        assertTrue(exception.getMessage().contains("Printer not found"));
    }
    
    @Test
    void redirectPrinter_shouldSetRedirectSuccessfully() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        Printer targetPrinter = new Printer(tenantId, siteId, "Backup Printer", "192.168.1.101", "kitchen");
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(printerRepository.findByIdAndTenantId(targetPrinterId, tenantId)).thenReturn(Optional.of(targetPrinter));
        when(printerRepository.save(any(Printer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        printerManagementService.redirectPrinter(printerId, targetPrinterId);
        
        // Then
        assertEquals(PrinterStatus.REDIRECT, printer.getStatus());
        assertEquals(targetPrinterId, printer.getRedirectToPrinterId());
        verify(printerRepository).save(printer);
    }
    
    @Test
    void redirectPrinter_shouldThrowExceptionWhenSourcePrinterNotFound() {
        // Given
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.empty());
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            printerManagementService.redirectPrinter(printerId, targetPrinterId)
        );
        
        assertTrue(exception.getMessage().contains("Printer not found"));
    }
    
    @Test
    void redirectPrinter_shouldThrowExceptionWhenTargetPrinterNotFound() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(printerRepository.findByIdAndTenantId(targetPrinterId, tenantId)).thenReturn(Optional.empty());
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            printerManagementService.redirectPrinter(printerId, targetPrinterId)
        );
        
        assertTrue(exception.getMessage().contains("Target printer not found"));
    }
    
    @Test
    void redirectPrinter_shouldPreventCircularRedirects() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        Printer targetPrinter = new Printer(tenantId, siteId, "Backup Printer", "192.168.1.101", "kitchen");
        targetPrinter.setRedirectToPrinterId(printerId); // Target already redirects to source
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        when(printerRepository.findByIdAndTenantId(targetPrinterId, tenantId)).thenReturn(Optional.of(targetPrinter));
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            printerManagementService.redirectPrinter(printerId, targetPrinterId)
        );
        
        assertTrue(exception.getMessage().contains("circular redirect"));
    }
    
    @Test
    void testPrinter_shouldReturnTrueWhenPrinterHasIpAddress() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        
        // When
        boolean result = printerManagementService.testPrinter(printerId);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testPrinter_shouldReturnFalseWhenPrinterHasNoIpAddress() {
        // Given
        Printer printer = new Printer(tenantId, siteId, "Kitchen Printer", null, "kitchen");
        
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.of(printer));
        
        // When
        boolean result = printerManagementService.testPrinter(printerId);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testPrinter_shouldThrowExceptionWhenPrinterNotFound() {
        // Given
        when(printerRepository.findByIdAndTenantId(printerId, tenantId)).thenReturn(Optional.empty());
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            printerManagementService.testPrinter(printerId)
        );
        
        assertTrue(exception.getMessage().contains("Printer not found"));
    }
    
    @Test
    void listPrinters_shouldReturnAllPrintersForSite() {
        // Given
        Printer printer1 = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.100", "kitchen");
        Printer printer2 = new Printer(tenantId, siteId, "Bar Printer", "192.168.1.101", "bar");
        List<Printer> printers = List.of(printer1, printer2);
        
        when(printerRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(printers);
        
        // When
        List<Printer> result = printerManagementService.listPrinters(siteId);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Kitchen Printer", result.get(0).getName());
        assertEquals("Bar Printer", result.get(1).getName());
    }
    
    @Test
    void listPrinters_shouldReturnEmptyListWhenNoPrinters() {
        // Given
        when(printerRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(List.of());
        
        // When
        List<Printer> result = printerManagementService.listPrinters(siteId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
