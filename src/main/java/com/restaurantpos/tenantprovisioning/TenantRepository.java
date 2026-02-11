package com.restaurantpos.tenantprovisioning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Tenant entity.
 * 
 * Requirements: 1.1, 1.8
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    /**
     * Finds a tenant by name.
     * 
     * @param name the tenant name
     * @return the tenant if found
     */
    Optional<Tenant> findByName(String name);
    
    /**
     * Finds all tenants with a specific status.
     * 
     * @param status the tenant status
     * @return list of tenants
     */
    List<Tenant> findByStatus(TenantStatus status);
    
    /**
     * Finds all tenants with a specific subscription plan.
     * 
     * @param subscriptionPlan the subscription plan
     * @return list of tenants
     */
    List<Tenant> findBySubscriptionPlan(String subscriptionPlan);
    
    /**
     * Checks if a tenant name already exists.
     * 
     * @param name the tenant name
     * @return true if the name exists
     */
    boolean existsByName(String name);
}
