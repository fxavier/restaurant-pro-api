package com.restaurantpos.cashregister.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.model.CashRegisterStatus;

/**
 * Repository for CashRegister entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 10.1
 */
@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {
    
    /**
     * Finds all cash registers for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of cash registers
     */
    List<CashRegister> findByTenantId(UUID tenantId);
    
    /**
     * Finds all cash registers for a specific site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return list of cash registers
     */
    List<CashRegister> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);
    
    /**
     * Finds all cash registers for a site with a given status.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param status the register status
     * @return list of cash registers
     */
    List<CashRegister> findByTenantIdAndSiteIdAndStatus(UUID tenantId, UUID siteId, CashRegisterStatus status);
    
    /**
     * Finds a cash register by ID and tenant ID (for tenant isolation).
     * 
     * @param id the register ID
     * @param tenantId the tenant ID
     * @return the cash register if found
     */
    Optional<CashRegister> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a cash register by register number within a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param registerNumber the register number
     * @return the cash register if found
     */
    Optional<CashRegister> findByTenantIdAndSiteIdAndRegisterNumber(UUID tenantId, UUID siteId, String registerNumber);
    
    /**
     * Checks if a cash register exists with the given register number in a site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param registerNumber the register number
     * @return true if register exists
     */
    boolean existsByTenantIdAndSiteIdAndRegisterNumber(UUID tenantId, UUID siteId, String registerNumber);
}
