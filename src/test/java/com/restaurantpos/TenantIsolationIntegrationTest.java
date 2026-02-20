package com.restaurantpos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.restaurantpos.catalog.entity.Family;
import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.repository.FamilyRepository;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.catalog.repository.SubfamilyRepository;
import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.diningroom.entity.DiningTable;
import com.restaurantpos.diningroom.repository.DiningTableRepository;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.repository.OrderLineRepository;
import com.restaurantpos.orders.repository.OrderRepository;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;

/**
 * Integration test for tenant data isolation.
 * 
 * Tests that:
 * 1. Data created for Tenant A is not visible when querying as Tenant B
 * 2. Data created for Tenant B is not visible when querying as Tenant A
 * 3. Each tenant can only see their own data across all domain entities
 * 
 * This test validates the multi-tenancy isolation at the application level
 * by setting different tenant contexts and verifying data access is properly filtered.
 * 
 * Requirements: Testing Strategy, Requirement 1.4 (Tenant Data Isolation)
 */
class TenantIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private SubfamilyRepository subfamilyRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void testTenantDataIsolation() {
        // Create Tenant A
        UUID tenantAId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Restaurant A")
                .status("ACTIVE")
                .build();

        UUID siteAId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantAId)
                .name("Site A")
                .build();

        UUID userAId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantAId)
                .username("userA")
                .build();

        // Create Tenant B
        UUID tenantBId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Restaurant B")
                .status("ACTIVE")
                .build();

        UUID siteBId = TestDataBuilder.site(jdbcTemplate)
                .tenantId(tenantBId)
                .name("Site B")
                .build();

        UUID userBId = TestDataBuilder.user(jdbcTemplate)
                .tenantId(tenantBId)
                .username("userB")
                .build();

        // Create data for Tenant A
        UUID tableAId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantAId)
                .siteId(siteAId)
                .tableNumber("A-T1")
                .status("AVAILABLE")
                .build();

        UUID familyAId = TestDataBuilder.family(jdbcTemplate)
                .tenantId(tenantAId)
                .name("Family A")
                .build();

        UUID subfamilyAId = TestDataBuilder.subfamily(jdbcTemplate)
                .tenantId(tenantAId)
                .familyId(familyAId)
                .name("Subfamily A")
                .build();

        UUID itemAId = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantAId)
                .subfamilyId(subfamilyAId)
                .name("Item A")
                .basePrice(new BigDecimal("10.00"))
                .build();

        UUID orderAId = TestDataBuilder.order(jdbcTemplate)
                .tenantId(tenantAId)
                .siteId(siteAId)
                .tableId(tableAId)
                .orderType("DINE_IN")
                .status("OPEN")
                .build();

        UUID orderLineAId = TestDataBuilder.orderLine(jdbcTemplate)
                .orderId(orderAId)
                .itemId(itemAId)
                .quantity(1)
                .unitPrice(new BigDecimal("10.00"))
                .build();

        UUID customerAId = TestDataBuilder.customer(jdbcTemplate)
                .tenantId(tenantAId)
                .name("Customer A")
                .phone("1111111111")
                .build();

        UUID paymentAId = TestDataBuilder.payment(jdbcTemplate)
                .tenantId(tenantAId)
                .orderId(orderAId)
                .amount(new BigDecimal("10.00"))
                .paymentMethod("CASH")
                .build();

        // Create data for Tenant B
        UUID tableBId = TestDataBuilder.diningTable(jdbcTemplate)
                .tenantId(tenantBId)
                .siteId(siteBId)
                .tableNumber("B-T1")
                .status("AVAILABLE")
                .build();

        UUID familyBId = TestDataBuilder.family(jdbcTemplate)
                .tenantId(tenantBId)
                .name("Family B")
                .build();

        UUID subfamilyBId = TestDataBuilder.subfamily(jdbcTemplate)
                .tenantId(tenantBId)
                .familyId(familyBId)
                .name("Subfamily B")
                .build();

        UUID itemBId = TestDataBuilder.item(jdbcTemplate)
                .tenantId(tenantBId)
                .subfamilyId(subfamilyBId)
                .name("Item B")
                .basePrice(new BigDecimal("20.00"))
                .build();

        UUID orderBId = TestDataBuilder.order(jdbcTemplate)
                .tenantId(tenantBId)
                .siteId(siteBId)
                .tableId(tableBId)
                .orderType("DINE_IN")
                .status("OPEN")
                .build();

        UUID orderLineBId = TestDataBuilder.orderLine(jdbcTemplate)
                .orderId(orderBId)
                .itemId(itemBId)
                .quantity(2)
                .unitPrice(new BigDecimal("20.00"))
                .build();

        UUID customerBId = TestDataBuilder.customer(jdbcTemplate)
                .tenantId(tenantBId)
                .name("Customer B")
                .phone("2222222222")
                .build();

        UUID paymentBId = TestDataBuilder.payment(jdbcTemplate)
                .tenantId(tenantBId)
                .orderId(orderBId)
                .amount(new BigDecimal("40.00"))
                .paymentMethod("CARD")
                .build();

        // Test 1: Query as Tenant A - should only see Tenant A data
        TenantContext.setTenantId(tenantAId);
        try {
            // Verify Tenant A can see their own data
            List<DiningTable> tablesA = tableRepository.findByTenantIdAndSiteId(tenantAId, siteAId);
            assertEquals(1, tablesA.size(), "Tenant A should see 1 table");
            assertEquals("A-T1", tablesA.get(0).getTableNumber(), "Should be Tenant A's table");

            List<Family> familiesA = familyRepository.findByTenantId(tenantAId);
            assertEquals(1, familiesA.size(), "Tenant A should see 1 family");
            assertEquals("Family A", familiesA.get(0).getName(), "Should be Tenant A's family");

            List<Item> itemsA = itemRepository.findByTenantId(tenantAId);
            assertEquals(1, itemsA.size(), "Tenant A should see 1 item");
            assertEquals("Item A", itemsA.get(0).getName(), "Should be Tenant A's item");

            List<Order> ordersA = orderRepository.findByTenantIdAndSiteId(tenantAId, siteAId);
            assertEquals(1, ordersA.size(), "Tenant A should see 1 order");
            assertEquals(orderAId, ordersA.get(0).getId(), "Should be Tenant A's order");

            List<OrderLine> orderLinesA = orderLineRepository.findByOrderId(orderAId);
            assertEquals(1, orderLinesA.size(), "Tenant A should see 1 order line");
            assertEquals(orderLineAId, orderLinesA.get(0).getId(), "Should be Tenant A's order line");

            List<Customer> customersA = customerRepository.findByTenantId(tenantAId);
            assertEquals(1, customersA.size(), "Tenant A should see 1 customer");
            assertEquals("Customer A", customersA.get(0).getName(), "Should be Tenant A's customer");

            List<Payment> paymentsA = paymentRepository.findByTenantIdAndOrderId(tenantAId, orderAId);
            assertEquals(1, paymentsA.size(), "Tenant A should see 1 payment");
            assertEquals(paymentAId, paymentsA.get(0).getId(), "Should be Tenant A's payment");

            // Verify Tenant A CANNOT see Tenant B's data
            List<DiningTable> tablesBFromA = tableRepository.findByTenantIdAndSiteId(tenantAId, siteBId);
            assertTrue(tablesBFromA.isEmpty(), "Tenant A should not see Tenant B's tables");

            List<Order> ordersBFromA = orderRepository.findByTenantIdAndSiteId(tenantAId, siteBId);
            assertTrue(ordersBFromA.isEmpty(), "Tenant A should not see Tenant B's orders");

            // Try to access Tenant B's specific entities by ID - should return empty
            assertTrue(tableRepository.findByIdAndTenantId(tableBId, tenantAId).isEmpty(),
                    "Tenant A should not access Tenant B's table by ID");
            assertTrue(familyRepository.findByIdAndTenantId(familyBId, tenantAId).isEmpty(),
                    "Tenant A should not access Tenant B's family by ID");
            assertTrue(itemRepository.findByIdAndTenantId(itemBId, tenantAId).isEmpty(),
                    "Tenant A should not access Tenant B's item by ID");
            assertTrue(orderRepository.findByIdAndTenantId(orderBId, tenantAId).isEmpty(),
                    "Tenant A should not access Tenant B's order by ID");
            assertTrue(customerRepository.findByIdAndTenantId(customerBId, tenantAId).isEmpty(),
                    "Tenant A should not access Tenant B's customer by ID");

        } finally {
            TenantContext.clear();
        }

        // Test 2: Query as Tenant B - should only see Tenant B data
        TenantContext.setTenantId(tenantBId);
        try {
            // Verify Tenant B can see their own data
            List<DiningTable> tablesB = tableRepository.findByTenantIdAndSiteId(tenantBId, siteBId);
            assertEquals(1, tablesB.size(), "Tenant B should see 1 table");
            assertEquals("B-T1", tablesB.get(0).getTableNumber(), "Should be Tenant B's table");

            List<Family> familiesB = familyRepository.findByTenantId(tenantBId);
            assertEquals(1, familiesB.size(), "Tenant B should see 1 family");
            assertEquals("Family B", familiesB.get(0).getName(), "Should be Tenant B's family");

            List<Item> itemsB = itemRepository.findByTenantId(tenantBId);
            assertEquals(1, itemsB.size(), "Tenant B should see 1 item");
            assertEquals("Item B", itemsB.get(0).getName(), "Should be Tenant B's item");

            List<Order> ordersB = orderRepository.findByTenantIdAndSiteId(tenantBId, siteBId);
            assertEquals(1, ordersB.size(), "Tenant B should see 1 order");
            assertEquals(orderBId, ordersB.get(0).getId(), "Should be Tenant B's order");

            List<OrderLine> orderLinesB = orderLineRepository.findByOrderId(orderBId);
            assertEquals(1, orderLinesB.size(), "Tenant B should see 1 order line");
            assertEquals(orderLineBId, orderLinesB.get(0).getId(), "Should be Tenant B's order line");

            List<Customer> customersB = customerRepository.findByTenantId(tenantBId);
            assertEquals(1, customersB.size(), "Tenant B should see 1 customer");
            assertEquals("Customer B", customersB.get(0).getName(), "Should be Tenant B's customer");

            List<Payment> paymentsB = paymentRepository.findByTenantIdAndOrderId(tenantBId, orderBId);
            assertEquals(1, paymentsB.size(), "Tenant B should see 1 payment");
            assertEquals(paymentBId, paymentsB.get(0).getId(), "Should be Tenant B's payment");

            // Verify Tenant B CANNOT see Tenant A's data
            List<DiningTable> tablesAFromB = tableRepository.findByTenantIdAndSiteId(tenantBId, siteAId);
            assertTrue(tablesAFromB.isEmpty(), "Tenant B should not see Tenant A's tables");

            List<Order> ordersAFromB = orderRepository.findByTenantIdAndSiteId(tenantBId, siteAId);
            assertTrue(ordersAFromB.isEmpty(), "Tenant B should not see Tenant A's orders");

            // Try to access Tenant A's specific entities by ID - should return empty
            assertTrue(tableRepository.findByIdAndTenantId(tableAId, tenantBId).isEmpty(),
                    "Tenant B should not access Tenant A's table by ID");
            assertTrue(familyRepository.findByIdAndTenantId(familyAId, tenantBId).isEmpty(),
                    "Tenant B should not access Tenant A's family by ID");
            assertTrue(itemRepository.findByIdAndTenantId(itemAId, tenantBId).isEmpty(),
                    "Tenant B should not access Tenant A's item by ID");
            assertTrue(orderRepository.findByIdAndTenantId(orderAId, tenantBId).isEmpty(),
                    "Tenant B should not access Tenant A's order by ID");
            assertTrue(customerRepository.findByIdAndTenantId(customerAId, tenantBId).isEmpty(),
                    "Tenant B should not access Tenant A's customer by ID");

        } finally {
            TenantContext.clear();
        }

        // Verify both tenants' data exists in database (without tenant context)
        Integer totalTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dining_tables WHERE id IN (?, ?)",
                Integer.class,
                tableAId, tableBId
        );
        assertEquals(2, totalTables, "Both tenants' tables should exist in database");

        Integer totalOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE id IN (?, ?)",
                Integer.class,
                orderAId, orderBId
        );
        assertEquals(2, totalOrders, "Both tenants' orders should exist in database");

        Integer totalCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE id IN (?, ?)",
                Integer.class,
                customerAId, customerBId
        );
        assertEquals(2, totalCustomers, "Both tenants' customers should exist in database");

        // Test passed - tenant isolation is working correctly
        assertNotNull(tenantAId, "Tenant A should exist");
        assertNotNull(tenantBId, "Tenant B should exist");
    }
}
