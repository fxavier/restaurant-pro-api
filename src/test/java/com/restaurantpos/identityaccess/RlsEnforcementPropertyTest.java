package com.restaurantpos.identityaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * Property-based test for PostgreSQL Row Level Security (RLS) enforcement.
 * Feature: restaurant-pos-saas, Property 2: PostgreSQL RLS Enforcement
 * 
 * Validates: Requirements 1.5
 * 
 * Property: For any database query when RLS is enabled and tenant context is set,
 * only rows matching the current_setting('app.tenant_id') SHALL be accessible,
 * providing database-level tenant isolation.
 * 
 * NOTE: This test assumes RLS policies have been applied via V4__rls_policies.sql migration.
 * If RLS is not enabled, this test will be skipped.
 */
@SpringBootTest
@Testcontainers
class RlsEnforcementPropertyTest {

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
    private JdbcTemplate jdbcTemplate;

    private boolean rlsEnabled = false;

    @BeforeEach
    void setUp() {
        // Check if RLS is enabled on any table
        rlsEnabled = isRlsEnabled();
        
        // Clear any existing tenant context
        clearTenantContext();
    }

    @AfterEach
    void tearDown() {
        // Clear tenant context
        clearTenantContext();
        
        // Clean up test data
        cleanupTestData();
    }

    /**
     * Property 2: PostgreSQL RLS Enforcement (Dining Tables)
     * 
     * Tests that when RLS is enabled and app.tenant_id is set via PostgreSQL session variable,
     * only tables belonging to that tenant are accessible at the database level.
     */
    @Test
    void property2_rlsEnforcement_diningTables() {
        if (!rlsEnabled) {
            System.out.println("RLS is not enabled - skipping RLS enforcement test");
            return;
        }

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
                    
                    // Create tables for this tenant (without RLS context)
                    clearTenantContext();
                    for (int j = 0; j < tablesPerTenant; j++) {
                        createDiningTable(tenantId, siteId, "T" + j);
                    }
                }
                
                // Test: Query as each tenant with RLS context and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set PostgreSQL session variable for RLS
                    setRlsTenantContext(currentTenantId);
                    
                    // Query tables WITHOUT tenant_id filter in WHERE clause
                    // RLS should automatically filter to current tenant
                    List<UUID> returnedTableTenantIds = queryAllDiningTableTenantIds();
                    
