package com.restaurantpos.paymentsbilling.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.paymentsbilling.entity.PaymentCardBlacklist;

/**
 * Repository for PaymentCardBlacklist entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 7.10
 */
@Repository
public interface PaymentCardBlacklistRepository extends JpaRepository<PaymentCardBlacklist, UUID> {
    
    /**
     * Finds all blacklisted cards for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of blacklisted cards
     */
    List<PaymentCardBlacklist> findByTenantId(UUID tenantId);
    
    /**
     * Finds a blacklisted card by card last four digits and tenant ID.
     * 
     * @param tenantId the tenant ID
     * @param cardLastFour the last four digits of the card
     * @return the blacklist entry if found
     */
    Optional<PaymentCardBlacklist> findByTenantIdAndCardLastFour(UUID tenantId, String cardLastFour);
    
    /**
     * Checks if a card is blacklisted.
     * 
     * @param tenantId the tenant ID
     * @param cardLastFour the last four digits of the card
     * @return true if card is blacklisted
     */
    boolean existsByTenantIdAndCardLastFour(UUID tenantId, String cardLastFour);
}
