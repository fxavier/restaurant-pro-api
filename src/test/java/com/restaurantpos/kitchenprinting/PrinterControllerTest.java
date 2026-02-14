package com.restaurantpos.kitchenprinting;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurantpos.kitchenprinting.controller.PrinterController;
import com.restaurantpos.kitchenprinting.entity.PrintJob;
import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;
import com.restaurantpos.kitchenprinting.service.PrinterManagementService;
import com.restaurantpos.kitchenprinting.service.PrintingService;

/**
 * Unit tests for PrinterController.
 * Tests REST endpoints for printer management and print job operations.
 */
@WebMvcTest(controllers = PrinterController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
class PrinterControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PrinterManagementService printerManagementService;
    
    @MockBean
    private PrintingService printingService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID printerId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        printerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }
    
    @Test
    void listPrinters_withValidSiteId_returnsOk() throws Exception {
        // Given
        Printer printer1 = new Printer(tenantId, siteId, "Kitchen Printer", "192.168.1.10", "Kitchen");
        Printer printer2 = new Printer(tenantId, siteId, "Bar Printer", "192.168.1.11", "Bar");
        
        when(printerManagementService.listPrinters(siteId))
            .thenReturn(List.of(printer1, printer2));
        
        // When & Then
        mockMvc.perform(get("/api/printers")
                .with(csrf())
                .param("siteId", siteId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Kitchen Printer"))
            .andExpect(jsonPath("$[0].ipAddress").value("192.168.1.10"))
            .andExpect(jsonPath("$[0].zone").value("Kitchen"))
            .andExpect(jsonPath("$[0].status").value("NORMAL"))
            .andExpect(jsonPath("$[1].name").value("Bar Printer"))
            .andExpect(jsonPath("$[1].ipAddress").value("192.168.1.11"))
            .andExpect(jsonPath("$[1].zone").value("Bar"));
        
        verify(printerManagementService).listPrinters(siteId);
    }
    
    @Test
    void listPrinters_withEmptyResult_returnsEmptyList() throws Exception {
        // Given
        when(printerManagementService.listPrinters(siteId))
            .thenReturn(List.of());
        
        // When & Then
        mockMvc.perform(get("/api/printers")
                .with(csrf())
                .param("siteId", siteId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    void listPrinters_withInvalidSiteId_returnsBadRequest() throws Exception {
        // Given
        when(printerManagementService.listPrinters(any(UUID.class)))
            .thenThrow(new IllegalArgumentException("Site not found"));
        
        // When & Then
        mockMvc.perform(get("/api/printers")
                .with(csrf())
                .param("siteId", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void updatePrinterStatus_withValidRequest_returnsOk() throws Exception {
        // Given
        String requestBody = """
            {
                "status": "WAIT"
            }
            """;
        
        // When & Then
        mockMvc.perform(put("/api/printers/{id}/status", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        verify(printerManagementService).updatePrinterStatus(printerId, PrinterStatus.WAIT);
    }
    
    @Test
    void updatePrinterStatus_withNonExistentPrinter_returnsNotFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Printer not found"))
            .when(printerManagementService).updatePrinterStatus(any(UUID.class), any(PrinterStatus.class));
        
        String requestBody = """
            {
                "status": "IGNORE"
            }
            """;
        
        // When & Then
        mockMvc.perform(put("/api/printers/{id}/status", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void updatePrinterStatus_withAllStatuses_returnsOk() throws Exception {
        // Test all printer statuses
        for (PrinterStatus status : PrinterStatus.values()) {
            String requestBody = """
                {
                    "status": "%s"
                }
                """.formatted(status.name());
            
            mockMvc.perform(put("/api/printers/{id}/status", printerId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk());
            
            verify(printerManagementService).updatePrinterStatus(printerId, status);
        }
    }
    
    @Test
    void redirectPrinter_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID targetPrinterId = UUID.randomUUID();
        
        String requestBody = """
            {
                "targetPrinterId": "%s"
            }
            """.formatted(targetPrinterId);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/redirect", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        verify(printerManagementService).redirectPrinter(printerId, targetPrinterId);
    }
    
    @Test
    void redirectPrinter_withNonExistentPrinter_returnsNotFound() throws Exception {
        // Given
        UUID targetPrinterId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Printer not found"))
            .when(printerManagementService).redirectPrinter(any(UUID.class), any(UUID.class));
        
        String requestBody = """
            {
                "targetPrinterId": "%s"
            }
            """.formatted(targetPrinterId);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/redirect", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void redirectPrinter_withNonExistentTargetPrinter_returnsNotFound() throws Exception {
        // Given
        UUID targetPrinterId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Target printer not found"))
            .when(printerManagementService).redirectPrinter(any(UUID.class), any(UUID.class));
        
        String requestBody = """
            {
                "targetPrinterId": "%s"
            }
            """.formatted(targetPrinterId);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/redirect", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void redirectPrinter_withCircularRedirect_returnsBadRequest() throws Exception {
        // Given
        UUID targetPrinterId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Cannot create circular redirect between printers"))
            .when(printerManagementService).redirectPrinter(any(UUID.class), any(UUID.class));
        
        String requestBody = """
            {
                "targetPrinterId": "%s"
            }
            """.formatted(targetPrinterId);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/redirect", printerId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testPrinter_withValidPrinter_returnsSuccess() throws Exception {
        // Given
        when(printerManagementService.testPrinter(printerId))
            .thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/test", printerId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
        
        verify(printerManagementService).testPrinter(printerId);
    }
    
    @Test
    void testPrinter_withFailedTest_returnsFailure() throws Exception {
        // Given
        when(printerManagementService.testPrinter(printerId))
            .thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/test", printerId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    void testPrinter_withNonExistentPrinter_returnsNotFound() throws Exception {
        // Given
        when(printerManagementService.testPrinter(any(UUID.class)))
            .thenThrow(new IllegalArgumentException("Printer not found"));
        
        // When & Then
        mockMvc.perform(post("/api/printers/{id}/test", printerId)
                .with(csrf()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void reprintJob_withValidRequest_returnsCreated() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        
        PrintJob printJob1 = new PrintJob(tenantId, orderId, printerId, "Print content 1", "dedupe-1");
        PrintJob printJob2 = new PrintJob(tenantId, orderId, printerId, "Print content 2", "dedupe-2");
        
        when(printingService.reprintOrder(eq(orderId), eq(printerId), any(UUID.class)))
            .thenReturn(List.of(printJob1, printJob2));
        
        String requestBody = """
            {
                "printerId": "%s"
            }
            """.formatted(printerId);
        
        // When & Then
        mockMvc.perform(post("/api/print-jobs/{id}/reprint", orderId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
            .andExpect(jsonPath("$[0].printerId").value(printerId.toString()))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
    
    @Test
    void reprintJob_withNonExistentOrder_returnsNotFound() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        
        when(printingService.reprintOrder(eq(orderId), eq(printerId), any(UUID.class)))
            .thenThrow(new IllegalArgumentException("Order not found"));
        
        String requestBody = """
            {
                "printerId": "%s"
            }
            """.formatted(printerId);
        
        // When & Then
        mockMvc.perform(post("/api/print-jobs/{id}/reprint", orderId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void reprintJob_withNonExistentPrinter_returnsNotFound() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        
        when(printingService.reprintOrder(eq(orderId), eq(printerId), any(UUID.class)))
            .thenThrow(new IllegalArgumentException("Printer not found"));
        
        String requestBody = """
            {
                "printerId": "%s"
            }
            """.formatted(printerId);
        
        // When & Then
        mockMvc.perform(post("/api/print-jobs/{id}/reprint", orderId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void reprintJob_withoutPermission_returnsForbidden() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        
        when(printingService.reprintOrder(eq(orderId), eq(printerId), any(UUID.class)))
            .thenThrow(new SecurityException("Permission denied"));
        
        String requestBody = """
            {
                "printerId": "%s"
            }
            """.formatted(printerId);
        
        // When & Then
        mockMvc.perform(post("/api/print-jobs/{id}/reprint", orderId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }
}
