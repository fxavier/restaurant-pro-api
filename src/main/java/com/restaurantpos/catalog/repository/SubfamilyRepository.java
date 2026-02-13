package com.restaurantpos.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.catalog.entity.Subfamily;

/**
 * Repository for Subfamily entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 4.1, 4.2
 */
@Repository
public interface SubfamilyRepository extends JpaRepository<Subfamily, UUID> {
    
    /**
     * Finds all subfamilies for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of subfamilies
     */
    List<Subfamily> findByTenantId(UUID tenantId);
    
    /**
     * Finds all subfamilies for a specific family.
     * 
     * @param tenantId the tenant ID
     * @param familyId the family ID
     * @return list of subfamilies
     */
    List<Subfamily> findByTenantIdAndFamilyId(UUID tenantId, UUID familyId);
    
    /**
     * Finds all active subfamilies for a family, ordered by display order.
     * 
     * @param tenantId the tenant ID
     * @param familyId the family ID
     * @param active the active status
     * @return list of active subfamilies
     */
    List<Subfamily> findByTenantIdAndFamilyIdAndActiveOrderByDisplayOrder(UUID tenantId, UUID familyId, Boolean active);
    
    /**
     * Finds a subfamily by ID and tenant ID (for tenant isolation).
     * 
     * @param id the subfamily ID
     * @param tenantId the tenant ID
     * @return the subfamily if found
     */
    Optional<Subfamily> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a subfamily by name within a family.
     * 
     * @param tenantId the tenant ID
     * @param familyId the family ID
     * @param name the subfamily name
     * @return the subfamily if found
     */
    Optional<Subfamily> findByTenantIdAndFamilyIdAndName(UUID tenantId, UUID familyId, String name);
    
    /**
     * Checks if a subfamily name exists within a family.
     * 
     * @param tenantId the tenant ID
     * @param familyId the family ID
     * @param name the subfamily name
     * @return true if the subfamily name exists
     */
    boolean existsByTenantIdAndFamilyIdAndName(UUID tenantId, UUID familyId, String name);
    
    /**
     * Counts the number of subfamilies for a family.
     * 
     * @param tenantId the tenant ID
     * @param familyId the family ID
     * @return the count of subfamilies
     */
    long countByTenantIdAndFamilyId(UUID tenantId, UUID familyId);
}
