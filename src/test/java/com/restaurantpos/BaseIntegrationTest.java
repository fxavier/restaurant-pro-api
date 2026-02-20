package com.restaurantpos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.restaurantpos.identityaccess.tenant.TenantContext;

/**
 * Base class for integration tests using Testcontainers.
 * 
 * Provides:
 * - Shared PostgreSQL container for all integration tests
 * - Automatic tenant context cleanup
 * - Database cleanup utilities
 * - Common test configuration
 * 
 * Usage: Extend this class in your integration tests to get automatic
 * Testcontainers setup and cleanup.
 */
@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("restaurant_pos_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void baseSetUp() {
        TenantContext.clear();
    }

    @AfterEach
    void baseTearDown() {
        TenantContext.clear();
        cleanupTestData();
    }

    /**
     * Clean up all test data from the database.
     * Override this method in subclasses to add custom cleanup logic.
     */
    protected void cleanupTestData() {
        // Delete in reverse order of dependencies
        jdbcTemplate.execute("DELETE FROM cash_movements");
        jdbcTemplate.execute("DELETE FROM cash_closings");
        jdbcTemplate.execute("DELETE FROM cash_sessions");
        jdbcTemplate.execute("DELETE FROM cash_registers");
        jdbcTemplate.execute("DELETE FROM fiscal_documents");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM print_jobs");
        jdbcTemplate.execute("DELETE FROM printers");
        jdbcTemplate.execute("DELETE FROM discounts");
        jdbcTemplate.execute("DELETE FROM consumptions");
        jdbcTemplate.execute("DELETE FROM order_lines");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM items");
        jdbcTemplate.execute("DELETE FROM subfamilies");
        jdbcTemplate.execute("DELETE FROM families");
        jdbcTemplate.execute("DELETE FROM dining_tables");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM blacklist_entries");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM sites");
        jdbcTemplate.execute("DELETE FROM tenants");
    }
}
