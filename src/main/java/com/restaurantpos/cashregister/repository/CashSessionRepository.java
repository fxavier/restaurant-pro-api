package com.restaurantpos.cashregister.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashSessionStatus;

/**
 * Repository for CashSession entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 10.2, 10.6
 */
@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {
    
    /**
     * Finds all cash sessions for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of cash sessions
     */
    List<CashSession> findByTenantId(UUID tenantId);
    
    /**
     * Finds all cash sessions for a specific register.
     * 
     * @param tenantId the tenant ID
     * @param registerId the register ID
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndRegisterId(UUID tenantId, UUID registerId);
    
    /**
     * Finds all cash sessions for a register with a given status.
     * 
     * @param tenantId the tenant ID
     * @param registerId the register ID
     * @param status the session status
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndRegisterIdAndStatus(UUID tenantId, UUID registerId, CashSessionStatus status);
    
    /**
     * Finds all cash sessions for an employee.
     * 
     * @param tenantId the tenant ID
     * @param employeeId the employee ID
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
    
    /**
     * Finds all cash sessions for an employee within a date range.
     * 
     * @param tenantId the tenant ID
     * @param employeeId the employee ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndEmployeeIdAndOpenedAtBetween(UUID tenantId, UUID employeeId, Instant startDate, Instant endDate);
    
    /**
     * Finds a cash session by ID and tenant ID (for tenant isolation).
     * 
     * @param id the session ID
     * @param tenantId the tenant ID
     * @return the cash session if found
     */
    Optional<CashSession> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds the current open session for a register.
     * 
     * @param tenantId the tenant ID
     * @param registerId the register ID
     * @param status the session status (OPEN)
     * @return the open session if found
     */
    Optional<CashSession> findFirstByTenantIdAndRegisterIdAndStatusOrderByOpenedAtDesc(UUID tenantId, UUID registerId, CashSessionStatus status);
    
    /**
     * Finds all cash sessions for a register within a date range.
     * 
     * @param tenantId the tenant ID
     * @param registerId the register ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndRegisterIdAndOpenedAtBetween(UUID tenantId, UUID registerId, Instant startDate, Instant endDate);
    
    /**
     * Finds all cash sessions for multiple registers within a date range.
     * 
     * @param tenantId the tenant ID
     * @param registerIds the list of register IDs
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndRegisterIdInAndOpenedAtBetween(UUID tenantId, List<UUID> registerIds, Instant startDate, Instant endDate);
    
    /**
     * Finds all cash sessions for a tenant within a date range.
     * 
     * @param tenantId the tenant ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of cash sessions
     */
    List<CashSession> findByTenantIdAndOpenedAtBetween(UUID tenantId, Instant startDate, Instant endDate);
}
