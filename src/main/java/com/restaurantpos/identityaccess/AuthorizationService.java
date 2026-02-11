package com.restaurantpos.identityaccess;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling authorization and permission checks.
 * Provides methods to verify user permissions for sensitive operations.
 * 
 * Requirements: 2.5
 */
@Service
@Transactional(readOnly = true)
public class AuthorizationService {
    
    private final UserRepository userRepository;
    
    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Checks if a user has a specific permission.
     * 
     * @param userId the user ID to check
     * @param permission the permission to verify
     * @return true if the user has the permission, false otherwise
     * @throws AuthorizationException if the user is not found or inactive
     */
    public boolean hasPermission(UUID userId, Permission permission) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthorizationException("User not found: " + userId));
        
        if (!user.isActive()) {
            throw new AuthorizationException("User is not active: " + userId);
        }
        
        return user.getRole().hasPermission(permission);
    }
    
    /**
     * Requires that the current user has a specific permission.
     * Throws an exception if the permission is not granted.
     * 
     * @param userId the user ID to check
     * @param permission the required permission
     * @throws AuthorizationException if the user does not have the permission
     */
    public void requirePermission(UUID userId, Permission permission) {
        if (!hasPermission(userId, permission)) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthorizationException("User not found: " + userId));
            throw new AuthorizationException(
                String.format("User %s with role %s does not have permission: %s", 
                    user.getUsername(), user.getRole(), permission)
            );
        }
    }
    
    /**
     * Gets the current tenant ID from the tenant context.
     * 
     * @return the current tenant ID
     * @throws AuthorizationException if no tenant context is set
     */
    public UUID getTenantContext() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AuthorizationException("No tenant context set for current request");
        }
        return tenantId;
    }
}
