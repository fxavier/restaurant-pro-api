package com.restaurantpos.paymentsbilling.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.model.DocumentType;
import com.restaurantpos.paymentsbilling.repository.FiscalDocumentRepository;

/**
 * Service for billing operations including fiscal document generation and bill management.
 * Handles invoice/receipt generation with sequential numbering, document voiding, and bill splitting.
 * 
 * Requirements: 7.6, 7.7, 7.8
 */
@Service
public class BillingService {
    
    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final OrderRepository orderRepository;
    private final AuthorizationApi authorizationApi;
    
    public BillingService(
            FiscalDocumentRepository fiscalDocumentRepository,
            OrderRepository orderRepository,
            AuthorizationApi authorizationApi) {
        this.fiscalDocumentRepository = fiscalDocumentRepository;
        this.orderRepository = orderRepository;
        this.authorizationApi = authorizationApi;
    }
    
    /**
     * Generates a fiscal document (invoice or receipt) for an order with sequential numbering.
     * For invoices, customerNif is required. For receipts, it's optional.
     * Document numbers are sequential per tenant, site, and document type.
     * 
     * @param orderId the order ID
     * @param documentType the type of document (INVOICE, RECEIPT, CREDIT_NOTE)
     * @param customerNif the customer's tax ID (required for invoices)
     * @return the generated fiscal document
     * @throws IllegalArgumentException if order not found or validation fails
     * 
     * Requirements: 7.6, 7.7
     */
    @Transactional
    public FiscalDocument generateFiscalDocument(UUID orderId, DocumentType documentType, String customerNif) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Fetch order
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // Validate order has a site
        if (order.getSiteId() == null) {
            throw new IllegalStateException("Order must have a site to generate fiscal document");
        }
        
        // Validate invoice requires NIF
        if (documentType == DocumentType.INVOICE && (customerNif == null || customerNif.trim().isEmpty())) {
            throw new IllegalArgumentException("Customer NIF is required for invoices");
        }
        
        // Generate sequential document number
        String documentNumber = generateNextDocumentNumber(tenantId, order.getSiteId(), documentType);
        
        // Create fiscal document
        FiscalDocument fiscalDocument = new FiscalDocument(
                tenantId,
                order.getSiteId(),
                documentType,
                documentNumber,
                orderId,
                order.getTotalAmount(),
                customerNif
        );
        
