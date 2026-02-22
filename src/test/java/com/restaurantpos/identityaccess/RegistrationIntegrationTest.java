package com.restaurantpos.identityaccess;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.BaseIntegrationTest;
import com.restaurantpos.TestDataBuilder;
import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.entity.User;
import com.restaurantpos.identityaccess.model.Role;
import com.restaurantpos.identityaccess.repository.UserRepository;
import com.restaurantpos.identityaccess.service.AuthenticationService;

/**
 * Integration test for user registration functionality.
 */
@SpringBootTest
@Transactional
class RegistrationIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void register_withValidData_createsUserAndReturnsTokens() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String username = "newuser";
        String password = "SecurePassword123!";
        String email = "newuser@example.com";
        String role = "WAITER";
        
        // Act
        AuthResponse response = authenticationService.register(
                tenantId, username, password, email, role
        );
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(username, response.username());
        assertEquals(tenantId, response.tenantId());
        assertEquals(role, response.role());
        
        // Verify user was created in database
        User user = userRepository.findByTenantIdAndUsername(tenantId, username).orElse(null);
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(Role.WAITER, user.getRole());
        assertTrue(user.isActive());
        
        // Verify password was hashed (not stored in plain text)
        assertNotEquals(password, user.getPasswordHash());
    }
    
    @Test
    void register_withDuplicateUsername_throwsException() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String username = "existinguser";
        String password = "SecurePassword123!";
        
        // Create first user
        authenticationService.register(tenantId, username, password, "user1@example.com", "WAITER");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            authenticationService.register(tenantId, username, password, "user2@example.com", "CASHIER");
        });
        assertNotNull(exception);
    }
    
    @Test
    void register_withDuplicateEmail_throwsException() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String email = "duplicate@example.com";
        String password = "SecurePassword123!";
        
        // Create first user
        authenticationService.register(tenantId, "user1", password, email, "WAITER");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            authenticationService.register(tenantId, "user2", password, email, "CASHIER");
        });
        assertNotNull(exception);
    }
    
    @Test
    void register_withInvalidRole_throwsException() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String username = "testuser";
        String password = "SecurePassword123!";
        String invalidRole = "INVALID_ROLE";
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            authenticationService.register(tenantId, username, password, "test@example.com", invalidRole);
        });
        assertNotNull(exception);
    }
    
    @Test
    void register_withoutEmail_createsUser() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String username = "noemailuser";
        String password = "SecurePassword123!";
        
        // Act
        AuthResponse response = authenticationService.register(
                tenantId, username, password, null, "WAITER"
        );
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.accessToken());
        
        // Verify user was created
        User user = userRepository.findByTenantIdAndUsername(tenantId, username).orElse(null);
        assertNotNull(user);
        assertNull(user.getEmail());
    }
    
    @Test
    void register_thenLogin_succeeds() {
        // Arrange
        UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
                .name("Test Restaurant")
                .build();
        String username = "logintest";
        String password = "SecurePassword123!";
        
        // Register
        authenticationService.register(tenantId, username, password, "login@example.com", "WAITER");
        
        // Act - Login with registered credentials
        AuthResponse loginResponse = authenticationService.login(tenantId, username, password);
        
        // Assert
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.accessToken());
        assertEquals(username, loginResponse.username());
    }
    
    @Test
    void register_differentTenantsWithSameUsername_succeeds() {
        // Arrange
        UUID tenant1 = TestDataBuilder.tenant(jdbcTemplate)
                .name("Restaurant 1")
                .build();
        UUID tenant2 = TestDataBuilder.tenant(jdbcTemplate)
                .name("Restaurant 2")
                .build();
        String username = "sharedusername";
        String password = "SecurePassword123!";
        
        // Act - Register same username in different tenants
        AuthResponse response1 = authenticationService.register(
                tenant1, username, password, "user1@example.com", "WAITER"
        );
        AuthResponse response2 = authenticationService.register(
                tenant2, username, password, "user2@example.com", "CASHIER"
        );
        
        // Assert
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(tenant1, response1.tenantId());
        assertEquals(tenant2, response2.tenantId());
        
        // Verify both users exist
        assertTrue(userRepository.findByTenantIdAndUsername(tenant1, username).isPresent());
        assertTrue(userRepository.findByTenantIdAndUsername(tenant2, username).isPresent());
    }
}
