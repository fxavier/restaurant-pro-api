package com.restaurantpos.diningroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for DiningTable entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 3.2
 */
@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    
    /**
     * Finds all tables for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of tables
     */
    List<DiningTable> findByTenantId(UUID tenantId);
    
    /**
     * Finds all tables for a specific site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return list of tables
     */
    List<DiningTable> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);
    
    /**
     * Finds all tables for a site with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param status the table status
     * @return list of tables
     */
    List<DiningTable> findByTenantIdAndSiteIdAndStatus(UUID tenantId, UUID siteId, TableStatus status);
    
    /**
     * Finds a table by tenant, site, and table number.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param tableNumber the table number
     * @return the table if found
     */
    Optional<DiningTable> findByTenantIdAndSiteIdAndTableNumber(UUID tenantId, UUID siteId, String tableNumber);
    
    /**
     * Finds a table by ID and tenant ID (for tenant isolation).
     * 
     * @param id the table ID
     * @param tenantId the tenant ID
     * @return the table if found
     */
    Optional<DiningTable> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds all tables in a specific area.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param area the area name
     * @return list of tables
     */
    List<DiningTable> findByTenantIdAndSiteIdAndArea(UUID tenantId, UUID siteId, String area);
    
    /**
     * Checks if a table number exists for a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param tableNumber the table number
     * @return true if the table number exists
     */
    boolean existsByTenantIdAndSiteIdAndTableNumber(UUID tenantId, UUID siteId, String tableNumber);
    
    /**
     * Counts the number of tables for a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return the count of tables
     */
    long countByTenantIdAndSiteId(UUID tenantId, UUID siteId);
}
