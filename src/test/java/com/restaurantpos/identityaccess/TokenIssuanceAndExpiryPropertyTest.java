package com.restaurantpos.identityaccess;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Property-based test for token issuance and expiry.
 * Feature: restaurant-pos-saas, Property 5: Token Issuance and Expiry
 * 
 * Validates: Requirements 2.2
 * 
 * Property: For any valid login with correct credentials, the system SHALL issue
 * an access token with 15-minute expiry and a refresh token with 7-day expiry.
 */
@SpringBootTest
@Testcontainers
class TokenIssuanceAndExpiryPropertyTest {

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
        cleanupTestData();
    }

    /**
     * Property 5: Token Issuance and Expiry
     * 
     * Tests that for any valid login with correct credentials, the system issues
     * an access token with 15-minute expiry and a refresh token with 7-day expiry.
     */
    @Test
    void property5_tokenIssuanceAndExpiry() {
        qt()
            .withExamples(100)
            .forAll(
                arbitrary().pick(Role.values()),
                arbitrary().pick("user1", "user2", "user3", "admin", "cashier", "waiter"),
                arbitrary().pick("password123", "securePass!", "test1234")
            )
            .checkAssert((role, username, password) -> {
                // Setup: Create a tenant and user
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
                    Instant beforeLogin = Instant.now();
                    AuthResponse authResponse = authenticationService.login(
                        tenantId,
                        user.getUsername(),
                        password
                    );
                    Instant afterLogin = Instant.now();
                    
                    // Assert: Both tokens are issued
                    assertNotNull(
                        authResponse.accessToken(),
                        "Access token must be issued on successful login"
                    );
                    assertNotNull(
                        authResponse.refreshToken(),
                        "Refresh token must be issued on successful login"
                    );
                    
                    // Validate and decode the access token
                    Jwt accessJwt = jwtTokenProvider.validateToken(authResponse.accessToken());
                    
                    // Assert: Access token has expiry time
                    assertNotNull(
                        accessJwt.getExpiresAt(),
                        "Access token must have an expiry time"
                    );
                    
                    // Assert: Access token expires in approximately 15 minutes
                    Instant accessExpiry = accessJwt.getExpiresAt();
                    Duration accessTokenLifetime = Duration.between(beforeLogin, accessExpiry);
                    
                    // Allow 1 second tolerance for test execution time
                    long expectedAccessMinutes = 15;
                    long minAccessSeconds = (expectedAccessMinutes * 60) - 1;
                    long maxAccessSeconds = (expectedAccessMinutes * 60) + 1;
                    
                    assertTrue(
                        accessTokenLifetime.getSeconds() >= minAccessSeconds,
                        String.format(
                            "Access token lifetime must be at least %d minutes (got %d seconds)",
                            expectedAccessMinutes,
                            accessTokenLifetime.getSeconds()
                        )
                    );
                    assertTrue(
                        accessTokenLifetime.getSeconds() <= maxAccessSeconds,
                        String.format(
                            "Access token lifetime must be at most %d minutes (got %d seconds)",
                            expectedAccessMinutes,
                            accessTokenLifetime.getSeconds()
                        )
                    );
                    
                    // Validate and decode the refresh token
                    Jwt refreshJwt = jwtTokenProvider.validateToken(authResponse.refreshToken());
                    
                    // Assert: Refresh token has expiry time
                    assertNotNull(
                        refreshJwt.getExpiresAt(),
                        "Refresh token must have an expiry time"
                    );
                    
                    // Assert: Refresh token expires in approximately 7 days
                    Instant refreshExpiry = refreshJwt.getExpiresAt();
                    Duration refreshTokenLifetime = Duration.between(beforeLogin, refreshExpiry);
                    
                    // Allow 1 second tolerance for test execution time
                    long expectedRefreshDays = 7;
                    long minRefreshSeconds = (expectedRefreshDays * 24 * 60 * 60) - 1;
                    long maxRefreshSeconds = (expectedRefreshDays * 24 * 60 * 60) + 1;
                    
                    assertTrue(
                        refreshTokenLifetime.getSeconds() >= minRefreshSeconds,
                        String.format(
                            "Refresh token lifetime must be at least %d days (got %d seconds)",
                            expectedRefreshDays,
                            refreshTokenLifetime.getSeconds()
                        )
                    );
                    assertTrue(
                        refreshTokenLifetime.getSeconds() <= maxRefreshSeconds,
                        String.format(
                            "Refresh token lifetime must be at most %d days (got %d seconds)",
                            expectedRefreshDays,
                            refreshTokenLifetime.getSeconds()
                        )
                    );
                    
                    // Assert: Access token issued time is reasonable
                    assertNotNull(
                        accessJwt.getIssuedAt(),
                        "Access token must have an issued time"
                    );
                    assertTrue(
                        !accessJwt.getIssuedAt().isBefore(beforeLogin),
                        "Access token issued time must not be before login"
                    );
                    assertTrue(
                        !accessJwt.getIssuedAt().isAfter(afterLogin),
                        "Access token issued time must not be after login"
                    );
                    
                    // Assert: Refresh token issued time is reasonable
                    assertNotNull(
                        refreshJwt.getIssuedAt(),
                        "Refresh token must have an issued time"
                    );
                    assertTrue(
                        !refreshJwt.getIssuedAt().isBefore(beforeLogin),
                        "Refresh token issued time must not be before login"
                    );
                    assertTrue(
                        !refreshJwt.getIssuedAt().isAfter(afterLogin),
                        "Refresh token issued time must not be after login"
                    );
                    
                } finally {
                    // Cleanup happens in tearDown
                }
            });
    }

    /**
     * Property 5: Token Issuance and Expiry (Direct Token Generation)
     * 
     * Tests that directly generated tokens have the correct expiry times.
     */
    @Test
    void property5_directTokenGenerationExpiry() {
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
                
                // Execute: Generate access token
                Instant beforeGeneration = Instant.now();
                String accessToken = jwtTokenProvider.generateAccessToken(
                    userId,
                    username,
                    tenantId,
                    role.name()
                );
                Instant afterGeneration = Instant.now();
                
                // Validate and decode the access token
                Jwt accessJwt = jwtTokenProvider.validateToken(accessToken);
                
                // Assert: Access token expires in approximately 15 minutes
                assertNotNull(
                    accessJwt.getExpiresAt(),
                    "Access token must have an expiry time"
                );
                
                Instant accessExpiry = accessJwt.getExpiresAt();
                Duration accessTokenLifetime = Duration.between(beforeGeneration, accessExpiry);
                
                long expectedAccessMinutes = 15;
                long minAccessSeconds = (expectedAccessMinutes * 60) - 1;
                long maxAccessSeconds = (expectedAccessMinutes * 60) + 1;
                
                assertTrue(
                    accessTokenLifetime.getSeconds() >= minAccessSeconds,
                    String.format(
                        "Access token lifetime must be at least %d minutes (got %d seconds)",
                        expectedAccessMinutes,
                        accessTokenLifetime.getSeconds()
                    )
                );
                assertTrue(
                    accessTokenLifetime.getSeconds() <= maxAccessSeconds,
                    String.format(
                        "Access token lifetime must be at most %d minutes (got %d seconds)",
                        expectedAccessMinutes,
                        accessTokenLifetime.getSeconds()
                    )
                );
                
                // Execute: Generate refresh token
                String refreshToken = jwtTokenProvider.generateRefreshToken(userId, tenantId);
                
                // Validate and decode the refresh token
                Jwt refreshJwt = jwtTokenProvider.validateToken(refreshToken);
                
                // Assert: Refresh token expires in approximately 7 days
                assertNotNull(
                    refreshJwt.getExpiresAt(),
                    "Refresh token must have an expiry time"
                );
                
                Instant refreshExpiry = refreshJwt.getExpiresAt();
                Duration refreshTokenLifetime = Duration.between(beforeGeneration, refreshExpiry);
                
                long expectedRefreshDays = 7;
                long minRefreshSeconds = (expectedRefreshDays * 24 * 60 * 60) - 1;
                long maxRefreshSeconds = (expectedRefreshDays * 24 * 60 * 60) + 1;
                
                assertTrue(
                    refreshTokenLifetime.getSeconds() >= minRefreshSeconds,
                    String.format(
                        "Refresh token lifetime must be at least %d days (got %d seconds)",
                        expectedRefreshDays,
                        refreshTokenLifetime.getSeconds()
                    )
                );
                assertTrue(
                    refreshTokenLifetime.getSeconds() <= maxRefreshSeconds,
                    String.format(
                        "Refresh token lifetime must be at most %d days (got %d seconds)",
                        expectedRefreshDays,
                        refreshTokenLifetime.getSeconds()
                    )
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
