package com.restaurantpos.identityaccess.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.identityaccess.entity.User;
import com.restaurantpos.identityaccess.model.Role;
import com.restaurantpos.identityaccess.model.UserStatus;

/**
 * Repository for User entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirement: 2.1
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Finds a user by tenant ID and username.
     * 
     * @param tenantId the tenant ID
     * @param username the username
     * @return the user if found
     */
    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);
    
    /**
     * Finds all users for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of users
     */
    List<User> findByTenantId(UUID tenantId);
    
    /**
     * Finds all users for a tenant with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param status the user status
     * @return list of users
     */
    List<User> findByTenantIdAndStatus(UUID tenantId, UserStatus status);
    
    /**
     * Finds all users for a tenant with a specific role.
     * 
     * @param tenantId the tenant ID
     * @param role the user role
     * @return list of users
     */
    List<User> findByTenantIdAndRole(UUID tenantId, Role role);
    
    /**
     * Checks if a username exists for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param username the username
     * @return true if the username exists
     */
    boolean existsByTenantIdAndUsername(UUID tenantId, String username);
    
    /**
     * Finds a user by tenant ID and email.
     * 
     * @param tenantId the tenant ID
     * @param email the email address
     * @return the user if found
     */
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);
}
