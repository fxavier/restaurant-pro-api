package com.restaurantpos.paymentsbilling;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.entity.PaymentCardBlacklist;
import com.restaurantpos.paymentsbilling.model.DocumentType;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;

/**
 * Unit tests for payment entities.
 * Verifies entity creation and basic functionality.
 * 
 * Requirements: 7.1, 7.2, 7.6, 7.10
 */
class PaymentEntityTest {
    
    @Test
    void shouldCreatePaymentEntity() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        String idempotencyKey = "payment-123";
        
        Payment payment = new Payment(tenantId, orderId, amount, PaymentMethod.CASH, idempotencyKey);
        
        assertThat(payment.getTenantId()).isEqualTo(tenantId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.isPending()).isTrue();
    }
    
    @Test
    void shouldUpdatePaymentStatus() {
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), 
                                      new BigDecimal("50.00"), PaymentMethod.CARD, "key-123");
        
        payment.setStatus(PaymentStatus.COMPLETED);
        
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.isPending()).isFalse();
    }
    
    @Test
    void shouldCreateFiscalDocumentEntity() {
        UUID tenantId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String documentNumber = "INV-2024-001";
        BigDecimal amount = new BigDecimal("100.00");
        String customerNif = "123456789";
        
        FiscalDocument document = new FiscalDocument(tenantId, siteId, DocumentType.INVOICE, 
                                                     documentNumber, orderId, amount, customerNif);
        
        assertThat(document.getTenantId()).isEqualTo(tenantId);
        assertThat(document.getSiteId()).isEqualTo(siteId);
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.INVOICE);
        assertThat(document.getDocumentNumber()).isEqualTo(documentNumber);
        assertThat(document.getOrderId()).isEqualTo(orderId);
        assertThat(document.getAmount()).isEqualTo(amount);
        assertThat(document.getCustomerNif()).isEqualTo(customerNif);
        assertThat(document.isInvoice()).isTrue();
        assertThat(document.isVoided()).isFalse();
    }
    
    @Test
    void shouldVoidFiscalDocument() {
        FiscalDocument document = new FiscalDocument(UUID.randomUUID(), UUID.randomUUID(), 
                                                     DocumentType.RECEIPT, "REC-001", 
                                                     UUID.randomUUID(), new BigDecimal("50.00"), null);
        
        assertThat(document.isVoided()).isFalse();
        
        document.setVoidedAt(java.time.Instant.now());
        
        assertThat(document.isVoided()).isTrue();
    }
    
    @Test
    void shouldCreatePaymentCardBlacklistEntity() {
        UUID tenantId = UUID.randomUUID();
        String cardLastFour = "1234";
        String reason = "Fraudulent activity";
        
        PaymentCardBlacklist blacklist = new PaymentCardBlacklist(tenantId, cardLastFour, reason);
        
        assertThat(blacklist.getTenantId()).isEqualTo(tenantId);
        assertThat(blacklist.getCardLastFour()).isEqualTo(cardLastFour);
        assertThat(blacklist.getReason()).isEqualTo(reason);
        assertThat(blacklist.getBlockedAt()).isNotNull();
    }
}
