package com.restaurantpos.cashregister.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashSessionStatus;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.identityaccess.api.AuthorizationApi;

/**
 * Service for cash session management operations.
 * Handles session opening, closing, movement recording, and session summaries.
 * 
 * Requirements: 10.2, 10.3, 10.5, 10.6
 */
@Service
public class CashSessionService {
    
    private final CashSessionRepository cashSessionRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final AuthorizationApi authorizationApi;
    
    public CashSessionService(
            CashSessionRepository cashSessionRepository,
            CashRegisterRepository cashRegisterRepository,
            CashMovementRepository cashMovementRepository,
            AuthorizationApi authorizationApi) {
        this.cashSessionRepository = cashSessionRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.authorizationApi = authorizationApi;
    }
    
    /**
     * Opens a new cash session for an employee on a register.
     * Creates a session with OPEN status and records the opening amount.
     * Only one session can be open per register at a time.
     * 
     * @param registerId the register ID
     * @param employeeId the employee ID
     * @param openingAmount the initial cash amount in the register
     * @return the created cash session
     * @throws IllegalArgumentException if register not found or opening amount is invalid
     * @throws IllegalStateException if register already has an open session
     * 
     * Requirements: 10.2
     */
    @Transactional
    public CashSession openSession(UUID registerId, UUID employeeId, BigDecimal openingAmount) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate opening amount
        if (openingAmount == null || openingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opening amount must be non-negative");
        }
        
        // Fetch register
        CashRegister register = cashRegisterRepository.findByIdAndTenantId(registerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash register not found: " + registerId));
        
        // Check if register is active
        if (!register.isActive()) {
            throw new IllegalStateException("Cannot open session on inactive register");
        }
        
        // Check if register already has an open session
        var existingOpenSession = cashSessionRepository.findFirstByTenantIdAndRegisterIdAndStatusOrderByOpenedAtDesc(
                tenantId, registerId, CashSessionStatus.OPEN);
        if (existingOpenSession.isPresent()) {
            throw new IllegalStateException("Register already has an open session: " + existingOpenSession.get().getId());
        }
        
        // Create session
        CashSession session = new CashSession(tenantId, registerId, employeeId, openingAmount);
        session = cashSessionRepository.save(session);
        
        // Record opening movement
        CashMovement openingMovement = new CashMovement(tenantId, session.getId(), MovementType.OPENING, openingAmount);
        openingMovement.setReason("Session opened");
        openingMovement.setCreatedBy(employeeId);
        cashMovementRepository.save(openingMovement);
        
        return session;
    }
    
    /**
     * Closes a cash session and calculates variance.
     * Expected close is calculated from opening amount plus all movements.
     * Variance = actual close - expected close.
     * 
     * @param sessionId the session ID
     * @param actualAmount the actual cash counted in the register
     * @return the closed cash session with variance calculated
     * @throws IllegalArgumentException if session not found or actual amount is invalid
     * @throws IllegalStateException if session is already closed
     * 
     * Requirements: 10.3, 10.6
     */
    @Transactional
    public CashSession closeSession(UUID sessionId, BigDecimal actualAmount) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate actual amount
        if (actualAmount == null || actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Actual amount must be non-negative");
        }
        
        // Fetch session
        CashSession session = cashSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash session not found: " + sessionId));
        
        // Validate session is open
        if (session.isClosed()) {
            throw new IllegalStateException("Session is already closed");
        }
        
        // Calculate expected close from opening amount and all movements
        List<CashMovement> movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        BigDecimal expectedClose = session.getOpeningAmount();
        
        for (CashMovement movement : movements) {
            switch (movement.getMovementType()) {
                case SALE, DEPOSIT -> expectedClose = expectedClose.add(movement.getAmount());
                case REFUND, WITHDRAWAL -> expectedClose = expectedClose.subtract(movement.getAmount());
                case OPENING, CLOSING -> {
                    // Opening already included in openingAmount, closing will be added below
                }
            }
        }
        
        // Calculate variance
        BigDecimal variance = actualAmount.subtract(expectedClose);
        
        // Update session
        session.setExpectedClose(expectedClose);
        session.setActualClose(actualAmount);
        session.setVariance(variance);
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session = cashSessionRepository.save(session);
        
        // Record closing movement
        CashMovement closingMovement = new CashMovement(tenantId, sessionId, MovementType.CLOSING, actualAmount);
        closingMovement.setReason("Session closed");
        cashMovementRepository.save(closingMovement);
        
        return session;
    }
    
    /**
     * Records a manual cash movement (deposit or withdrawal) in a session.
     * 
     * @param sessionId the session ID
     * @param type the movement type (DEPOSIT or WITHDRAWAL)
     * @param amount the movement amount
     * @param reason the reason for the movement
     * @return the created cash movement
     * @throws IllegalArgumentException if session not found, amount is invalid, or type is not manual
     * @throws IllegalStateException if session is closed
     * 
     * Requirements: 10.5
     */
    @Transactional
    public CashMovement recordMovement(UUID sessionId, MovementType type, BigDecimal amount, String reason) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Movement amount must be positive");
        }
        
        // Validate movement type is manual (DEPOSIT or WITHDRAWAL)
        if (type != MovementType.DEPOSIT && type != MovementType.WITHDRAWAL) {
            throw new IllegalArgumentException("Only DEPOSIT and WITHDRAWAL movements can be recorded manually");
        }
        
        // Fetch session
        CashSession session = cashSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash session not found: " + sessionId));
        
        // Validate session is open
        if (session.isClosed()) {
            throw new IllegalStateException("Cannot record movement on closed session");
        }
        
        // Create movement
        CashMovement movement = new CashMovement(tenantId, sessionId, type, amount);
        movement.setReason(reason);
        movement = cashMovementRepository.save(movement);
        
        return movement;
    }
    
    /**
     * Gets a session summary with all movements.
     * 
     * @param sessionId the session ID
     * @return the session summary containing session details and all movements
     * @throws IllegalArgumentException if session not found
     * 
     * Requirements: 10.5
     */
    @Transactional(readOnly = true)
    public CashSessionSummary getSessionSummary(UUID sessionId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Fetch session
        CashSession session = cashSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash session not found: " + sessionId));
        
        // Fetch all movements
        List<CashMovement> movements = cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        
        return new CashSessionSummary(session, movements);
    }
    
    /**
     * DTO for cash session summary containing session and movements.
     */
    public static class CashSessionSummary {
        private final CashSession session;
        private final List<CashMovement> movements;
        
        public CashSessionSummary(CashSession session, List<CashMovement> movements) {
            this.session = session;
            this.movements = movements;
        }
        
        public CashSession getSession() {
            return session;
        }
        
        public List<CashMovement> getMovements() {
            return movements;
        }
    }
}
