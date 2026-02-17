package com.restaurantpos.cashregister.listener;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashSessionStatus;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.paymentsbilling.event.PaymentCompleted;
import com.restaurantpos.paymentsbilling.model.PaymentMethod;

/**
 * Event listener for PaymentCompleted events.
 * Creates cash movement records for CASH payments.
 * 
 * Requirements: 10.4
 */
@Component
public class PaymentCompletedListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentCompletedListener.class);
    
    private final CashSessionRepository cashSessionRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    
    public PaymentCompletedListener(
            CashSessionRepository cashSessionRepository,
            CashRegisterRepository cashRegisterRepository,
            CashMovementRepository cashMovementRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
    }
    
    /**
     * Handles PaymentCompleted events by creating cash movement records for CASH payments.
     * Only processes CASH payment methods.
     * Finds an open cash session for the site and creates a SALE movement.
     * If no open session is found, logs a warning and skips movement creation.
     * 
     * @param event the PaymentCompleted event
     * 
     * Requirements: 10.4
     */
    @EventListener
    @Transactional
    public void handlePaymentCompleted(PaymentCompleted event) {
        // Only process CASH payments
        if (event.paymentMethod() != PaymentMethod.CASH) {
            logger.debug("Skipping cash movement for non-CASH payment: {} (method: {})", 
                    event.paymentId(), event.paymentMethod());
            return;
        }
        
        UUID tenantId = event.tenantId();
        UUID siteId = event.siteId();
        
        // Find an open cash session for the site
        // We need to find any open session for any register at this site
        var openSession = findOpenSessionForSite(tenantId, siteId);
        
        if (openSession.isEmpty()) {
            logger.warn("No open cash session found for site {} when processing payment {}. " +
                    "Cash movement will not be recorded.", siteId, event.paymentId());
            return;
        }
        
        CashSession session = openSession.get();
        
        // Create SALE cash movement
        CashMovement movement = new CashMovement(
                tenantId,
                session.getId(),
                MovementType.SALE,
                event.amount()
        );
        movement.setPaymentId(event.paymentId());
        movement.setReason("Payment for order " + event.orderId());
        
        cashMovementRepository.save(movement);
        
        logger.info("Created cash movement {} for payment {} in session {}", 
                movement.getId(), event.paymentId(), session.getId());
    }
    
    /**
     * Finds an open cash session for a site.
     * Searches for any register at the site with an open session.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return the first open session found, or empty if none
     */
    private java.util.Optional<CashSession> findOpenSessionForSite(UUID tenantId, UUID siteId) {
        // Find all registers for the site
        var registers = cashRegisterRepository.findByTenantIdAndSiteId(tenantId, siteId);
        
        // Find the first register with an open session
        for (CashRegister register : registers) {
            var openSession = cashSessionRepository.findFirstByTenantIdAndRegisterIdAndStatusOrderByOpenedAtDesc(
                    tenantId, register.getId(), CashSessionStatus.OPEN);
            if (openSession.isPresent()) {
                return openSession;
            }
        }
        
        return java.util.Optional.empty();
    }
}
