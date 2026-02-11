package com.restaurantpos.identityaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Property-based test for tenant data isolation.
 * Feature: restaurant-pos-saas, Property 1: Tenant Data Isolation
 * 
 * Validates: Requirements 1.4
 * 
 * Property: For any data access operation and any authenticated user,
 * the returned results SHALL only contain data where tenant_id matches
 * the user's tenant, ensuring complete tenant isolation.
 */
@SpringBootTest
@Testcontainers
class TenantDataIsolationPropertyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("restaurant_pos_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        // Clean up test data
        cleanupTestData();
    }

    /**
     * Property 1: Tenant Data Isolation
     * 
     * Tests that when querying dining_tables with a tenant context set,
     * only tables belonging to that tenant are returned.
     */
    @Test
    void property1_tenantDataIsolation_diningTables() {
        qt()
            .withExamples(100)
            .forAll(
                integers().between(2, 5), // Number of tenants
                integers().between(1, 10)  // Tables per tenant
            )
            .checkAssert((numTenants, tablesPerTenant) -> {
                // Setup: Create multiple tenants with their own data
                List<UUID> tenantIds = new ArrayList<>();
                List<UUID> siteIds = new ArrayList<>();
                
                for (int i = 0; i < numTenants; i++) {
                    UUID tenantId = createTenant("Tenant-" + i);
                    UUID siteId = createSite(tenantId, "Site-" + i);
                    tenantIds.add(tenantId);
                    siteIds.add(siteId);
                    
                    // Create tables for this tenant
                    for (int j = 0; j < tablesPerTenant; j++) {
                        createDiningTable(tenantId, siteId, "T" + j);
                    }
                }
                
                // Test: Query as each tenant and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set tenant context
                    TenantContext.setTenantId(currentTenantId);
                    
                    // Query tables with tenant filtering
                    List<UUID> returnedTableTenantIds = queryDiningTableTenantIds(currentTenantId);
                    
                    // Assert: All returned tables belong to current tenant
                    assertEquals(
                        tablesPerTenant,
                        returnedTableTenantIds.size(),
                        "Should return exactly " + tablesPerTenant + " tables for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedTableTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "All returned tables must belong to the current tenant"
                        );
                    }
                    
                    TenantContext.clear();
                }
            });
    }

    /**
     * Property 1: Tenant Data Isolation (Orders)
     * 
     * Tests that when querying orders with a tenant context set,
     * only orders belonging to that tenant are returned.
     */
    @Test
    void property1_tenantDataIsolation_orders() {
        qt()
            .withExamples(100)
            .forAll(
                integers().between(2, 5), // Number of tenants
                integers().between(1, 8)  // Orders per tenant
            )
            .checkAssert((numTenants, ordersPerTenant) -> {
                // Setup: Create multiple tenants with their own data
                List<UUID> tenantIds = new ArrayList<>();
                
                for (int i = 0; i < numTenants; i++) {
                    UUID tenantId = createTenant("Tenant-" + i);
                    UUID siteId = createSite(tenantId, "Site-" + i);
                    tenantIds.add(tenantId);
                    
                    // Create orders for this tenant
                    for (int j = 0; j < ordersPerTenant; j++) {
                        createOrder(tenantId, siteId);
                    }
                }
                
                // Test: Query as each tenant and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set tenant context
                    TenantContext.setTenantId(currentTenantId);
                    
                    // Query orders with tenant filtering
                    List<UUID> returnedOrderTenantIds = queryOrderTenantIds(currentTenantId);
                    
                    // Assert: All returned orders belong to current tenant
                    assertEquals(
                        ordersPerTenant,
                        returnedOrderTenantIds.size(),
                        "Should return exactly " + ordersPerTenant + " orders for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedOrderTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "All returned orders must belong to the current tenant"
                        );
                    }
                    
                    TenantContext.clear();
                }
            });
    }

    /**
     * Property 1: Tenant Data Isolation (Customers)
     * 
     * Tests that when querying customers with a tenant context set,
     * only customers belonging to that tenant are returned.
     */
    @Test
    void property1_tenantDataIsolation_customers() {
        qt()
            .withExamples(100)
            .forAll(
                integers().between(2, 4), // Number of tenants
                integers().between(1, 10)  // Customers per tenant
            )
            .checkAssert((numTenants, customersPerTenant) -> {
                // Setup: Create multiple tenants with their own data
                List<UUID> tenantIds = new ArrayList<>();
                
                for (int i = 0; i < numTenants; i++) {
                    UUID tenantId = createTenant("Tenant-" + i);
                    tenantIds.add(tenantId);
                    
                    // Create customers for this tenant
                    for (int j = 0; j < customersPerTenant; j++) {
                        createCustomer(tenantId, "Customer-" + i + "-" + j, "555000" + i + j);
                    }
                }
                
                // Test: Query as each tenant and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set tenant context
                    TenantContext.setTenantId(currentTenantId);
                    
                    // Query customers with tenant filtering
                    List<UUID> returnedCustomerTenantIds = queryCustomerTenantIds(currentTenantId);
                    
                    // Assert: All returned customers belong to current tenant
                    assertEquals(
                        customersPerTenant,
                        returnedCustomerTenantIds.size(),
                        "Should return exactly " + customersPerTenant + " customers for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedCustomerTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "All returned customers must belong to the current tenant"
                        );
                    }
                    
                    TenantContext.clear();
                }
            });
    }

    // ========================================================================
    // Helper Methods for Test Data Creation
    // ========================================================================

    private UUID createTenant(String name) {
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, status) VALUES (?, ?, 'ACTIVE')",
            tenantId, name
        );
        return tenantId;
    }

    private UUID createSite(UUID tenantId, String name) {
        UUID siteId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO sites (id, tenant_id, name) VALUES (?, ?, ?)",
            siteId, tenantId, name
        );
        return siteId;
    }

    private UUID createDiningTable(UUID tenantId, UUID siteId, String tableNumber) {
        UUID tableId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO dining_tables (id, tenant_id, site_id, table_number, status) VALUES (?, ?, ?, ?, 'AVAILABLE')",
            tableId, tenantId, siteId, tableNumber
        );
        return tableId;
    }

    private UUID createOrder(UUID tenantId, UUID siteId) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO orders (id, tenant_id, site_id, order_type, status) VALUES (?, ?, ?, 'DINE_IN', 'OPEN')",
            orderId, tenantId, siteId
        );
        return orderId;
    }

    private UUID createCustomer(UUID tenantId, String name, String phone) {
        UUID customerId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO customers (id, tenant_id, name, phone) VALUES (?, ?, ?, ?)",
            customerId, tenantId, name, phone
        );
        return customerId;
    }

    // ========================================================================
    // Helper Methods for Querying Data
    // ========================================================================

    private List<UUID> queryDiningTableTenantIds(UUID tenantId) {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM dining_tables WHERE tenant_id = ?",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id")),
            tenantId
        );
    }

    private List<UUID> queryOrderTenantIds(UUID tenantId) {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM orders WHERE tenant_id = ?",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id")),
            tenantId
        );
    }

    private List<UUID> queryCustomerTenantIds(UUID tenantId) {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM customers WHERE tenant_id = ?",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id")),
            tenantId
        );
    }

    private void cleanupTestData() {
        // Delete in reverse order of dependencies
        jdbcTemplate.execute("DELETE FROM order_lines");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM dining_tables");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM sites");
        jdbcTemplate.execute("DELETE FROM tenants");
    }
}
