package com.restaurantpos.paymentsbilling;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;
import com.restaurantpos.paymentsbilling.service.PaymentService;

/**
 * Unit tests for PaymentService.
 * Tests payment processing, voiding, and order payment queries.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private AuthorizationApi authorizationApi;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID orderId;
    private UUID paymentId;
    private UUID userId;
    private String idempotencyKey;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = "payment-" + UUID.randomUUID();
    }
    
    @Test
    void processPayment_withValidRequest_createsPayment() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByTenantIdAndOrderIdAndStatus(tenantId, orderId, PaymentStatus.COMPLETED))
            .thenReturn(List.of());
        
        // When
        Payment payment = paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey);
        
        // Then
        assertNotNull(payment);
        assertEquals(tenantId, payment.getTenantId());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(amount, payment.getAmount());
        assertEquals(PaymentMethod.CASH, payment.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(idempotencyKey, payment.getIdempotencyKey());
        
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
    
    @Test
    void processPayment_withExistingIdempotencyKey_returnsExistingPayment() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        Payment existingPayment = new Payment(tenantId, orderId, amount, PaymentMethod.CASH, idempotencyKey);
        existingPayment.setStatus(PaymentStatus.COMPLETED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.of(existingPayment));
        
        // When
        Payment payment = paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey);
        
        // Then
        assertNotNull(payment);
        assertEquals(existingPayment, payment);
        
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).findByIdAndTenantId(any(), any());
    }
    
    @Test
    void processPayment_withNullAmount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.processPayment(orderId, null, PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_withNegativeAmount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.processPayment(orderId, BigDecimal.valueOf(-10.00), PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_withZeroAmount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.processPayment(orderId, BigDecimal.ZERO, PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_withNonExistentOrder_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_withClosedOrder_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setStatus(OrderStatus.CLOSED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_withVoidedOrder_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setStatus(OrderStatus.VOIDED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey));
    }
    
    @Test
    void processPayment_whenFullyPaid_closesOrder() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(100.00);
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setStatus(OrderStatus.CONFIRMED);
        
        Payment newPayment = new Payment(tenantId, orderId, amount, PaymentMethod.CASH, idempotencyKey);
        newPayment.setStatus(PaymentStatus.COMPLETED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(newPayment);
        when(paymentRepository.findByTenantIdAndOrderIdAndStatus(tenantId, orderId, PaymentStatus.COMPLETED))
            .thenReturn(List.of(newPayment));
        
        // When
        paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey);
        
        // Then
        assertEquals(OrderStatus.CLOSED, order.getStatus());
        verify(orderRepository, times(1)).save(order);
    }
    
    @Test
    void processPayment_whenPartiallyPaid_doesNotCloseOrder() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        Order order = new Order(tenantId, siteId, UUID.randomUUID(), null, OrderType.DINE_IN);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setStatus(OrderStatus.CONFIRMED);
        
        Payment newPayment = new Payment(tenantId, orderId, amount, PaymentMethod.CASH, idempotencyKey);
        newPayment.setStatus(PaymentStatus.COMPLETED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey))
            .thenReturn(Optional.empty());
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
            .thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(newPayment);
        when(paymentRepository.findByTenantIdAndOrderIdAndStatus(tenantId, orderId, PaymentStatus.COMPLETED))
            .thenReturn(List.of(newPayment));
        
        // When
        paymentService.processPayment(orderId, amount, PaymentMethod.CASH, idempotencyKey);
        
        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository, never()).save(order);
    }
    
    @Test
    void voidPayment_withValidRequest_voidsPayment() {
        // Given
        Payment payment = new Payment(tenantId, orderId, BigDecimal.valueOf(50.00), PaymentMethod.CASH, idempotencyKey);
        payment.setStatus(PaymentStatus.COMPLETED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByIdAndTenantId(paymentId, tenantId))
            .thenReturn(Optional.of(payment));
        
        // When
        paymentService.voidPayment(paymentId, "Customer request", userId);
        
        // Then
        assertEquals(PaymentStatus.VOIDED, payment.getStatus());
        verify(authorizationApi, times(1)).requirePermission(userId, PermissionType.VOID_INVOICE);
        verify(paymentRepository, times(1)).save(payment);
    }
    
    @Test
    void voidPayment_withNonExistentPayment_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByIdAndTenantId(paymentId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.voidPayment(paymentId, "Customer request", userId));
    }
    
    @Test
    void voidPayment_withAlreadyVoidedPayment_throwsException() {
        // Given
        Payment payment = new Payment(tenantId, orderId, BigDecimal.valueOf(50.00), PaymentMethod.CASH, idempotencyKey);
        payment.setStatus(PaymentStatus.VOIDED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByIdAndTenantId(paymentId, tenantId))
            .thenReturn(Optional.of(payment));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            paymentService.voidPayment(paymentId, "Customer request", userId));
    }
    
    @Test
    void getOrderPayments_returnsAllPaymentsForOrder() {
        // Given
        Payment payment1 = new Payment(tenantId, orderId, BigDecimal.valueOf(50.00), PaymentMethod.CASH, "key1");
        Payment payment2 = new Payment(tenantId, orderId, BigDecimal.valueOf(30.00), PaymentMethod.CARD, "key2");
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(paymentRepository.findByTenantIdAndOrderId(tenantId, orderId))
            .thenReturn(List.of(payment1, payment2));
        
        // When
        List<Payment> payments = paymentService.getOrderPayments(orderId);
        
        // Then
        assertNotNull(payments);
        assertEquals(2, payments.size());
        verify(paymentRepository, times(1)).findByTenantIdAndOrderId(tenantId, orderId);
    }
    
    @Test
    void calculateChange_withPaymentGreaterThanTotal_returnsChange() {
        // Given
        BigDecimal orderTotal = BigDecimal.valueOf(45.50);
        BigDecimal paymentAmount = BigDecimal.valueOf(50.00);
        
        // When
        BigDecimal change = paymentService.calculateChange(orderTotal, paymentAmount);
        
        // Then
        assertEquals(BigDecimal.valueOf(4.50), change);
    }
    
    @Test
    void calculateChange_withPaymentEqualToTotal_returnsZero() {
        // Given
        BigDecimal orderTotal = BigDecimal.valueOf(50.00);
        BigDecimal paymentAmount = BigDecimal.valueOf(50.00);
        
        // When
        BigDecimal change = paymentService.calculateChange(orderTotal, paymentAmount);
        
        // Then
        assertEquals(BigDecimal.ZERO, change);
    }
    
    @Test
    void calculateChange_withPaymentLessThanTotal_returnsZero() {
        // Given
        BigDecimal orderTotal = BigDecimal.valueOf(50.00);
        BigDecimal paymentAmount = BigDecimal.valueOf(30.00);
        
        // When
        BigDecimal change = paymentService.calculateChange(orderTotal, paymentAmount);
        
        // Then
        assertEquals(BigDecimal.ZERO, change);
    }
    
    @Test
    void calculateChange_withNullOrderTotal_throwsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.calculateChange(null, BigDecimal.valueOf(50.00)));
    }
    
    @Test
    void calculateChange_withNullPaymentAmount_throwsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.calculateChange(BigDecimal.valueOf(50.00), null));
    }
}
