package com.restaurantpos.kitchenprinting.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.kitchenprinting.entity.Printer;
import com.restaurantpos.kitchenprinting.model.PrinterStatus;

/**
 * Repository for Printer entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 6.1, 6.3
 */
@Repository
public interface PrinterRepository extends JpaRepository<Printer, UUID> {
    
    /**
     * Finds all printers for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of printers
     */
    List<Printer> findByTenantId(UUID tenantId);
    
    /**
     * Finds all printers for a specific site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return list of printers
     */
    List<Printer> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);
    
    /**
     * Finds all printers for a site with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param status the printer status
     * @return list of printers
     */
    List<Printer> findByTenantIdAndSiteIdAndStatus(UUID tenantId, UUID siteId, PrinterStatus status);
    
    /**
     * Finds a printer by tenant, site, and name.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param name the printer name
     * @return the printer if found
     */
    Optional<Printer> findByTenantIdAndSiteIdAndName(UUID tenantId, UUID siteId, String name);
    
    /**
     * Finds a printer by ID and tenant ID (for tenant isolation).
     * 
     * @param id the printer ID
     * @param tenantId the tenant ID
     * @return the printer if found
     */
    Optional<Printer> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds all printers in a specific zone.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param zone the zone name
     * @return list of printers
     */
    List<Printer> findByTenantIdAndSiteIdAndZone(UUID tenantId, UUID siteId, String zone);
    
    /**
     * Checks if a printer name exists for a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param name the printer name
     * @return true if the printer name exists
     */
    boolean existsByTenantIdAndSiteIdAndName(UUID tenantId, UUID siteId, String name);
    
    /**
     * Counts the number of printers for a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return the count of printers
     */
    long countByTenantIdAndSiteId(UUID tenantId, UUID siteId);
}
