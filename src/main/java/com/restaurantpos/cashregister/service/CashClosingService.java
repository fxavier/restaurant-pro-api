package com.restaurantpos.cashregister.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.cashregister.entity.CashClosing;
import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.ClosingType;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashClosingRepository;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;

/**
 * Service for cash closing operations at various levels.
 * Handles register closings, day closings, financial period closings, and report generation.
 * 
 * Requirements: 10.7, 10.8, 10.9
 */
@Service
public class CashClosingService {
    
    private final CashClosingRepository cashClosingRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final AuthorizationApi authorizationApi;
    
    public CashClosingService(
            CashClosingRepository cashClosingRepository,
            CashSessionRepository cashSessionRepository,
            CashMovementRepository cashMovementRepository,
            CashRegisterRepository cashRegisterRepository,
            AuthorizationApi authorizationApi) {
        this.cashClosingRepository = cashClosingRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.authorizationApi = authorizationApi;
    }
    
    /**
     * Closes a cash register for a specific period.
     * Aggregates all sessions for the register within the period.
     * 
     * @param registerId the register ID
     * @param periodStart the start of the period
     * @param periodEnd the end of the period
     * @return the created cash closing
     * @throws IllegalArgumentException if register not found or period is invalid
     * 
     * Requirements: 10.7
     */
    @Transactional
    public CashClosing closeRegister(UUID registerId, Instant periodStart, Instant periodEnd) {
        UUID tenantId = authorizationApi.getTenantContext();
        UUID userId = getCurrentUserId();
        
        // Validate period
        validatePeriod(periodStart, periodEnd);
        
        // Fetch register
        CashRegister register = cashRegisterRepository.findByIdAndTenantId(registerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash register not found: " + registerId));
        
        // Fetch all sessions for the register in the period
        List<CashSession> sessions = cashSessionRepository.findByTenantIdAndRegisterIdAndOpenedAtBetween(
                tenantId, registerId, periodStart, periodEnd);
        
        // Calculate totals from sessions
        ClosingTotals totals = calculateTotalsFromSessions(tenantId, sessions);
        
        // Create closing record
        CashClosing closing = new CashClosing(
                tenantId,
                ClosingType.REGISTER,
                periodStart,
                periodEnd,
                totals.totalSales,
                totals.totalRefunds,
                totals.variance,
                userId
        );
        
        return cashClosingRepository.save(closing);
    }
    
    /**
     * Closes a day for a specific site.
     * Aggregates all sessions for all registers at the site for the given date.
     * 
     * @param siteId the site ID
     * @param date the date to close
     * @return the created cash closing
     * @throws IllegalArgumentException if site not found or date is invalid
     * 
     * Requirements: 10.7
     */
    @Transactional
    public CashClosing closeDay(UUID siteId, LocalDate date) {
        UUID tenantId = authorizationApi.getTenantContext();
        UUID userId = getCurrentUserId();
        
        // Validate date
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        
        // Convert date to period (start of day to end of day)
        ZoneId zoneId = ZoneId.systemDefault(); // In production, use site's timezone
        Instant periodStart = date.atStartOfDay(zoneId).toInstant();
        Instant periodEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Fetch all registers for the site
        List<CashRegister> registers = cashRegisterRepository.findByTenantIdAndSiteId(tenantId, siteId);
        
        if (registers.isEmpty()) {
            throw new IllegalArgumentException("No registers found for site: " + siteId);
        }
        
        // Fetch all sessions for all registers in the period
        List<UUID> registerIds = registers.stream().map(CashRegister::getId).toList();
        List<CashSession> sessions = cashSessionRepository.findByTenantIdAndRegisterIdInAndOpenedAtBetween(
                tenantId, registerIds, periodStart, periodEnd);
        
        // Calculate totals from sessions
        ClosingTotals totals = calculateTotalsFromSessions(tenantId, sessions);
        
        // Create closing record
        CashClosing closing = new CashClosing(
                tenantId,
                ClosingType.DAY,
                periodStart,
                periodEnd,
                totals.totalSales,
                totals.totalRefunds,
                totals.variance,
                userId
        );
        
        return cashClosingRepository.save(closing);
    }
    
    /**
     * Closes a financial period for a tenant.
     * Aggregates all sessions across all sites for the tenant within the period.
     * 
     * @param tenantId the tenant ID
     * @param periodStart the start of the period
     * @param periodEnd the end of the period
     * @return the created cash closing
     * @throws IllegalArgumentException if period is invalid
     * 
     * Requirements: 10.7
     */
    @Transactional
    public CashClosing closeFinancialPeriod(UUID tenantId, Instant periodStart, Instant periodEnd) {
        UUID contextTenantId = authorizationApi.getTenantContext();
        UUID userId = getCurrentUserId();
        
        // Ensure tenant ID matches context
        if (!tenantId.equals(contextTenantId)) {
            throw new IllegalArgumentException("Tenant ID mismatch");
        }
        
        // Validate period
        validatePeriod(periodStart, periodEnd);
        
        // Fetch all sessions for the tenant in the period
        List<CashSession> sessions = cashSessionRepository.findByTenantIdAndOpenedAtBetween(
                tenantId, periodStart, periodEnd);
        
        // Calculate totals from sessions
        ClosingTotals totals = calculateTotalsFromSessions(tenantId, sessions);
        
        // Create closing record
        CashClosing closing = new CashClosing(
                tenantId,
                ClosingType.FINANCIAL_PERIOD,
                periodStart,
                periodEnd,
                totals.totalSales,
                totals.totalRefunds,
                totals.variance,
                userId
        );
        
        return cashClosingRepository.save(closing);
    }
    
    /**
     * Generates a closing report for a specific closing.
     * 
     * @param closingId the closing ID
     * @return the closing report
     * @throws IllegalArgumentException if closing not found
     * 
     * Requirements: 10.8
     */
    @Transactional(readOnly = true)
    public ClosingReport generateClosingReport(UUID closingId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Fetch closing
        CashClosing closing = cashClosingRepository.findByIdAndTenantId(closingId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cash closing not found: " + closingId));
        
        // Fetch all sessions in the closing period
        List<CashSession> sessions = fetchSessionsForClosing(tenantId, closing);
        
        // Fetch all movements for the sessions
        List<UUID> sessionIds = sessions.stream().map(CashSession::getId).toList();
        List<CashMovement> movements = sessionIds.isEmpty() 
                ? List.of() 
                : cashMovementRepository.findByTenantIdAndSessionIdIn(tenantId, sessionIds);
        
        return new ClosingReport(closing, sessions, movements);
    }
    
    /**
     * Reprints a previous closing report.
     * Requires REPRINT_DOCUMENT permission.
     * 
     * @param closingId the closing ID
     * @return the closing report
     * @throws IllegalArgumentException if closing not found
     * @throws RuntimeException if user lacks permission
     * 
     * Requirements: 10.9
     */
    @Transactional(readOnly = true)
    public ClosingReport reprintClosingReport(UUID closingId) {
        UUID userId = getCurrentUserId();
        
        // Check permission
        authorizationApi.requirePermission(userId, PermissionType.REPRINT_DOCUMENT);
        
        // Generate report (same as generateClosingReport)
        return generateClosingReport(closingId);
    }
    
    // Helper methods
    
    private void validatePeriod(Instant periodStart, Instant periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("Period start and end cannot be null");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("Period start must be before period end");
        }
    }
    
    private ClosingTotals calculateTotalsFromSessions(UUID tenantId, List<CashSession> sessions) {
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        BigDecimal totalVariance = BigDecimal.ZERO;
        
        for (CashSession session : sessions) {
            // Fetch movements for this session
            List<CashMovement> movements = cashMovementRepository.findByTenantIdAndSessionId(
                    tenantId, session.getId());
            
            // Calculate sales and refunds from movements (exclude OPENING and CLOSING)
            for (CashMovement movement : movements) {
                if (movement.getMovementType() == MovementType.SALE) {
                    totalSales = totalSales.add(movement.getAmount());
                } else if (movement.getMovementType() == MovementType.REFUND) {
                    totalRefunds = totalRefunds.add(movement.getAmount());
                }
                // DEPOSIT and WITHDRAWAL are not included in sales/refunds
                // OPENING and CLOSING are not included in sales/refunds
            }
            
            // Add session variance if session is closed
            if (session.isClosed() && session.getVariance() != null) {
                totalVariance = totalVariance.add(session.getVariance());
            }
        }
        
        return new ClosingTotals(totalSales, totalRefunds, totalVariance);
    }
    
    private List<CashSession> fetchSessionsForClosing(UUID tenantId, CashClosing closing) {
        return cashSessionRepository.findByTenantIdAndOpenedAtBetween(
                tenantId, closing.getPeriodStart(), closing.getPeriodEnd());
    }
    
    private UUID getCurrentUserId() {
        // In a real implementation, this would get the user ID from the security context
        // For now, we'll use a placeholder
        // TODO: Implement proper user ID retrieval from security context
        return UUID.randomUUID();
    }
    
    // Helper classes
    
    private static class ClosingTotals {
        final BigDecimal totalSales;
        final BigDecimal totalRefunds;
        final BigDecimal variance;
        
        ClosingTotals(BigDecimal totalSales, BigDecimal totalRefunds, BigDecimal variance) {
            this.totalSales = totalSales;
            this.totalRefunds = totalRefunds;
            this.variance = variance;
        }
    }
    
    /**
     * DTO for closing report containing closing details, sessions, and movements.
     * 
     * Requirements: 10.8
     */
    public static class ClosingReport {
        private final CashClosing closing;
        private final List<CashSession> sessions;
        private final List<CashMovement> movements;
        
        public ClosingReport(CashClosing closing, List<CashSession> sessions, List<CashMovement> movements) {
            this.closing = closing;
            this.sessions = sessions;
            this.movements = movements;
        }
        
        public CashClosing getClosing() {
            return closing;
        }
        
        public List<CashSession> getSessions() {
            return sessions;
        }
        
        public List<CashMovement> getMovements() {
            return movements;
        }
        
        /**
         * Gets the opening balance (sum of all session opening amounts).
         */
        public BigDecimal getOpeningBalance() {
            return sessions.stream()
                    .map(CashSession::getOpeningAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        /**
         * Gets the expected close (sum of all session expected closes).
         */
        public BigDecimal getExpectedClose() {
            return sessions.stream()
                    .filter(CashSession::isClosed)
                    .map(s -> s.getExpectedClose() != null ? s.getExpectedClose() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        /**
         * Gets the actual close (sum of all session actual closes).
         */
        public BigDecimal getActualClose() {
            return sessions.stream()
                    .filter(CashSession::isClosed)
                    .map(s -> s.getActualClose() != null ? s.getActualClose() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