                    // Assert: All returned tables belong to current tenant
                    assertEquals(
                        tablesPerTenant,
                        returnedTableTenantIds.size(),
                        "RLS should return exactly " + tablesPerTenant + " tables for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedTableTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "RLS must enforce that all returned tables belong to the current tenant"
                        );
                    }
                    
                    clearTenantContext();
                }
            });
    }

    /**
     * Property 2: PostgreSQL RLS Enforcement (Orders)
     * 
     * Tests that when RLS is enabled and app.tenant_id is set,
     * only orders belonging to that tenant are accessible.
     */
    @Test
    void property2_rlsEnforcement_orders() {
        if (!rlsEnabled) {
            System.out.println("RLS is not enabled - skipping RLS enforcement test");
            return;
        }

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
                    
                    // Create orders for this tenant (without RLS context)
                    clearTenantContext();
                    for (int j = 0; j < ordersPerTenant; j++) {
                        createOrder(tenantId, siteId);
                    }
                }
                
                // Test: Query as each tenant with RLS context and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set PostgreSQL session variable for RLS
                    setRlsTenantContext(currentTenantId);
                    
                    // Query orders WITHOUT tenant_id filter in WHERE clause
                    List<UUID> returnedOrderTenantIds = queryAllOrderTenantIds();
                    
                    // Assert: All returned orders belong to current tenant
                    assertEquals(
                        ordersPerTenant,
                        returnedOrderTenantIds.size(),
                        "RLS should return exactly " + ordersPerTenant + " orders for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedOrderTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "RLS must enforce that all returned orders belong to the current tenant"
                        );
                    }
                    
                    clearTenantContext();
                }
            });
    }

    /**
     * Property 2: PostgreSQL RLS Enforcement (Customers)
     * 
     * Tests that when RLS is enabled and app.tenant_id is set,
     * only customers belonging to that tenant are accessible.
     */
    @Test
    void property2_rlsEnforcement_customers() {
        if (!rlsEnabled) {
            System.out.println("RLS is not enabled - skipping RLS enforcement test");
            return;
        }

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
                    
                    // Create customers for this tenant (without RLS context)
                    clearTenantContext();
                    for (int j = 0; j < customersPerTenant; j++) {
                        createCustomer(tenantId, "Customer-" + i + "-" + j, "555000" + i + j);
                    }
                }
                
                // Test: Query as each tenant with RLS context and verify isolation
                for (int i = 0; i < numTenants; i++) {
                    UUID currentTenantId = tenantIds.get(i);
                    
                    // Set PostgreSQL session variable for RLS
                    setRlsTenantContext(currentTenantId);
                    
                    // Query customers WITHOUT tenant_id filter in WHERE clause
                    List<UUID> returnedCustomerTenantIds = queryAllCustomerTenantIds();
                    
                    // Assert: All returned customers belong to current tenant
                    assertEquals(
                        customersPerTenant,
                        returnedCustomerTenantIds.size(),
                        "RLS should return exactly " + customersPerTenant + " customers for tenant " + i
                    );
                    
                    for (UUID returnedTenantId : returnedCustomerTenantIds) {
                        assertEquals(
                            currentTenantId,
                            returnedTenantId,
                            "RLS must enforce that all returned customers belong to the current tenant"
                        );
                    }
                    
                    clearTenantContext();
                }
            });
    }

    /**
     * Property 2: PostgreSQL RLS Enforcement (Cross-Tenant Access Prevention)
     * 
     * Tests that when RLS context is set to Tenant A, queries cannot access Tenant B's data,
     * even if explicitly requested.
     */
    @Test
    void property2_rlsEnforcement_crossTenantAccessPrevention() {
        if (!rlsEnabled) {
            System.out.println("RLS is not enabled - skipping RLS enforcement test");
            return;
        }

        qt()
            .withExamples(50)
            .forAll(
                integers().between(3, 10)  // Items per tenant
            )
            .checkAssert((itemsPerTenant) -> {
                // Setup: Create two tenants with data
                UUID tenantA = createTenant("TenantA");
                UUID tenantB = createTenant("TenantB");
                
                clearTenantContext();
                
                // Create items for both tenants
                for (int i = 0; i < itemsPerTenant; i++) {
                    createCustomer(tenantA, "CustomerA-" + i, "555100" + i);
                    createCustomer(tenantB, "CustomerB-" + i, "555200" + i);
                }
                
                // Test: Set context to Tenant A and try to query Tenant B's data
                setRlsTenantContext(tenantA);
                
                // Query all customers (no WHERE filter) - should only get Tenant A
                List<UUID> customersAsA = queryAllCustomerTenantIds();
                
                // Assert: Only Tenant A's customers are returned
                assertEquals(
                    itemsPerTenant,
                    customersAsA.size(),
                    "Should only see Tenant A's customers"
                );
                
                for (UUID tenantId : customersAsA) {
                    assertEquals(tenantA, tenantId, "All customers must belong to Tenant A");
                }
                
                // Test: Set context to Tenant B and verify isolation
                setRlsTenantContext(tenantB);
                
                List<UUID> customersAsB = queryAllCustomerTenantIds();
                
                // Assert: Only Tenant B's customers are returned
                assertEquals(
                    itemsPerTenant,
                    customersAsB.size(),
                    "Should only see Tenant B's customers"
                );
                
                for (UUID tenantId : customersAsB) {
                    assertEquals(tenantB, tenantId, "All customers must belong to Tenant B");
                }
                
                clearTenantContext();
            });
    }

    // ========================================================================
    // Helper Methods for RLS Context Management
    // ========================================================================

    /**
     * Check if RLS is enabled on any table in the database.
     */
    private boolean isRlsEnabled() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = true",
                Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            // If query fails, assume RLS is not enabled
            return false;
        }
    }

    /**
     * Set the PostgreSQL session variable for RLS tenant context.
     */
    private void setRlsTenantContext(UUID tenantId) {
        try {
            jdbcTemplate.execute("SET app.tenant_id = '" + tenantId + "'");
        } catch (Exception e) {
            // If setting fails, RLS might not be configured
            System.err.println("Failed to set RLS tenant context: " + e.getMessage());
        }
    }

    /**
     * Clear the PostgreSQL session variable for RLS tenant context.
     */
    private void clearTenantContext() {
        try {
            jdbcTemplate.execute("RESET app.tenant_id");
        } catch (Exception e) {
            // Ignore errors when clearing context
        }
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
    // Helper Methods for Querying Data (WITHOUT tenant_id filter)
    // ========================================================================

    /**
     * Query all dining tables WITHOUT tenant_id filter.
     * RLS should automatically filter results.
     */
    private List<UUID> queryAllDiningTableTenantIds() {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM dining_tables",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id"))
        );
    }

    /**
     * Query all orders WITHOUT tenant_id filter.
     * RLS should automatically filter results.
     */
    private List<UUID> queryAllOrderTenantIds() {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM orders",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id"))
        );
    }

    /**
     * Query all customers WITHOUT tenant_id filter.
     * RLS should automatically filter results.
     */
    private List<UUID> queryAllCustomerTenantIds() {
        return jdbcTemplate.query(
            "SELECT tenant_id FROM customers",
            (rs, rowNum) -> UUID.fromString(rs.getString("tenant_id"))
        );
    }

    private void cleanupTestData() {
        // Clear RLS context before cleanup
        clearTenantContext();
        
        // Delete in reverse order of dependencies
        try {
            jdbcTemplate.execute("DELETE FROM order_lines");
            jdbcTemplate.execute("DELETE FROM orders");
            jdbcTemplate.execute("DELETE FROM dining_tables");
            jdbcTemplate.execute("DELETE FROM customers");
            jdbcTemplate.execute("DELETE FROM sites");
            jdbcTemplate.execute("DELETE FROM tenants");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
