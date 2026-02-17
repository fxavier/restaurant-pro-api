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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;
import com.restaurantpos.paymentsbilling.service.PaymentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * REST controller for payment operations.
 * Provides endpoints for processing payments, voiding payments, and retrieving order payments.
 * 
 * Requirements: 7.1, 7.2, 7.5, 7.8
 */
@RestController
@RequestMapping("/api")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Processes a payment for an order.
     * Supports idempotency via Idempotency-Key header.
     * 
     * POST /api/payments
     * 
     * Requirements: 7.1, 7.2, 7.5
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {
        try {
            logger.info("Processing payment for order {}: amount {}, method {}, idempotency key {}", 
                request.orderId(), request.amount(), request.method(), idempotencyKey);
            
            Payment payment = paymentService.processPayment(
                request.orderId(),
                request.amount(),
                request.method(),
                idempotencyKey
            );
            
            PaymentResponse response = toPaymentResponse(payment);
            logger.info("Payment processed successfully: {}", payment.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to process payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Payment processing failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Voids a payment with permission check.
     * Requires VOID_INVOICE permission.
     * 
     * POST /api/payments/{id}/void
     * 
     * Requirements: 7.8
     */
    @PostMapping("/payments/{id}/void")
    public ResponseEntity<Void> voidPayment(
            @PathVariable UUID id,
            @Valid @RequestBody VoidPaymentRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Voiding payment {}: reason {}", id, request.reason());
            
            paymentService.voidPayment(id, request.reason(), userId);
            
            logger.info("Payment voided successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to void payment: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Payment void failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("permission")) {
                logger.warn("Permission denied for voiding payment: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            logger.error("Error voiding payment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error voiding payment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets all payments for an order.
     * 
     * GET /api/orders/{orderId}/payments
     * 
     * Requirements: 7.1
     */
    @GetMapping("/orders/{orderId}/payments")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(@PathVariable UUID orderId) {
        try {
            logger.debug("Fetching payments for order: {}", orderId);
            
            List<Payment> payments = paymentService.getOrderPayments(orderId);
            List<PaymentResponse> response = payments.stream()
                .map(this::toPaymentResponse)
                .toList();
            
            logger.debug("Retrieved {} payments for order {}", response.size(), orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching payments for order: {}", e.getMessage());
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
    
    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getTenantId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getStatus(),
            payment.getIdempotencyKey(),
            payment.getTerminalTransactionId(),
            payment.getCreatedAt(),
            payment.getUpdatedAt(),
            payment.getVersion()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for processing a payment.
     */
    public record ProcessPaymentRequest(
        @NotNull UUID orderId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method
    ) {}
    
    /**
     * Request DTO for voiding a payment.
     */
    public record VoidPaymentRequest(
        @NotNull String reason
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for payment details.
     */
    public record PaymentResponse(
        UUID id,
        UUID tenantId,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String idempotencyKey,
        String terminalTransactionId,
        Instant createdAt,
        Instant updatedAt,
        Integer version
    ) {}
}
