package com.restaurantpos.common.security;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.customers.service.CustomerService;
import com.restaurantpos.identityaccess.tenant.TenantContext;

/**
 * Integration tests to verify SQL injection prevention across the application.
 * 
 * This test verifies that:
 * 1. All queries use parameterized statements (JPA/JPQL)
 * 2. User input is properly sanitized
 * 3. SQL injection attempts are prevented
 * 
 * Requirements: 13.2 - Prevent SQL injection attacks
 */
@SpringBootTest
@Testcontainers
class SqlInjectionPreventionIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private UUID tenantId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        
        // Create tenant
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, subscription_plan, status, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            tenantId, "Test Tenant", "BASIC", "ACTIVE"
        );
        
        // Set tenant context
        TenantContext.setTenantId(tenantId);
        
        // Create test customers
        customerRepository.save(new Customer(tenantId, "John Doe", "1234567890", "123 Main St", null));
        customerRepository.save(new Customer(tenantId, "Jane Smith", "9876543210", "456 Oak Ave", null));
        customerRepository.save(new Customer(tenantId, "Bob Johnson", "5551234567", "789 Pine Rd", null));
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM tenants");
    }
    
    @Test
    void testPhoneSuffixSearch_PreventsSqlInjection_WithSelectStatement() {
        // Attempt SQL injection with SELECT statement
        String maliciousInput = "1234'; SELECT * FROM users--";
        
        // The service should sanitize the input and not execute the injection
        List<Customer> results = customerService.searchByPhoneSuffix(tenantId, maliciousInput);
        
        // Should return empty list (no match) rather than executing malicious SQL
        assertTrue(results.isEmpty(), "SQL injection attempt should not return results");
        
        // Verify database integrity - all customers should still exist
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(3, allCustomers.size(), "All customers should still exist");
    }
    
    @Test
    void testPhoneSuffixSearch_PreventsSqlInjection_WithDropTable() {
        // Attempt SQL injection with DROP TABLE
        String maliciousInput = "1234'; DROP TABLE customers--";
        
        // The service should sanitize the input
        List<Customer> results = customerService.searchByPhoneSuffix(tenantId, maliciousInput);
        
        // Should return empty list
        assertTrue(results.isEmpty(), "SQL injection attempt should not return results");
        
        // Verify table still exists by querying it
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(3, allCustomers.size(), "Customers table should not be dropped");
    }
    
    @Test
    void testPhoneSuffixSearch_PreventsSqlInjection_WithUnion() {
        // Attempt SQL injection with UNION
        String maliciousInput = "1234' UNION SELECT id, name, phone FROM users--";
        
        // The service should sanitize the input
        List<Customer> results = customerService.searchByPhoneSuffix(tenantId, maliciousInput);
        
        // Should return empty list
        assertTrue(results.isEmpty(), "SQL injection attempt should not return results");
    }
    
    @Test
    void testPhoneSuffixSearch_PreventsSqlInjection_WithWildcards() {
        // Attempt to use SQL wildcards to match all records
        String maliciousInput = "%";
        
        // The service should sanitize wildcards and throw exception
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhoneSuffix(tenantId, maliciousInput),
            "Wildcard-only input should be rejected");
    }
    
    @Test
    void testPhoneSuffixSearch_PreventsSqlInjection_WithMultipleWildcards() {
        // Attempt to use multiple wildcards
        String maliciousInput = "%%__%%";
        
        // Should throw exception for empty suffix after sanitization
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhoneSuffix(tenantId, maliciousInput),
            "Multiple wildcards should be rejected");
    }
    
    @Test
    void testPhoneSuffixSearch_WorksCorrectly_WithLegitimateInput() {
        // Test that legitimate searches still work after sanitization
        List<Customer> results = customerService.searchByPhoneSuffix(tenantId, "7890");
        
        assertEquals(1, results.size(), "Should find one customer");
        assertEquals("John Doe", results.get(0).getName());
    }
    
    @Test
    void testPhoneSuffixSearch_WorksCorrectly_WithPartialMatch() {
        // Test partial match - only "5551234567" ends with "567"
        List<Customer> results = customerService.searchByPhoneSuffix(tenantId, "567");
        
        // Should match "5551234567"
        assertEquals(1, results.size(), "Should find one customer");
        assertTrue(results.stream().anyMatch(c -> c.getPhone().equals("5551234567")));
    }
    
    @Test
    void testFullPhoneSearch_PreventsSqlInjection() {
        // Attempt SQL injection in full phone search
        String maliciousInput = "1234567890'; DROP TABLE customers--";
        
        // The repository uses parameterized queries, so this should be safe
        List<Customer> results = customerService.searchByPhone(tenantId, maliciousInput);
        
        // Should return empty list (no match)
        assertTrue(results.isEmpty(), "SQL injection attempt should not return results");
        
        // Verify table still exists
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(3, allCustomers.size(), "Customers table should not be affected");
    }
    
    @Test
    void testCreateCustomer_PreventsSqlInjection_InName() {
        // Attempt SQL injection in customer name
        String maliciousName = "John'; DROP TABLE customers--";
        
        // Create customer with malicious name
        Customer customer = customerService.createCustomer(
            tenantId, maliciousName, "1112223333", "Test Address", null
        );
        
        // Customer should be created with the literal string (not executed as SQL)
        assertNotNull(customer.getId());
        assertEquals(maliciousName, customer.getName());
        
        // Verify table still exists
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(4, allCustomers.size(), "Customer should be created, table should exist");
    }
    
    @Test
    void testCreateCustomer_PreventsSqlInjection_InPhone() {
        // Attempt SQL injection in phone number
        // Note: The database has a VARCHAR(20) constraint on phone, so very long
        // injection strings will be rejected by the database itself (defense in depth)
        String maliciousPhone = "123'; DELETE--";
        
        // Create customer with malicious phone (short enough to fit in VARCHAR(20))
        Customer customer = customerService.createCustomer(
            tenantId, "Test User", maliciousPhone, "Test Address", null
        );
        
        // Customer should be created with the literal string (not executed as SQL)
        assertNotNull(customer.getId());
        assertEquals(maliciousPhone, customer.getPhone());
        
        // Verify all customers still exist (no deletion occurred)
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(4, allCustomers.size(), "All customers should still exist");
    }
    
    @Test
    void testParameterizedQueries_PreventSqlInjection() {
        // This test verifies that JPA/JPQL queries are parameterized
        // by attempting various SQL injection patterns
        
        String[] injectionAttempts = {
            "'; DROP TABLE customers--",
            "' OR '1'='1",
            "' UNION SELECT * FROM users--",
            "'; DELETE FROM customers WHERE '1'='1'--",
            "1' AND 1=1--",
            "' OR 1=1#"
        };
        
        for (String attempt : injectionAttempts) {
            // Try injection in phone search
            List<Customer> results = customerRepository.findByTenantIdAndPhone(tenantId, attempt);
            assertTrue(results.isEmpty(), 
                "SQL injection attempt should not return results: " + attempt);
        }
        
        // Verify all customers still exist after all attempts
        List<Customer> allCustomers = customerRepository.findByTenantId(tenantId);
        assertEquals(3, allCustomers.size(), 
            "All customers should still exist after injection attempts");
    }
}
