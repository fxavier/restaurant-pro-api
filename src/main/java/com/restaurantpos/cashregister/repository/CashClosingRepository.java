package com.restaurantpos.cashregister.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.cashregister.entity.CashClosing;
import com.restaurantpos.cashregister.model.ClosingType;

/**
 * Repository for CashClosing entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 10.7, 10.8
 */
@Repository
public interface CashClosingRepository extends JpaRepository<CashClosing, UUID> {
    
    /**
     * Finds all cash closings for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of cash closings
     */
    List<CashClosing> findByTenantId(UUID tenantId);
    
    /**
     * Finds all cash closings for a tenant with a given closing type.
     * 
     * @param tenantId the tenant ID
     * @param closingType the closing type
     * @return list of cash closings
     */
    List<CashClosing> findByTenantIdAndClosingType(UUID tenantId, ClosingType closingType);
    
    /**
     * Finds all cash closings for a tenant within a date range.
     * 
     * @param tenantId the tenant ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash closings
     */
    List<CashClosing> findByTenantIdAndPeriodStartBetween(UUID tenantId, Instant startDate, Instant endDate);
    
    /**
     * Finds all cash closings for a tenant with a given closing type within a date range.
     * 
     * @param tenantId the tenant ID
     * @param closingType the closing type
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash closings
     */
    List<CashClosing> findByTenantIdAndClosingTypeAndPeriodStartBetween(UUID tenantId, ClosingType closingType, Instant startDate, Instant endDate);
    
    /**
     * Finds a cash closing by ID and tenant ID (for tenant isolation).
     * 
     * @param id the closing ID
     * @param tenantId the tenant ID
     * @return the cash closing if found
     */
    Optional<CashClosing> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds the most recent closing for a tenant and closing type.
     * 
     * @param tenantId the tenant ID
     * @param closingType the closing type
     * @return the most recent closing if found
     */
    Optional<CashClosing> findFirstByTenantIdAndClosingTypeOrderByClosedAtDesc(UUID tenantId, ClosingType closingType);
}
