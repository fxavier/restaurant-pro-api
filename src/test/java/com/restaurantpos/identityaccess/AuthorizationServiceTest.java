package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.entity.User;
import com.restaurantpos.identityaccess.exception.AuthorizationException;
import com.restaurantpos.identityaccess.model.Permission;
import com.restaurantpos.identityaccess.model.Role;
import com.restaurantpos.identityaccess.model.UserStatus;
import com.restaurantpos.identityaccess.repository.UserRepository;
import com.restaurantpos.identityaccess.service.AuthorizationService;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AuthorizationService.
 * Tests permission checking logic for different roles and permissions.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AuthorizationService authorizationService;
    
    private UUID tenantId;
    private UUID userId;
    private User adminUser;
    private User managerUser;
    private User cashierUser;
    private User waiterUser;
    private User kitchenStaffUser;
    private User inactiveUser;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        // Create users with different roles
        adminUser = new User(tenantId, "admin", "hash", "admin@test.com", Role.ADMIN);
        managerUser = new User(tenantId, "manager", "hash", "manager@test.com", Role.MANAGER);
        cashierUser = new User(tenantId, "cashier", "hash", "cashier@test.com", Role.CASHIER);
        waiterUser = new User(tenantId, "waiter", "hash", "waiter@test.com", Role.WAITER);
        kitchenStaffUser = new User(tenantId, "kitchen", "hash", "kitchen@test.com", Role.KITCHEN_STAFF);
        
        // Create inactive user
        inactiveUser = new User(tenantId, "inactive", "hash", "inactive@test.com", Role.WAITER);
        inactiveUser.setStatus(UserStatus.INACTIVE);
    }
    
    @Test
    void hasPermission_adminHasAllPermissions() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_AFTER_SUBTOTAL)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.REPRINT_DOCUMENT)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.REDIRECT_PRINTER)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.CLOSE_CASH)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_INVOICE)).isTrue();
    }
    
    @Test
    void hasPermission_managerHasAllPermissions() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(managerUser));
        
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_AFTER_SUBTOTAL)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.REPRINT_DOCUMENT)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.REDIRECT_PRINTER)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.CLOSE_CASH)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_INVOICE)).isTrue();
    }
    
    @Test
    void hasPermission_cashierHasLimitedPermissions() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(cashierUser));
        
        assertThat(authorizationService.hasPermission(userId, Permission.REPRINT_DOCUMENT)).isTrue();
        assertThat(authorizationService.hasPermission(userId, Permission.CLOSE_CASH)).isTrue();
        
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_AFTER_SUBTOTAL)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.REDIRECT_PRINTER)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_INVOICE)).isFalse();
    }
    
    @Test
    void hasPermission_waiterHasNoPermissions() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(waiterUser));
        
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_AFTER_SUBTOTAL)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.REPRINT_DOCUMENT)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.REDIRECT_PRINTER)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.CLOSE_CASH)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_INVOICE)).isFalse();
    }
    
    @Test
    void hasPermission_kitchenStaffCanRedirectPrinter() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(kitchenStaffUser));
        
        assertThat(authorizationService.hasPermission(userId, Permission.REDIRECT_PRINTER)).isTrue();
        
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_AFTER_SUBTOTAL)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.REPRINT_DOCUMENT)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.CLOSE_CASH)).isFalse();
        assertThat(authorizationService.hasPermission(userId, Permission.VOID_INVOICE)).isFalse();
    }
    
    @Test
    void hasPermission_throwsExceptionForNonExistentUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("User not found");
    }
    
    @Test
    void hasPermission_throwsExceptionForInactiveUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));
        
        assertThatThrownBy(() -> authorizationService.hasPermission(userId, Permission.APPLY_DISCOUNT))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("User is not active");
    }
    
    @Test
    void requirePermission_succeedsWhenUserHasPermission() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
        
        // Should not throw exception
        authorizationService.requirePermission(userId, Permission.VOID_AFTER_SUBTOTAL);
    }
    
    @Test
    void requirePermission_throwsExceptionWhenUserLacksPermission() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(waiterUser));
        
        assertThatThrownBy(() -> authorizationService.requirePermission(userId, Permission.APPLY_DISCOUNT))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("does not have permission")
            .hasMessageContaining("WAITER")
            .hasMessageContaining("APPLY_DISCOUNT");
    }
    
    @Test
    void getTenantContext_returnsCurrentTenantId() {
        TenantContext.setTenantId(tenantId);
        
        try {
            UUID result = authorizationService.getTenantContext();
            assertThat(result).isEqualTo(tenantId);
        } finally {
            TenantContext.clear();
        }
    }
    
    @Test
    void getTenantContext_throwsExceptionWhenNoTenantSet() {
        TenantContext.clear();
        
        assertThatThrownBy(() -> authorizationService.getTenantContext())
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("No tenant context set");
    }
}
