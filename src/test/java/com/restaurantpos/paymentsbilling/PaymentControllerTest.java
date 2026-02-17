package com.restaurantpos.paymentsbilling;

import java.math.BigDecimal;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurantpos.paymentsbilling.controller.PaymentController;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;
import com.restaurantpos.paymentsbilling.service.PaymentService;

/**
 * Unit tests for PaymentController.
 * Tests REST endpoints for payment operations.
 */
@WebMvcTest(controllers = PaymentController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PaymentService paymentService;
    
    private UUID tenantId;
    private UUID orderId;
    private UUID paymentId;
    private UUID userId;
    private String idempotencyKey;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = "payment-" + UUID.randomUUID();
    }
    
    @Test
    void processPayment_withValidRequest_returnsCreated() throws Exception {
        // Given
        Payment payment = new Payment(tenantId, orderId, new BigDecimal("50.00"), PaymentMethod.CASH, idempotencyKey);
        payment.setStatus(PaymentStatus.COMPLETED);
        
        when(paymentService.processPayment(eq(orderId), eq(new BigDecimal("50.00")), eq(PaymentMethod.CASH), eq(idempotencyKey)))
            .thenReturn(payment);
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "amount": 50.00,
                "method": "CASH"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.amount").value(50.00))
            .andExpect(jsonPath("$.method").value("CASH"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey));
        
        verify(paymentService).processPayment(eq(orderId), eq(new BigDecimal("50.00")), eq(PaymentMethod.CASH), eq(idempotencyKey));
    }
    
    @Test
    void processPayment_withoutIdempotencyKey_returnsBadRequest() throws Exception {
        // Given
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "amount": 50.00,
                "method": "CASH"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void processPayment_withInvalidAmount_returnsBadRequest() throws Exception {
        // Given
        when(paymentService.processPayment(any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Payment amount must be positive"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "amount": -10.00,
                "method": "CASH"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void processPayment_withClosedOrder_returnsUnprocessableEntity() throws Exception {
        // Given
        when(paymentService.processPayment(any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Cannot process payment for closed or voided order"));
        
        String requestBody = String.format("""
            {
                "orderId": "%s",
                "amount": 50.00,
                "method": "CASH"
            }
            """, orderId);
        
        // When & Then
        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void voidPayment_withValidRequest_returnsOk() throws Exception {
        // Given
        String requestBody = """
            {
                "reason": "Customer requested refund"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/payments/" + paymentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        verify(paymentService).voidPayment(eq(paymentId), eq("Customer requested refund"), any(UUID.class));
    }
    
    @Test
    void voidPayment_withNonExistentPayment_returnsNotFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Payment not found"))
            .when(paymentService).voidPayment(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested refund"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/payments/" + paymentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void voidPayment_withAlreadyVoidedPayment_returnsUnprocessableEntity() throws Exception {
        // Given
        doThrow(new IllegalStateException("Payment is already voided"))
            .when(paymentService).voidPayment(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested refund"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/payments/" + paymentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void voidPayment_withoutPermission_returnsForbidden() throws Exception {
        // Given
        doThrow(new RuntimeException("User lacks required permission"))
            .when(paymentService).voidPayment(any(), any(), any());
        
        String requestBody = """
            {
                "reason": "Customer requested refund"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/payments/" + paymentId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void getOrderPayments_withValidOrderId_returnsPayments() throws Exception {
        // Given
        Payment payment1 = new Payment(tenantId, orderId, new BigDecimal("30.00"), PaymentMethod.CASH, "key1");
        payment1.setStatus(PaymentStatus.COMPLETED);
        
        Payment payment2 = new Payment(tenantId, orderId, new BigDecimal("20.00"), PaymentMethod.CARD, "key2");
        payment2.setStatus(PaymentStatus.COMPLETED);
        
        when(paymentService.getOrderPayments(orderId))
            .thenReturn(List.of(payment1, payment2));
        
        // When & Then
        mockMvc.perform(get("/api/orders/" + orderId + "/payments")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
            .andExpect(jsonPath("$[0].amount").value(30.00))
            .andExpect(jsonPath("$[0].method").value("CASH"))
            .andExpect(jsonPath("$[1].amount").value(20.00))
            .andExpect(jsonPath("$[1].method").value("CARD"));
        
        verify(paymentService).getOrderPayments(orderId);
    }
}
