package com.restaurantpos.paymentsbilling;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.model.DocumentType;
import com.restaurantpos.paymentsbilling.repository.FiscalDocumentRepository;
import com.restaurantpos.paymentsbilling.service.BillingService;

/**
 * Unit tests for BillingService.
 * Tests fiscal document generation, voiding, subtotal printing, and bill splitting.
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    
    @Mock
    private FiscalDocumentRepository fiscalDocumentRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private AuthorizationApi authorizationApi;
    
    @InjectMocks
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
    void generateFiscalDocument_withReceipt_createsDocument() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(fiscalDocumentRepository.findMaxDocumentNumber(tenantId, siteId, DocumentType.RECEIPT))
            .thenReturn(null);
        when(fiscalDocumentRepository.save(any(FiscalDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        FiscalDocument document = billingService.generateFiscalDocument(
            orderId, DocumentType.RECEIPT, null);
        
        // Then
        assertNotNull(document);
        assertEquals(tenantId, document.getTenantId());
        assertEquals(siteId, document.getSiteId());
        assertEquals(DocumentType.RECEIPT, document.getDocumentType());
        assertEquals(orderId, document.getOrderId());
        assertEquals(BigDecimal.valueOf(100.00), document.getAmount());
        assertTrue(document.getDocumentNumber().startsWith("REC-"));
        
        verify(fiscalDocumentRepository, times(1)).save(any(FiscalDocument.class));
    }
    
    @Test
    void generateFiscalDocument_withInvoiceAndNif_createsDocument() {
        // Given
        String customerNif = "123456789";
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(150.00));
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(fiscalDocumentRepository.findMaxDocumentNumber(tenantId, siteId, DocumentType.INVOICE))
            .thenReturn(null);
        when(fiscalDocumentRepository.save(any(FiscalDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        FiscalDocument document = billingService.generateFiscalDocument(
            orderId, DocumentType.INVOICE, customerNif);
        
        // Then
        assertNotNull(document);
        assertEquals(DocumentType.INVOICE, document.getDocumentType());
        assertEquals(customerNif, document.getCustomerNif());
        assertTrue(document.getDocumentNumber().startsWith("INV-"));
        
        verify(fiscalDocumentRepository, times(1)).save(any(FiscalDocument.class));
    }
    
    @Test
    void generateFiscalDocument_withInvoiceWithoutNif_throwsException() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.generateFiscalDocument(orderId, DocumentType.INVOICE, null));
        
        verify(fiscalDocumentRepository, never()).save(any(FiscalDocument.class));
    }
    
    @Test
    void generateFiscalDocument_withInvoiceWithEmptyNif_throwsException() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.generateFiscalDocument(orderId, DocumentType.INVOICE, "   "));
        
        verify(fiscalDocumentRepository, never()).save(any(FiscalDocument.class));
    }
    
    @Test
    void generateFiscalDocument_withNonExistentOrder_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.generateFiscalDocument(orderId, DocumentType.RECEIPT, null));
    }
    
    @Test
    void generateFiscalDocument_withOrderWithoutSite_throwsException() {
        // Given
        Order order = new Order(tenantId, null, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            billingService.generateFiscalDocument(orderId, DocumentType.RECEIPT, null));
    }
    
    @Test
    void generateFiscalDocument_withSequentialNumbering_incrementsCorrectly() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(fiscalDocumentRepository.findMaxDocumentNumber(tenantId, siteId, DocumentType.RECEIPT))
            .thenReturn("REC-ABC-00042");
        when(fiscalDocumentRepository.save(any(FiscalDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        FiscalDocument document = billingService.generateFiscalDocument(
            orderId, DocumentType.RECEIPT, null);
        
        // Then
        assertNotNull(document);
        assertTrue(document.getDocumentNumber().endsWith("00043"));
    }
    
    @Test
    void generateFiscalDocument_withCreditNote_createsDocument() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(50.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(fiscalDocumentRepository.findMaxDocumentNumber(tenantId, siteId, DocumentType.CREDIT_NOTE))
            .thenReturn(null);
        when(fiscalDocumentRepository.save(any(FiscalDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        FiscalDocument document = billingService.generateFiscalDocument(
            orderId, DocumentType.CREDIT_NOTE, null);
        
        // Then
        assertNotNull(document);
        assertEquals(DocumentType.CREDIT_NOTE, document.getDocumentType());
        assertTrue(document.getDocumentNumber().startsWith("CRN-"));
    }
    
    @Test
    void voidFiscalDocument_withValidRequest_voidsDocument() {
        // Given
        FiscalDocument document = new FiscalDocument(
            tenantId, siteId, DocumentType.RECEIPT, "REC-001-00001", 
            orderId, BigDecimal.valueOf(100.00), null);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(fiscalDocumentRepository.findByIdAndTenantId(documentId, tenantId))
            .thenReturn(Optional.of(document));
        
        // When
        billingService.voidFiscalDocument(documentId, "Customer request", userId);
        
        // Then
        assertNotNull(document.getVoidedAt());
        assertTrue(document.isVoided());
        verify(authorizationApi, times(1)).requirePermission(userId, PermissionType.VOID_INVOICE);
        verify(fiscalDocumentRepository, times(1)).save(document);
    }
    
    @Test
    void voidFiscalDocument_withNonExistentDocument_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(fiscalDocumentRepository.findByIdAndTenantId(documentId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.voidFiscalDocument(documentId, "Customer request", userId));
    }
    
    @Test
    void voidFiscalDocument_withAlreadyVoidedDocument_throwsException() {
        // Given
        FiscalDocument document = new FiscalDocument(
            tenantId, siteId, DocumentType.RECEIPT, "REC-001-00001", 
            orderId, BigDecimal.valueOf(100.00), null);
        document.setVoidedAt(java.time.Instant.now());
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(fiscalDocumentRepository.findByIdAndTenantId(documentId, tenantId))
            .thenReturn(Optional.of(document));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            billingService.voidFiscalDocument(documentId, "Customer request", userId));
    }
    
    @Test
    void printSubtotal_withValidOrder_returnsFormattedBill() {
        // Given
        UUID tableId = UUID.randomUUID();
        Order order = new Order(tenantId, siteId, tableId, null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(125.50));
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When
        String subtotal = billingService.printSubtotal(orderId);
        
        // Then
        assertNotNull(subtotal);
        assertTrue(subtotal.contains("SUBTOTAL"));
        assertTrue(subtotal.contains(orderId.toString()));
        assertTrue(subtotal.contains(tableId.toString()));
        assertTrue(subtotal.contains("CONFIRMED"));
        assertTrue(subtotal.contains("€125.50"));
        assertTrue(subtotal.contains("This is not a fiscal document"));
    }
    
    @Test
    void printSubtotal_withNonExistentOrder_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.printSubtotal(orderId));
    }
    
    @Test
    void splitBill_withEvenSplit_dividesEqually() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When
        List<BigDecimal> splits = billingService.splitBill(orderId, 4);
        
        // Then
        assertNotNull(splits);
        assertEquals(4, splits.size());
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(splits.get(0)));
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(splits.get(1)));
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(splits.get(2)));
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(splits.get(3)));
        
        // Verify sum equals total
        BigDecimal sum = splits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, order.getTotalAmount().compareTo(sum));
    }
    
    @Test
    void splitBill_withUnevenSplit_handlesRemainder() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When
        List<BigDecimal> splits = billingService.splitBill(orderId, 3);
        
        // Then
        assertNotNull(splits);
        assertEquals(3, splits.size());
        assertEquals(0, BigDecimal.valueOf(33.33).compareTo(splits.get(0)));
        assertEquals(0, BigDecimal.valueOf(33.33).compareTo(splits.get(1)));
        assertEquals(0, BigDecimal.valueOf(33.34).compareTo(splits.get(2))); // Last split gets remainder
        
        // Verify sum equals total exactly
        BigDecimal sum = splits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, order.getTotalAmount().compareTo(sum));
    }
    
    @Test
    void splitBill_withSingleSplit_returnsFullAmount() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(75.50));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When
        List<BigDecimal> splits = billingService.splitBill(orderId, 1);
        
        // Then
        assertNotNull(splits);
        assertEquals(1, splits.size());
        assertEquals(BigDecimal.valueOf(75.50), splits.get(0));
    }
    
    @Test
    void splitBill_withZeroSplitCount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.splitBill(orderId, 0));
    }
    
    @Test
    void splitBill_withNegativeSplitCount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.splitBill(orderId, -1));
    }
    
    @Test
    void splitBill_withNonExistentOrder_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            billingService.splitBill(orderId, 2));
    }
    
    @Test
    void splitBill_withLargeSplitCount_dividesCorrectly() {
        // Given
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When
        List<BigDecimal> splits = billingService.splitBill(orderId, 10);
        
        // Then
        assertNotNull(splits);
        assertEquals(10, splits.size());
        
        // Verify sum equals total exactly
        BigDecimal sum = splits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, order.getTotalAmount().compareTo(sum));
    }
}
