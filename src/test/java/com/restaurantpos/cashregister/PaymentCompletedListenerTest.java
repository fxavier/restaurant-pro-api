package com.restaurantpos.cashregister;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.listener.PaymentCompletedListener;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.paymentsbilling.event.PaymentCompleted;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;

/**
 * Tests for PaymentCompletedListener.
 * 
 * Requirements: 10.4
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentCompletedListenerTest {
    
    @Autowired
    private PaymentCompletedListener listener;
    
    @Autowired
    private CashSessionRepository cashSessionRepository;
    
    @Autowired
    private CashRegisterRepository cashRegisterRepository;
    
    @Autowired
    private CashMovementRepository cashMovementRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID registerId;
    private UUID sessionId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        
        // Set tenant context for repository operations
        TenantContext.setTenantId(tenantId);
        
        // Create tenant and site records (required for foreign key constraints)
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, subscription_plan, status, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                tenantId, "Test Tenant", "BASIC", "ACTIVE");
        jdbcTemplate.update(
                "INSERT INTO sites (id, tenant_id, name, timezone, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                siteId, tenantId, "Test Site", "UTC");
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, email, role, status, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                employeeId, tenantId, "testuser", "hash", "test@example.com", "CASHIER", "ACTIVE");
        
        // Create a cash register
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        register = cashRegisterRepository.save(register);
        registerId = register.getId();
        
        // Create an open cash session
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        session = cashSessionRepository.save(session);
        sessionId = session.getId();
    }
    
    @Test
    void shouldCreateCashMovementForCashPayment() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);
        
        // Create a minimal payment record to satisfy foreign key constraint
        jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, site_id, order_type, status, total_amount, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                orderId, tenantId, siteId, "DINE_IN", "OPEN", amount);
        jdbcTemplate.update(
                "INSERT INTO payments (id, tenant_id, order_id, amount, payment_method, status, idempotency_key, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                paymentId, tenantId, orderId, amount, "CASH", "COMPLETED", UUID.randomUUID().toString());
        
        PaymentCompleted event = new PaymentCompleted(
                paymentId,
                orderId,
                tenantId,
                siteId,
                amount,
                PaymentMethod.CASH,
                Instant.now()
        );
        
        // When
        listener.handlePaymentCompleted(event);
        
        // Then
        var movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        assertThat(movements).hasSize(1);
        
        CashMovement movement = movements.get(0);
        assertThat(movement.getTenantId()).isEqualTo(tenantId);
        assertThat(movement.getSessionId()).isEqualTo(sessionId);
        assertThat(movement.getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movement.getAmount()).isEqualByComparingTo(amount);
        assertThat(movement.getPaymentId()).isEqualTo(paymentId);
        assertThat(movement.getReason()).contains(orderId.toString());
    }
    
    @Test
    void shouldNotCreateCashMovementForCardPayment() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);
        
        PaymentCompleted event = new PaymentCompleted(
                paymentId,
                orderId,
                tenantId,
                siteId,
                amount,
                PaymentMethod.CARD,
                Instant.now()
        );
        
        // When
        listener.handlePaymentCompleted(event);
        
        // Then
        var movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        assertThat(movements).isEmpty();
    }
    
    @Test
    void shouldNotCreateCashMovementForMobilePayment() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);
        
        PaymentCompleted event = new PaymentCompleted(
                paymentId,
                orderId,
                tenantId,
                siteId,
                amount,
                PaymentMethod.MOBILE,
                Instant.now()
        );
        
        // When
        listener.handlePaymentCompleted(event);
        
        // Then
        var movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        assertThat(movements).isEmpty();
    }
    
    @Test
    void shouldNotCreateCashMovementWhenNoOpenSession() {
        // Given - close the session
        CashSession session = cashSessionRepository.findById(sessionId).orElseThrow();
        cashSessionRepository.delete(session);
        
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50.00);
        
        PaymentCompleted event = new PaymentCompleted(
                paymentId,
                orderId,
                tenantId,
                siteId,
                amount,
                PaymentMethod.CASH,
                Instant.now()
        );
        
        // When
        listener.handlePaymentCompleted(event);
        
        // Then - no movement should be created
        var movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        assertThat(movements).isEmpty();
    }
    
    @Test
    void shouldCreateMultipleCashMovementsForMultiplePayments() {
        // Given
        UUID payment1Id = UUID.randomUUID();
        UUID payment2Id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount1 = BigDecimal.valueOf(30.00);
        BigDecimal amount2 = BigDecimal.valueOf(20.00);
        
        // Create order and payment records
        jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, site_id, order_type, status, total_amount, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                orderId, tenantId, siteId, "DINE_IN", "OPEN", amount1.add(amount2));
        jdbcTemplate.update(
                "INSERT INTO payments (id, tenant_id, order_id, amount, payment_method, status, idempotency_key, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                payment1Id, tenantId, orderId, amount1, "CASH", "COMPLETED", UUID.randomUUID().toString());
        jdbcTemplate.update(
                "INSERT INTO payments (id, tenant_id, order_id, amount, payment_method, status, idempotency_key, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                payment2Id, tenantId, orderId, amount2, "CASH", "COMPLETED", UUID.randomUUID().toString());
        
        PaymentCompleted event1 = new PaymentCompleted(
                payment1Id,
                orderId,
                tenantId,
                siteId,
                amount1,
                PaymentMethod.CASH,
                Instant.now()
        );
        
        PaymentCompleted event2 = new PaymentCompleted(
                payment2Id,
                orderId,
                tenantId,
                siteId,
                amount2,
                PaymentMethod.CASH,
                Instant.now()
        );
        
        // When
        listener.handlePaymentCompleted(event1);
        listener.handlePaymentCompleted(event2);
        
        // Then
        var movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        assertThat(movements).hasSize(2);
        
        assertThat(movements)
                .extracting(CashMovement::getAmount)
                .containsExactlyInAnyOrder(amount1, amount2);
        
        assertThat(movements)
                .extracting(CashMovement::getPaymentId)
                .containsExactlyInAnyOrder(payment1Id, payment2Id);
    }
}
