package com.restaurantpos.paymentsbilling.controller;

import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.model.DocumentType;
import com.restaurantpos.paymentsbilling.service.BillingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * REST controller for billing operations.
 * Provides endpoints for generating fiscal documents, voiding documents,
 * printing subtotals, and splitting bills.
 * 
 * Requirements: 7.6, 7.7, 7.8
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);
    
    private final BillingService billingService;
    
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }
    
    /**
     * Generates a fiscal document (invoice or receipt) for an order.
     * For invoices, customerNif is required.
     * 
     * POST /api/billing/documents
     * 
     * Requirements: 7.6, 7.7
     */
    @PostMapping("/documents")
    public ResponseEntity<FiscalDocumentResponse> generateFiscalDocument(
            @Valid @RequestBody GenerateFiscalDocumentRequest request) {
        try {
            logger.info("Generating fiscal document for order {}: type {}", 
                request.orderId(), request.documentType());
            
            FiscalDocument fiscalDocument = billingService.generateFiscalDocument(
                request.orderId(),
                request.documentType(),
                request.customerNif()
            );
            
            FiscalDocumentResponse response = toFiscalDocumentResponse(fiscalDocument);
            logger.info("Fiscal document generated successfully: {}", fiscalDocument.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to generate fiscal document: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Fiscal document generation failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error generating fiscal document: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Voids a fiscal document with permission check.
     * Requires VOID_INVOICE permission.
     * 
     * POST /api/billing/documents/{id}/void
     * 
     * Requirements: 7.8
     */
    @PostMapping("/documents/{id}/void")
    public ResponseEntity<Void> voidFiscalDocument(
            @PathVariable UUID id,
            @Valid @RequestBody VoidFiscalDocumentRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Voiding fiscal document {}: reason {}", id, request.reason());
            
            billingService.voidFiscalDocument(id, request.reason(), userId);
            
            logger.info("Fiscal document voided successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to void fiscal document: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Fiscal document void failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("permission")) {
                logger.warn("Permission denied for voiding fiscal document: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            logger.error("Error voiding fiscal document: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error voiding fiscal document: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Prints a subtotal (intermediate bill) for an order.
     * Returns a formatted string representation of the bill.
     * 
     * POST /api/billing/subtotal
     * 
     * Requirements: 7.7
     */
    @PostMapping("/subtotal")
    public ResponseEntity<SubtotalResponse> printSubtotal(
            @Valid @RequestBody PrintSubtotalRequest request) {
        try {
            logger.info("Printing subtotal for order: {}", request.orderId());
            
            String subtotalContent = billingService.printSubtotal(request.orderId());
            
            SubtotalResponse response = new SubtotalResponse(subtotalContent);
            logger.info("Subtotal printed successfully for order: {}", request.orderId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to print subtotal: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error printing subtotal: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Splits a bill into multiple equal parts for split payment.
     * Returns a list of amounts representing each split portion.
     * 
     * POST /api/billing/split
     * 
     * Requirements: 7.7
     */
    @PostMapping("/split")
    public ResponseEntity<SplitBillResponse> splitBill(
            @Valid @RequestBody SplitBillRequest request) {
        try {
            logger.info("Splitting bill for order {}: {} ways", 
                request.orderId(), request.splitCount());
            
            List<BigDecimal> splitAmounts = billingService.splitBill(
                request.orderId(),
                request.splitCount()
            );
            
            SplitBillResponse response = new SplitBillResponse(splitAmounts);
            logger.info("Bill split successfully for order {}: {} parts", 
                request.orderId(), splitAmounts.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to split bill: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error splitting bill: {}", e.getMessage());
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
    
    private FiscalDocumentResponse toFiscalDocumentResponse(FiscalDocument fiscalDocument) {
        return new FiscalDocumentResponse(
            fiscalDocument.getId(),
            fiscalDocument.getTenantId(),
            fiscalDocument.getSiteId(),
            fiscalDocument.getDocumentType(),
            fiscalDocument.getDocumentNumber(),
            fiscalDocument.getOrderId(),
            fiscalDocument.getAmount(),
            fiscalDocument.getCustomerNif(),
            fiscalDocument.getIssuedAt(),
            fiscalDocument.getVoidedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for generating a fiscal document.
     */
    public record GenerateFiscalDocumentRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,
        
        @NotNull(message = "Document type is required")
        DocumentType documentType,
        
        @Size(max = 20, message = "Customer NIF must not exceed 20 characters")
        String customerNif  // Required for invoices, optional for receipts
    ) {}
    
    /**
     * Request DTO for voiding a fiscal document.
     */
    public record VoidFiscalDocumentRequest(
        @NotNull(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
    ) {}
    
    /**
     * Request DTO for printing a subtotal.
     */
    public record PrintSubtotalRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId
    ) {}
    
    /**
     * Request DTO for splitting a bill.
     */
    public record SplitBillRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,
        
        @NotNull(message = "Split count is required")
        @Positive(message = "Split count must be positive")
        Integer splitCount
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for fiscal document details.
     */
    public record FiscalDocumentResponse(
        UUID id,
        UUID tenantId,
        UUID siteId,
        DocumentType documentType,
        String documentNumber,
        UUID orderId,
        BigDecimal amount,
        String customerNif,
        Instant issuedAt,
        Instant voidedAt
    ) {}
    
    /**
     * Response DTO for subtotal content.
     */
    public record SubtotalResponse(
        String content
    ) {}
    
    /**
     * Response DTO for split bill amounts.
     */
    public record SplitBillResponse(
        List<BigDecimal> splitAmounts
    ) {}
}
