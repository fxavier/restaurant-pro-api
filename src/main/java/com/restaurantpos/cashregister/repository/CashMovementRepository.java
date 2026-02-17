package com.restaurantpos.cashregister.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.model.MovementType;

/**
 * Repository for CashMovement entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 10.3, 10.4
 */
@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
    
    /**
     * Finds all cash movements for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of cash movements
     */
    List<CashMovement> findByTenantId(UUID tenantId);
    
    /**
     * Finds all cash movements for a specific session.
     * 
     * @param tenantId the tenant ID
     * @param sessionId the session ID
     * @return list of cash movements
     */
    List<CashMovement> findByTenantIdAndSessionId(UUID tenantId, UUID sessionId);
    
    /**
     * Finds all cash movements for a session with a given movement type.
     * 
     * @param tenantId the tenant ID
     * @param sessionId the session ID
     * @param movementType the movement type
     * @return list of cash movements
     */
    List<CashMovement> findByTenantIdAndSessionIdAndMovementType(UUID tenantId, UUID sessionId, MovementType movementType);
    
    /**
     * Finds a cash movement by ID and tenant ID (for tenant isolation).
     * 
     * @param id the movement ID
     * @param tenantId the tenant ID
     * @return the cash movement if found
     */
    Optional<CashMovement> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a cash movement by payment ID.
     * 
     * @param tenantId the tenant ID
     * @param paymentId the payment ID
     * @return the cash movement if found
     */
    Optional<CashMovement> findByTenantIdAndPaymentId(UUID tenantId, UUID paymentId);
    
    /**
     * Checks if a cash movement exists for a payment.
     * 
     * @param tenantId the tenant ID
     * @param paymentId the payment ID
     * @return true if movement exists
     */
    boolean existsByTenantIdAndPaymentId(UUID tenantId, UUID paymentId);
    
    /**
     * Finds all cash movements for multiple sessions.
     * 
     * @param tenantId the tenant ID
     * @param sessionIds the list of session IDs
     * @return list of cash movements
     */
    List<CashMovement> findByTenantIdAndSessionIdIn(UUID tenantId, List<UUID> sessionIds);
}
