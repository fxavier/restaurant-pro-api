package com.restaurantpos.paymentsbilling.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.event.PaymentCompleted;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;

/**
 * Service for payment processing operations.
 * Handles payment creation, voiding, and order payment queries.
 * 
 * Requirements: 7.1, 7.2, 7.5, 7.8, 7.9
 */
@Service
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuthorizationApi authorizationApi;
    private final ApplicationEventPublisher eventPublisher;
    
    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            AuthorizationApi authorizationApi,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.authorizationApi = authorizationApi;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Processes a payment for an order with idempotency support.
     * If a payment with the same idempotency key already exists, returns the existing payment.
     * After successful payment, checks if order is fully paid and closes it if so.
     * Emits PaymentCompleted event for cash tracking.
     * 
     * @param orderId the order ID
     * @param amount the payment amount
     * @param method the payment method
     * @param idempotencyKey the idempotency key to prevent duplicate payments
     * @return the created or existing payment
     * @throws IllegalArgumentException if order not found or amount is invalid
     * 
     * Requirements: 7.1, 7.2, 7.5
     */
    @Transactional
    public Payment processPayment(UUID orderId, BigDecimal amount, PaymentMethod method, String idempotencyKey) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        // Check for existing payment with same idempotency key (idempotency)
        var existingPayment = paymentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }
        
        // Fetch order
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // Validate order is not already closed or voided
        if (order.isClosed() || order.isVoided()) {
            throw new IllegalStateException("Cannot process payment for closed or voided order");
        }
        
        // Create payment
        Payment payment = new Payment(tenantId, orderId, amount, method, idempotencyKey);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment = paymentRepository.save(payment);
        
        // Emit PaymentCompleted event for cash tracking
        eventPublisher.publishEvent(new PaymentCompleted(
                payment.getId(),
                orderId,
                tenantId,
                order.getSiteId(),
                amount,
                method,
                Instant.now()
        ));
        
        // Check if order is fully paid and close if so
        checkAndCloseOrder(order, tenantId, orderId);
        
        return payment;
    }
    
    /**
     * Voids a payment with permission check and audit trail.
     * Requires VOID_INVOICE permission.
     * 
     * @param paymentId the payment ID to void
     * @param reason the reason for voiding
     * @param userId the user performing the void operation
     * @throws IllegalArgumentException if payment not found
     * @throws RuntimeException if user lacks permission
     * 
     * Requirements: 7.8
     */
    @Transactional
    public void voidPayment(UUID paymentId, String reason, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Check permission
        authorizationApi.requirePermission(userId, PermissionType.VOID_INVOICE);
        
        // Fetch payment
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        // Validate payment can be voided
        if (payment.isVoided()) {
            throw new IllegalStateException("Payment is already voided");
        }
        
        // Void payment
        payment.setStatus(PaymentStatus.VOIDED);
        paymentRepository.save(payment);
        
        // TODO: Create audit log entry (will be implemented in audit module)
        // AuditLog: operation=VOID_PAYMENT, userId, paymentId, reason, timestamp
    }
    
    /**
     * Gets all payments for an order.
     * 
     * @param orderId the order ID
     * @return list of payments for the order
     * 
     * Requirements: 7.1
     */
    @Transactional(readOnly = true)
    public List<Payment> getOrderPayments(UUID orderId) {
        UUID tenantId = authorizationApi.getTenantContext();
        return paymentRepository.findByTenantIdAndOrderId(tenantId, orderId);
    }
    
    /**
     * Calculates change for cash payments.
     * 
     * @param orderTotal the total amount of the order
     * @param paymentAmount the amount paid
     * @return the change amount (0 if payment is less than or equal to total)
     * 
     * Requirements: 7.9
     */
    public BigDecimal calculateChange(BigDecimal orderTotal, BigDecimal paymentAmount) {
        if (orderTotal == null || paymentAmount == null) {
            throw new IllegalArgumentException("Order total and payment amount must not be null");
        }
        
        BigDecimal change = paymentAmount.subtract(orderTotal);
        return change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
    }
    
    /**
     * Checks if an order is fully paid and closes it if so.
     * An order is fully paid when the sum of completed payments >= order total.
     * 
     * @param order the order to check
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * 
     * Requirements: 7.5
     */
    private void checkAndCloseOrder(Order order, UUID tenantId, UUID orderId) {
        List<Payment> completedPayments = paymentRepository.findByTenantIdAndOrderIdAndStatus(
                tenantId, orderId, PaymentStatus.COMPLETED);
        
        BigDecimal totalPaid = completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setStatus(OrderStatus.CLOSED);
            orderRepository.save(order);
        }
    }
}
