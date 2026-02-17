package com.restaurantpos.paymentsbilling;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurantpos.paymentsbilling.controller.BillingController;
import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.model.DocumentType;
import com.restaurantpos.paymentsbilling.service.BillingService;

/**
 * Unit tests for BillingController.
 * Tests REST endpoints for billing operations.
 */
@WebMvcTest(controllers = BillingController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
class BillingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BillingService billingService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID orderId;
    private UUID documentId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }
    
    @Test
    void generateFiscalDocument_withValidReceipt_returnsCreated() throws Exception {
        // Given
        FiscalDocument fiscalDocument = new FiscalDocument(
            tenantId, siteId, DocumentType.RECEIPT, "REC-001-00001", orderId, new BigDecimal("100.00"), null
        );
        
        when(billingService.generateFiscalDocument(eq(orderId), eq(DocumentType.RECEIPT), eq(null)))
            .thenReturn(fiscalDocument);
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "documentType": "RECEIPT",
                "customerNif": null
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.documentType").value("RECEIPT"))
            .andExpect(jsonPath("$.documentNumber").value("REC-001-00001"))
            .andExpect(jsonPath("$.amount").value(100.00));
        
        verify(billingService).generateFiscalDocument(eq(orderId), eq(DocumentType.RECEIPT), eq(null));
    }
    
    @Test
    void generateFiscalDocument_withValidInvoice_returnsCreated() throws Exception {
        // Given
        String customerNif = "123456789";
        FiscalDocument fiscalDocument = new FiscalDocument(
            tenantId, siteId, DocumentType.INVOICE, "INV-001-00001", orderId, new BigDecimal("100.00"), customerNif
        );
        
        when(billingService.generateFiscalDocument(eq(orderId), eq(DocumentType.INVOICE), eq(customerNif)))
            .thenReturn(fiscalDocument);
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "documentType": "INVOICE",
                "customerNif": "%s"
            }
            """, orderId, customerNif);
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.documentType").value("INVOICE"))
            .andExpect(jsonPath("$.documentNumber").value("INV-001-00001"))
            .andExpect(jsonPath("$.customerNif").value(customerNif))
            .andExpect(jsonPath("$.amount").value(100.00));
        
        verify(billingService).generateFiscalDocument(eq(orderId), eq(DocumentType.INVOICE), eq(customerNif));
    }
    
    @Test
    void generateFiscalDocument_withInvoiceWithoutNif_returnsBadRequest() throws Exception {
        // Given
        when(billingService.generateFiscalDocument(any(), eq(DocumentType.INVOICE), eq(null)))
            .thenThrow(new IllegalArgumentException("Customer NIF is required for invoices"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "documentType": "INVOICE",
                "customerNif": null
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void generateFiscalDocument_withNonExistentOrder_returnsBadRequest() throws Exception {
        // Given
        when(billingService.generateFiscalDocument(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Order not found"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "documentType": "RECEIPT",
                "customerNif": null
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void voidFiscalDocument_withValidRequest_returnsOk() throws Exception {
        // Given
        String requestBody = """
            {
                "reason": "Customer requested cancellation"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents/" + documentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        verify(billingService).voidFiscalDocument(eq(documentId), eq("Customer requested cancellation"), any(UUID.class));
    }
    
    @Test
    void voidFiscalDocument_withNonExistentDocument_returnsNotFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Fiscal document not found"))
            .when(billingService).voidFiscalDocument(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested cancellation"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents/" + documentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void voidFiscalDocument_withAlreadyVoidedDocument_returnsUnprocessableEntity() throws Exception {
        // Given
        doThrow(new IllegalStateException("Fiscal document is already voided"))
            .when(billingService).voidFiscalDocument(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested cancellation"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents/" + documentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void voidFiscalDocument_withoutPermission_returnsForbidden() throws Exception {
        // Given
        doThrow(new RuntimeException("User lacks required permission"))
            .when(billingService).voidFiscalDocument(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested cancellation"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/billing/documents/" + documentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void printSubtotal_withValidOrderId_returnsSubtotal() throws Exception {
        // Given
        String subtotalContent = """
            ========== SUBTOTAL ==========
            Order ID: %s
            Table: N/A
            Status: OPEN
            ------------------------------
            Total Amount: €100.00
            ==============================
            This is not a fiscal document
            """.formatted(orderId);
        
        when(billingService.printSubtotal(orderId))
            .thenReturn(subtotalContent);
        
        String requestBody = String.format("""
            {
                "orderId": "%s"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/subtotal")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").exists());
        
        verify(billingService).printSubtotal(orderId);
    }
    
    @Test
    void printSubtotal_withNonExistentOrder_returnsNotFound() throws Exception {
        // Given
        when(billingService.printSubtotal(any()))
            .thenThrow(new IllegalArgumentException("Order not found"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/subtotal")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void splitBill_withValidRequest_returnsSplitAmounts() throws Exception {
        // Given
        List<BigDecimal> splitAmounts = List.of(
            new BigDecimal("33.33"),
            new BigDecimal("33.33"),
            new BigDecimal("33.34")
        );
        
        when(billingService.splitBill(orderId, 3))
            .thenReturn(splitAmounts);
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "splitCount": 3
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/split")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.splitAmounts").isArray())
            .andExpect(jsonPath("$.splitAmounts.length()").value(3))
            .andExpect(jsonPath("$.splitAmounts[0]").value(33.33))
            .andExpect(jsonPath("$.splitAmounts[1]").value(33.33))
            .andExpect(jsonPath("$.splitAmounts[2]").value(33.34));
        
        verify(billingService).splitBill(orderId, 3);
    }
    
    @Test
    void splitBill_withInvalidSplitCount_returnsBadRequest() throws Exception {
        // Given
        when(billingService.splitBill(any(), eq(0)))
            .thenThrow(new IllegalArgumentException("Split count must be greater than 0"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "splitCount": 0
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/split")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void splitBill_withNonExistentOrder_returnsBadRequest() throws Exception {
        // Given
        when(billingService.splitBill(any(), anyInt()))
            .thenThrow(new IllegalArgumentException("Order not found"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "splitCount": 2
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/billing/split")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}
