package com.restaurantpos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.orders.service.OrderService;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;
import com.restaurantpos.paymentsbilling.service.BillingService;
import com.restaurantpos.paymentsbilling.service.PaymentService;

/**
 * Integration test for partial payment and split bill functionality.
 * 
 * Tests the end-to-end flow:
 * 1. Create order with items
 * 2. Add multiple partial payments
 * 3. Verify order remains open until fully paid
 * 4. Complete final payment
 * 5. Verify order is closed
 * 6. Test split bill calculation
 * 
 * Requirements: Testing Strategy, 7.3, 7.5
 */
class PartialPaymentAndSplitBillIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void testPartialPaymentsAndOrderClosure() {
        // Setup: Create test data
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .status("ACTIVE")
                .build();

        UUID siteId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantId)
                .name("Main Location")
                .build();

        UUID userId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantId)
                .username("cashier1")
                .role("CASHIER")
                .build();

        UUID tableId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableNumber("T5")
                .status("OCCUPIED")
                .build();

        // Create catalog items
        UUID familyId = TestDataBuilder.family(jdbcTemplate)
                .tenantId(tenantId)
                .name("Food")
                .build();

        UUID subfamilyId = TestDataBuilder.subfamily(jdbcTemplate)
                .tenantId(tenantId)
                .familyId(familyId)
                .name("Main Dishes")
                .build();

        UUID item1Id = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantId)
                .subfamilyId(subfamilyId)
                .name("Steak")
                .basePrice(new BigDecimal("25.00"))
                .available(true)
                .build();

        UUID item2Id = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantId)
                .subfamilyId(subfamilyId)
                .name("Salad")
                .basePrice(new BigDecimal("8.50"))
                .available(true)
                .build();

        // Set tenant context
        TenantContext.setTenantId(tenantId);

        try {
            // Step 1: Create order and add items
            Order order = orderService.createOrder(tableId, OrderType.DINE_IN, siteId, null, userId);
            assertNotNull(order, "Order should be created");
            assertEquals(OrderStatus.OPEN, order.getStatus(), "Order should be in OPEN status");

            // Add order lines
            orderService.addOrderLine(order.getId(), item1Id, 2, null, null, userId);
            orderService.addOrderLine(order.getId(), item2Id, 1, null, null, userId);

            // Confirm order
            orderService.confirmOrder(order.getId(), userId);

            // Verify order total: (25.00 * 2) + (8.50 * 1) = 58.50
            Order confirmedOrder = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            BigDecimal expectedTotal = new BigDecimal("58.50");
            assertEquals(0, expectedTotal.compareTo(confirmedOrder.getTotalAmount()),
                    "Order total should be 58.50");

            // Step 2: Process first partial payment (20.00)
            String idempotencyKey1 = UUID.randomUUID().toString();
            Payment payment1 = paymentService.processPayment(
                    order.getId(),
                    new BigDecimal("20.00"),
                    PaymentMethod.CASH,
                    idempotencyKey1
            );
            assertNotNull(payment1, "First payment should be created");
            assertEquals(PaymentStatus.COMPLETED, payment1.getStatus(),
                    "First payment should be COMPLETED");

            // Verify order is still CONFIRMED (not fully paid)
            Order afterPayment1 = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.CONFIRMED, afterPayment1.getStatus(),
                    "Order should remain CONFIRMED after partial payment");

            // Step 3: Process second partial payment (30.00)
            String idempotencyKey2 = UUID.randomUUID().toString();
            Payment payment2 = paymentService.processPayment(
                    order.getId(),
                    new BigDecimal("30.00"),
                    PaymentMethod.CARD,
                    idempotencyKey2
            );
            assertNotNull(payment2, "Second payment should be created");
            assertEquals(PaymentStatus.COMPLETED, payment2.getStatus(),
                    "Second payment should be COMPLETED");

            // Verify order is still CONFIRMED (total paid: 50.00, still less than 58.50)
            Order afterPayment2 = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.CONFIRMED, afterPayment2.getStatus(),
                    "Order should remain CONFIRMED after second partial payment");

            // Verify total payments so far
            List<Payment> paymentsAfter2 = paymentRepository.findByTenantIdAndOrderId(tenantId, order.getId());
            assertEquals(2, paymentsAfter2.size(), "Should have 2 payments");
            BigDecimal totalPaidSoFar = paymentsAfter2.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, new BigDecimal("50.00").compareTo(totalPaidSoFar),
                    "Total paid so far should be 50.00");

            // Step 4: Process final payment to complete the order (8.50)
            String idempotencyKey3 = UUID.randomUUID().toString();
            Payment payment3 = paymentService.processPayment(
                    order.getId(),
                    new BigDecimal("8.50"),
                    PaymentMethod.CASH,
                    idempotencyKey3
            );
            assertNotNull(payment3, "Third payment should be created");
            assertEquals(PaymentStatus.COMPLETED, payment3.getStatus(),
                    "Third payment should be COMPLETED");

            // Step 5: Verify order is now CLOSED (fully paid)
            Order closedOrder = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.CLOSED, closedOrder.getStatus(),
                    "Order should be CLOSED after full payment");

            // Verify all payments are recorded
            List<Payment> allPayments = paymentRepository.findByTenantIdAndOrderId(tenantId, order.getId());
            assertEquals(3, allPayments.size(), "Should have 3 payments");
            BigDecimal totalPaid = allPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, expectedTotal.compareTo(totalPaid),
                    "Total paid should equal order total");

        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testSplitBillCalculation() {
        // Setup: Create test data
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Split Bill Restaurant")
                .status("ACTIVE")
                .build();

        UUID siteId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantId)
                .name("Main Location")
                .build();

        UUID userId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantId)
                .username("waiter1")
                .role("WAITER")
                .build();

        UUID tableId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableNumber("T10")
                .status("OCCUPIED")
                .build();

        // Create catalog items
        UUID familyId = TestDataBuilder.family(jdbcTemplate)
                .tenantId(tenantId)
                .name("Food")
                .build();

        UUID subfamilyId = TestDataBuilder.subfamily(jdbcTemplate)
                .tenantId(tenantId)
                .familyId(familyId)
                .name("Appetizers")
                .build();

        UUID itemId = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantId)
                .subfamilyId(subfamilyId)
                .name("Nachos")
                .basePrice(new BigDecimal("15.00"))
                .available(true)
                .build();

        // Set tenant context
        TenantContext.setTenantId(tenantId);

        try {
            // Create order with total of 100.00
            Order order = orderService.createOrder(tableId, OrderType.DINE_IN, siteId, null, userId);
            
            // Add items to reach 100.00 total
            orderService.addOrderLine(order.getId(), itemId, 6, null, null, userId);
            orderService.addOrderLine(order.getId(), itemId, 1, null, null, userId);
            
            // Update order total manually for this test
            jdbcTemplate.update(
                "UPDATE orders SET total_amount = ? WHERE id = ?",
                new BigDecimal("100.00"),
                order.getId()
            );

            // Test split bill into 3 parts
            List<BigDecimal> splits = billingService.splitBill(order.getId(), 3);
            
            assertNotNull(splits, "Split amounts should not be null");
            assertEquals(3, splits.size(), "Should have 3 split amounts");
            
            // Verify split amounts: 33.33, 33.33, 33.34 (last one gets remainder)
            assertEquals(0, new BigDecimal("33.33").compareTo(splits.get(0)),
                    "First split should be 33.33");
            assertEquals(0, new BigDecimal("33.33").compareTo(splits.get(1)),
                    "Second split should be 33.33");
            assertEquals(0, new BigDecimal("33.34").compareTo(splits.get(2)),
                    "Third split should be 33.34 (gets remainder)");
            
            // Verify sum equals total
            BigDecimal sumOfSplits = splits.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, new BigDecimal("100.00").compareTo(sumOfSplits),
                    "Sum of splits should equal order total");

            // Test using split amounts for partial payments
            String idempotencyKey1 = UUID.randomUUID().toString();
            paymentService.processPayment(order.getId(), splits.get(0), PaymentMethod.CASH, idempotencyKey1);
            
            String idempotencyKey2 = UUID.randomUUID().toString();
            paymentService.processPayment(order.getId(), splits.get(1), PaymentMethod.CARD, idempotencyKey2);
            
            // Order should still be open after 2 of 3 payments
            Order afterTwoPayments = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.OPEN, afterTwoPayments.getStatus(),
                    "Order should remain OPEN after 2 of 3 split payments");
            
            // Complete with final payment
            String idempotencyKey3 = UUID.randomUUID().toString();
            paymentService.processPayment(order.getId(), splits.get(2), PaymentMethod.CASH, idempotencyKey3);
            
            // Order should now be closed
            Order closedOrder = orderRepository.findByIdAndTenantId(order.getId(), tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.CLOSED, closedOrder.getStatus(),
                    "Order should be CLOSED after all split payments completed");

        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testOverpaymentClosesOrder() {
        // Setup: Create test data
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Overpayment Test")
                .status("ACTIVE")
                .build();

        UUID siteId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantId)
                .name("Main Location")
                .build();

        UUID userId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantId)
                .username("cashier1")
                .role("CASHIER")
                .build();

        UUID tableId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableNumber("T15")
                .status("OCCUPIED")
                .build();

        // Create a simple order with known total
        UUID orderId = TestDataBuilder.order(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableId(tableId)
                .orderType("DINE_IN")
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("50.00"))
                .build();

        // Set tenant context
        TenantContext.setTenantId(tenantId);

        try {
            // Process payment that exceeds order total (customer pays with 100 bill)
            String idempotencyKey = UUID.randomUUID().toString();
            Payment payment = paymentService.processPayment(
                    orderId,
                    new BigDecimal("100.00"),
                    PaymentMethod.CASH,
                    idempotencyKey
            );
            
            assertNotNull(payment, "Payment should be created");
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus(),
                    "Payment should be COMPLETED");

            // Verify order is CLOSED (overpayment still closes the order)
            Order closedOrder = orderRepository.findByIdAndTenantId(orderId, tenantId)
                    .orElseThrow();
            assertEquals(OrderStatus.CLOSED, closedOrder.getStatus(),
                    "Order should be CLOSED after overpayment");

            // Verify change calculation
            BigDecimal change = paymentService.calculateChange(
                    new BigDecimal("50.00"),
                    new BigDecimal("100.00")
            );
            assertEquals(0, new BigDecimal("50.00").compareTo(change),
                    "Change should be 50.00");

        } finally {
            TenantContext.clear();
        }
    }
}
