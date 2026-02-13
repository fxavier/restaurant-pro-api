package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.entity.User;
import com.restaurantpos.identityaccess.model.Role;
import com.restaurantpos.identityaccess.repository.UserRepository;
import com.restaurantpos.identityaccess.security.JwtTokenProvider;
import com.restaurantpos.identityaccess.service.AuthenticationService;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.arbitrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Property-based test for JWT token contains tenant.
 * Feature: restaurant-pos-saas, Property 3: JWT Token Contains Tenant
 * 
 * Validates: Requirements 1.6
 * 
 * Property: For any successful authentication, the issued JWT access token
 * SHALL contain a tenant_id claim matching the authenticated user's tenant.
 */
@SpringBootTest
@Testcontainers
class JwtTokenContainsTenantPropertyTest {

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
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        // Clean up test data
        cleanupTestData();
    }

    /**
     * Property 3: JWT Token Contains Tenant
     * 
     * Tests that for any successful authentication, the issued JWT access token
     * contains a tenant_id claim matching the authenticated user's tenant.
     */
    @Test
    void property3_jwtTokenContainsTenant() {
        qt()
            .withExamples(100)
            .forAll(
                arbitrary().pick(Role.values()),
                arbitrary().pick("user1", "user2", "user3", "admin", "cashier", "waiter"),
                arbitrary().pick("password123", "securePass!", "test1234")
            )
            .checkAssert((role, username, password) -> {
                // Setup: Create a tenant first (required for foreign key constraint)
                UUID tenantId = createTenant("Tenant-" + UUID.randomUUID());
                String hashedPassword = passwordEncoder.encode(password);
                
                User user = new User(
                    tenantId,
                    username + "-" + UUID.randomUUID(), // Make username unique
                    hashedPassword,
                    username + "@example.com",
                    role
                );
                userRepository.save(user);
                
                try {
                    // Execute: Authenticate and get tokens
                    AuthResponse authResponse = authenticationService.login(
                        tenantId,
                        user.getUsername(),
                        password
                    );
                    
                    // Assert: Access token is not null
                    assertNotNull(
                        authResponse.accessToken(),
                        "Access token should not be null"
                    );
                    
                    // Validate and decode the access token
                    Jwt jwt = jwtTokenProvider.validateToken(authResponse.accessToken());
                    
                    // Assert: JWT contains tenant_id claim
                    UUID extractedTenantId = jwtTokenProvider.extractTenantId(jwt);
                    assertNotNull(
                        extractedTenantId,
                        "JWT token must contain tenant_id claim"
                    );
                    
                    // Assert: tenant_id in JWT matches user's tenant
                    assertEquals(
                        tenantId,
                        extractedTenantId,
                        "JWT tenant_id claim must match the authenticated user's tenant"
                    );
                    
                    // Assert: tenant_id in AuthResponse matches user's tenant
                    assertEquals(
                        tenantId,
                        authResponse.tenantId(),
                        "AuthResponse tenant_id must match the authenticated user's tenant"
                    );
                    
                } finally {
                    // Cleanup happens in tearDown
                }
            });
    }

    /**
     * Property 3: JWT Token Contains Tenant (Refresh Token)
     * 
     * Tests that refresh tokens also contain the tenant_id claim.
     */
    @Test
    void property3_refreshTokenContainsTenant() {
        qt()
            .withExamples(100)
            .forAll(
                arbitrary().pick(Role.values()),
                arbitrary().pick("user1", "user2", "user3", "admin", "cashier"),
                arbitrary().pick("password123", "securePass!", "test1234")
            )
            .checkAssert((role, username, password) -> {
                // Setup: Create a tenant first (required for foreign key constraint)
                UUID tenantId = createTenant("Tenant-" + UUID.randomUUID());
                String hashedPassword = passwordEncoder.encode(password);
                
                User user = new User(
                    tenantId,
                    username + "-" + UUID.randomUUID(), // Make username unique
                    hashedPassword,
                    username + "@example.com",
                    role
                );
                userRepository.save(user);
                
                try {
                    // Execute: Authenticate and get tokens
                    AuthResponse authResponse = authenticationService.login(
                        tenantId,
                        user.getUsername(),
                        password
                    );
                    
                    // Assert: Refresh token is not null
                    assertNotNull(
                        authResponse.refreshToken(),
                        "Refresh token should not be null"
                    );
                    
                    // Validate and decode the refresh token
                    Jwt jwt = jwtTokenProvider.validateToken(authResponse.refreshToken());
                    
                    // Assert: JWT contains tenant_id claim
                    UUID extractedTenantId = jwtTokenProvider.extractTenantId(jwt);
                    assertNotNull(
                        extractedTenantId,
                        "Refresh token must contain tenant_id claim"
                    );
                    
                    // Assert: tenant_id in JWT matches user's tenant
                    assertEquals(
                        tenantId,
                        extractedTenantId,
                        "Refresh token tenant_id claim must match the authenticated user's tenant"
                    );
                    
                } finally {
                    // Cleanup happens in tearDown
                }
            });
    }

    /**
     * Property 3: JWT Token Contains Tenant (Direct Token Generation)
     * 
     * Tests that directly generated tokens contain the tenant_id claim.
     */
    @Test
    void property3_directTokenGenerationContainsTenant() {
        qt()
            .withExamples(100)
            .forAll(
                arbitrary().pick(Role.values()),
                arbitrary().pick("user1", "user2", "user3", "admin", "cashier", "waiter")
            )
            .checkAssert((role, username) -> {
                // Setup: Generate random IDs
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();
                
                // Execute: Generate access token directly
                String accessToken = jwtTokenProvider.generateAccessToken(
                    userId,
                    username,
                    tenantId,
                    role.name()
                );
                
                // Assert: Token is not null
                assertNotNull(accessToken, "Generated access token should not be null");
                
                // Validate and decode the token
                Jwt jwt = jwtTokenProvider.validateToken(accessToken);
                
                // Assert: JWT contains tenant_id claim
                UUID extractedTenantId = jwtTokenProvider.extractTenantId(jwt);
                assertNotNull(
                    extractedTenantId,
                    "JWT token must contain tenant_id claim"
                );
                
                // Assert: tenant_id in JWT matches the provided tenant
                assertEquals(
                    tenantId,
                    extractedTenantId,
                    "JWT tenant_id claim must match the provided tenant"
                );
                
                // Execute: Generate refresh token directly
                String refreshToken = jwtTokenProvider.generateRefreshToken(userId, tenantId);
                
                // Assert: Refresh token is not null
                assertNotNull(refreshToken, "Generated refresh token should not be null");
                
                // Validate and decode the refresh token
                Jwt refreshJwt = jwtTokenProvider.validateToken(refreshToken);
                
                // Assert: Refresh JWT contains tenant_id claim
                UUID extractedRefreshTenantId = jwtTokenProvider.extractTenantId(refreshJwt);
                assertNotNull(
                    extractedRefreshTenantId,
                    "Refresh token must contain tenant_id claim"
                );
                
                // Assert: tenant_id in refresh JWT matches the provided tenant
                assertEquals(
                    tenantId,
                    extractedRefreshTenantId,
                    "Refresh token tenant_id claim must match the provided tenant"
                );
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

    private void cleanupTestData() {
        // Delete in reverse order of dependencies
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");
    }
}
