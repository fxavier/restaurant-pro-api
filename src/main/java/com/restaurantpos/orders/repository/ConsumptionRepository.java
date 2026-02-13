package com.restaurantpos.orders.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.orders.entity.Consumption;

/**
 * Repository for Consumption entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 5.4
 */
@Repository
public interface ConsumptionRepository extends JpaRepository<Consumption, UUID> {
    
    /**
     * Finds all consumptions for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of consumptions
     */
    List<Consumption> findByTenantId(UUID tenantId);
    
    /**
     * Finds all consumptions for a specific order line.
     * 
     * @param tenantId the tenant ID
     * @param orderLineId the order line ID
     * @return list of consumptions
     */
    List<Consumption> findByTenantIdAndOrderLineId(UUID tenantId, UUID orderLineId);
    
    /**
     * Finds a consumption by ID and tenant ID (for tenant isolation).
     * 
     * @param id the consumption ID
     * @param tenantId the tenant ID
     * @return the consumption if found
     */
    Optional<Consumption> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds all consumptions confirmed within a date range.
     * 
     * @param tenantId the tenant ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of consumptions
     */
    List<Consumption> findByTenantIdAndConfirmedAtBetween(UUID tenantId, Instant startDate, Instant endDate);
    
    /**
     * Finds all non-voided consumptions for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of consumptions
     */
    List<Consumption> findByTenantIdAndVoidedAtIsNull(UUID tenantId);
    
    /**
     * Finds all voided consumptions for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of consumptions
     */
    List<Consumption> findByTenantIdAndVoidedAtIsNotNull(UUID tenantId);
}
