package com.restaurantpos.paymentsbilling.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.model.PaymentStatus;

/**
 * Repository for Payment entity with tenant filtering and idempotency support.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 7.1, 7.2
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    /**
     * Finds all payments for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of payments
     */
    List<Payment> findByTenantId(UUID tenantId);
    
    /**
     * Finds all payments for a specific order.
     * 
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * @return list of payments
     */
    List<Payment> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);
    
    /**
     * Finds all payments for a specific order with a given status.
     * 
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * @param status the payment status
     * @return list of payments
     */
    List<Payment> findByTenantIdAndOrderIdAndStatus(UUID tenantId, UUID orderId, PaymentStatus status);
    
    /**
     * Finds a payment by ID and tenant ID (for tenant isolation).
     * 
     * @param id the payment ID
     * @param tenantId the tenant ID
     * @return the payment if found
     */
    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a payment by idempotency key and tenant ID.
     * Used to prevent duplicate payment processing.
     * 
     * @param tenantId the tenant ID
     * @param idempotencyKey the idempotency key
     * @return the payment if found
     */
    Optional<Payment> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    
    /**
     * Checks if a payment exists with the given idempotency key.
     * 
     * @param tenantId the tenant ID
     * @param idempotencyKey the idempotency key
     * @return true if payment exists
     */
    boolean existsByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
}