        return fiscalDocumentRepository.save(fiscalDocument);
    }
    
    /**
     * Voids a fiscal document with permission check and audit trail.
     * Requires VOID_INVOICE permission.
     * 
     * @param documentId the fiscal document ID to void
     * @param reason the reason for voiding
     * @param userId the user performing the void operation
     * @throws IllegalArgumentException if document not found
     * @throws RuntimeException if user lacks permission
     * 
     * Requirements: 7.8
     */
    @Transactional
    public void voidFiscalDocument(UUID documentId, String reason, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Check permission
        authorizationApi.requirePermission(userId, PermissionType.VOID_INVOICE);
        
        // Fetch fiscal document
        FiscalDocument fiscalDocument = fiscalDocumentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal document not found: " + documentId));
        
        // Validate document can be voided
        if (fiscalDocument.isVoided()) {
            throw new IllegalStateException("Fiscal document is already voided");
        }
        
        // Void document
        fiscalDocument.setVoidedAt(Instant.now());
        fiscalDocumentRepository.save(fiscalDocument);
        
        // TODO: Create audit log entry (will be implemented in audit module)
        // AuditLog: operation=VOID_FISCAL_DOCUMENT, userId, documentId, reason, timestamp
    }
    
    /**
     * Prints a subtotal (intermediate bill) for an order.
     * This generates a summary of the current order without creating a fiscal document.
     * Returns a formatted string representation of the bill.
     * 
     * @param orderId the order ID
     * @return formatted subtotal bill as string
     * @throws IllegalArgumentException if order not found
     * 
     * Requirements: 7.7
     */
    @Transactional(readOnly = true)
    public String printSubtotal(UUID orderId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Fetch order
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // Build subtotal bill
        StringBuilder bill = new StringBuilder();
        bill.append("========== SUBTOTAL ==========\n");
        bill.append("Order ID: ").append(orderId).append("\n");
        bill.append("Table: ").append(order.getTableId() != null ? order.getTableId() : "N/A").append("\n");
        bill.append("Status: ").append(order.getStatus()).append("\n");
        bill.append("------------------------------\n");
        bill.append("Total Amount: ").append(formatCurrency(order.getTotalAmount())).append("\n");
        bill.append("==============================\n");
        bill.append("This is not a fiscal document\n");
        
        return bill.toString();
    }
    
    /**
     * Splits a bill into multiple equal parts for split payment.
     * Returns a list of amounts representing each split portion.
     * 
     * @param orderId the order ID
     * @param splitCount the number of ways to split the bill (must be > 0)
     * @return list of split amounts
     * @throws IllegalArgumentException if order not found or splitCount invalid
     * 
     * Requirements: 7.7
     */
    @Transactional(readOnly = true)
    public List<BigDecimal> splitBill(UUID orderId, int splitCount) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate split count
        if (splitCount <= 0) {
            throw new IllegalArgumentException("Split count must be greater than 0");
        }
        
        // Fetch order
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // Calculate split amount
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal splitAmount = totalAmount.divide(
                BigDecimal.valueOf(splitCount), 
                2, 
                RoundingMode.DOWN
        );
        
        // Create list of split amounts
        List<BigDecimal> splits = new ArrayList<>();
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        
        // Add equal splits
        for (int i = 0; i < splitCount - 1; i++) {
            splits.add(splitAmount);
            allocatedAmount = allocatedAmount.add(splitAmount);
        }
        
        // Last split gets the remainder to ensure total matches exactly
        BigDecimal lastSplit = totalAmount.subtract(allocatedAmount);
        splits.add(lastSplit);
        
        return splits;
    }
    
    /**
     * Generates the next sequential document number for a given tenant, site, and document type.
     * Format: {TYPE}-{SITE_PREFIX}-{SEQUENCE}
     * Example: INV-001-00001, REC-001-00042
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param documentType the document type
     * @return the next document number
     */
    private String generateNextDocumentNumber(UUID tenantId, UUID siteId, DocumentType documentType) {
        // Get the highest existing document number
        String maxDocumentNumber = fiscalDocumentRepository.findMaxDocumentNumber(tenantId, siteId, documentType);
        
        // Extract sequence number from existing document or start at 1
        int nextSequence = 1;
        if (maxDocumentNumber != null && !maxDocumentNumber.isEmpty()) {
            // Extract the numeric part from the document number
            // Expected format: PREFIX-SITE-SEQUENCE
            String[] parts = maxDocumentNumber.split("-");
            if (parts.length >= 3) {
                try {
                    int currentSequence = Integer.parseInt(parts[2]);
                    nextSequence = currentSequence + 1;
                } catch (NumberFormatException e) {
                    // If parsing fails, start at 1
                    nextSequence = 1;
                }
            }
        }
        
        // Generate document number with prefix based on type
        String prefix = getDocumentTypePrefix(documentType);
        String sitePrefix = getSitePrefix(siteId);
        
        return String.format("%s-%s-%05d", prefix, sitePrefix, nextSequence);
    }
    
    /**
     * Gets the document type prefix for document numbering.
     */
    private String getDocumentTypePrefix(DocumentType documentType) {
        return switch (documentType) {
            case INVOICE -> "INV";
            case RECEIPT -> "REC";
            case CREDIT_NOTE -> "CRN";
        };
    }
    
    /**
     * Gets a short prefix from the site ID for document numbering.
     * Uses the first 3 characters of the site ID.
     */
    private String getSitePrefix(UUID siteId) {
        return siteId.toString().substring(0, 3).toUpperCase();
    }
    
    /**
     * Formats a currency amount for display.
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("€%.2f", amount);
    }
}
