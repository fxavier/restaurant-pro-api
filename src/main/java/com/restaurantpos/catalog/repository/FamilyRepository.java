package com.restaurantpos.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.catalog.entity.Family;

/**
 * Repository for Family entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 4.1, 4.2
 */
@Repository
public interface FamilyRepository extends JpaRepository<Family, UUID> {
    
    /**
     * Finds all families for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of families
     */
    List<Family> findByTenantId(UUID tenantId);
    
    /**
     * Finds all active families for a tenant, ordered by display order.
     * 
     * @param tenantId the tenant ID
     * @param active the active status
     * @return list of active families
     */
    List<Family> findByTenantIdAndActiveOrderByDisplayOrder(UUID tenantId, Boolean active);
    
    /**
     * Finds a family by ID and tenant ID (for tenant isolation).
     * 
     * @param id the family ID
     * @param tenantId the tenant ID
     * @return the family if found
     */
    Optional<Family> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a family by name and tenant ID.
     * 
     * @param tenantId the tenant ID
     * @param name the family name
     * @return the family if found
     */
    Optional<Family> findByTenantIdAndName(UUID tenantId, String name);
    
    /**
     * Checks if a family name exists for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param name the family name
     * @return true if the family name exists
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    
    /**
     * Counts the number of families for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return the count of families
     */
    long countByTenantId(UUID tenantId);
}
