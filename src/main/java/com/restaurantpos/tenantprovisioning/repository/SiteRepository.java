package com.restaurantpos.tenantprovisioning.repository;

import com.restaurantpos.tenantprovisioning.entity.Site;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Site entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 1.1, 1.8
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, UUID> {
    
    /**
     * Finds all sites for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of sites
     */
    List<Site> findByTenantId(UUID tenantId);
    
    /**
     * Finds a site by tenant ID and name.
     * 
     * @param tenantId the tenant ID
     * @param name the site name
     * @return the site if found
     */
    Optional<Site> findByTenantIdAndName(UUID tenantId, String name);
    
    /**
     * Finds a site by ID and tenant ID (for tenant isolation).
     * 
     * @param id the site ID
     * @param tenantId the tenant ID
     * @return the site if found
     */
    Optional<Site> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Checks if a site name exists for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param name the site name
     * @return true if the name exists
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    
    /**
     * Counts the number of sites for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return the count of sites
     */
    long countByTenantId(UUID tenantId);
}
