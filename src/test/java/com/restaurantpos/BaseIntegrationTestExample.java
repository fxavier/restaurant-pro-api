package com.restaurantpos;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Example integration test demonstrating usage of BaseIntegrationTest
 * and TestDataBuilder.
 * 
 * This test shows how to:
 * - Extend BaseIntegrationTest for automatic Testcontainers setup
 * - Use TestDataBuilder for creating test data with fluent API
 * - Verify data is properly persisted in the database
 */
class BaseIntegrationTestExample extends BaseIntegrationTest {

    @Test
    void demonstrateTestDataBuilderUsage() {
        // Create a tenant
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Demo Restaurant")
                .status("ACTIVE")
                .build();

        assertNotNull(tenantId);

        // Create a site for the tenant
        UUID siteId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantId)
                .name("Main Location")
                .address("123 Main St")
                .timezone("America/New_York")
                .build();

        assertNotNull(siteId);

        // Create a user
        UUID userId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantId)
                .username("waiter1")
                .email("waiter1@demo.com")
                .role("WAITER")
                .build();

        assertNotNull(userId);

        // Create a dining table
        UUID tableId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableNumber("T1")
                .area("Main Dining")
                .capacity(4)
                .status("AVAILABLE")
                .build();

        assertNotNull(tableId);

        // Create catalog structure
        UUID familyId = TestDataBuilder.family(jdbcTemplate)
                .tenantId(tenantId)
                .name("Beverages")
                .displayOrder(1)
                .build();

        UUID subfamilyId = TestDataBuilder.subfamily(jdbcTemplate)
                .tenantId(tenantId)
                .familyId(familyId)
                .name("Hot Drinks")
                .displayOrder(1)
                .build();

        UUID itemId = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantId)
                .subfamilyId(subfamilyId)
                .name("Coffee")
                .description("Fresh brewed coffee")
                .basePrice(new BigDecimal("3.50"))
                .available(true)
                .build();

        assertNotNull(itemId);

        // Create an order
        UUID orderId = TestDataBuilder.order(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .tableId(tableId)
                .orderType("DINE_IN")
                .status("OPEN")
                .build();

        assertNotNull(orderId);

        // Add order line
        UUID orderLineId = TestDataBuilder.orderLine(jdbcTemplate)
                .orderId(orderId)
                .itemId(itemId)
                .quantity(2)
                .unitPrice(new BigDecimal("3.50"))
                .status("PENDING")
                .build();

        assertNotNull(orderLineId);

        // Create a customer
        UUID customerId = TestDataBuilder.customer(jdbcTemplate)
                .tenantId(tenantId)
                .name("John Doe")
                .phone("5551234567")
                .address("456 Customer Ave")
                .build();

        assertNotNull(customerId);

        // Create a printer
        UUID printerId = TestDataBuilder.printer(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .name("Kitchen Printer")
                .ipAddress("192.168.1.100")
                .zone("Kitchen")
                .status("NORMAL")
                .build();

        assertNotNull(printerId);

        // Create a cash register
        UUID registerId = TestDataBuilder.cashRegister(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .registerNumber("REG-1")
                .status("ACTIVE")
                .build();

        assertNotNull(registerId);

        // Create a cash session
        UUID sessionId = TestDataBuilder.cashSession(jdbcTemplate)
                .tenantId(tenantId)
                .registerId(registerId)
                .employeeId(userId)
                .openingAmount(new BigDecimal("100.00"))
                .status("OPEN")
                .build();

        assertNotNull(sessionId);

        // Create a payment
        UUID paymentId = TestDataBuilder.payment(jdbcTemplate)
                .tenantId(tenantId)
                .orderId(orderId)
                .amount(new BigDecimal("7.00"))
                .paymentMethod("CASH")
                .status("COMPLETED")
                .build();

        assertNotNull(paymentId);

        // Create a fiscal document
        UUID fiscalDocId = TestDataBuilder.fiscalDocument(jdbcTemplate)
                .tenantId(tenantId)
                .siteId(siteId)
                .orderId(orderId)
                .documentType("RECEIPT")
                .documentNumber("REC-001")
                .amount(new BigDecimal("7.00"))
                .build();

        assertNotNull(fiscalDocId);

        // Verify data exists in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?",
                Integer.class,
                tenantId
        );
        assertEquals(1, count);

        // Verify cleanup will happen automatically after test
    }

    @Test
    void demonstrateAutomaticCleanup() {
        // Create some test data
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Cleanup Test")
                .build();

        // Verify it exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE id = ?",
                Integer.class,
                tenantId
        );
        assertEquals(1, count);

        // After this test completes, baseTearDown() will automatically
        // clean up all test data via cleanupTestData()
    }
}
