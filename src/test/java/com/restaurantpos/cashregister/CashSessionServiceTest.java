package com.restaurantpos.cashregister;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashRegisterStatus;
import com.restaurantpos.cashregister.model.CashSessionStatus;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.cashregister.service.CashSessionService;
import com.restaurantpos.identityaccess.api.AuthorizationApi;

/**
 * Unit tests for CashSessionService.
 * Tests session opening, closing, movement recording, and session summaries.
 */
@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {
    
    @Mock
    private CashSessionRepository cashSessionRepository;
    
    @Mock
    private CashRegisterRepository cashRegisterRepository;
    
    @Mock
    private CashMovementRepository cashMovementRepository;
    
    @Mock
    private AuthorizationApi authorizationApi;
    
    private CashSessionService cashSessionService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID registerId;
    private UUID employeeId;
    private UUID sessionId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        registerId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        
        cashSessionService = new CashSessionService(
            cashSessionRepository,
            cashRegisterRepository,
            cashMovementRepository,
            authorizationApi
        );
    }
    
    @Test
    void openSession_withValidRequest_createsSession() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        register.setStatus(CashRegisterStatus.ACTIVE);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
            .thenReturn(Optional.of(register));
        when(cashSessionRepository.findFirstByTenantIdAndRegisterIdAndStatusOrderByOpenedAtDesc(
            tenantId, registerId, CashSessionStatus.OPEN))
            .thenReturn(Optional.empty());
        when(cashSessionRepository.save(any(CashSession.class)))
            .thenAnswer(invocation -> {
                CashSession s = invocation.getArgument(0);
                // Simulate JPA setting the ID
                try {
                    var idField = CashSession.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(s, sessionId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return s;
            });
        when(cashMovementRepository.save(any(CashMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CashSession session = cashSessionService.openSession(registerId, employeeId, openingAmount);
        
        // Then
        assertNotNull(session);
        assertEquals(sessionId, session.getId());
        assertEquals(tenantId, session.getTenantId());
        assertEquals(registerId, session.getRegisterId());
        assertEquals(employeeId, session.getEmployeeId());
        assertEquals(openingAmount, session.getOpeningAmount());
        assertEquals(CashSessionStatus.OPEN, session.getStatus());
        assertTrue(session.isOpen());
        
        verify(cashSessionRepository, times(1)).save(any(CashSession.class));
        verify(cashMovementRepository, times(1)).save(any(CashMovement.class));
    }
    
    @Test
    void openSession_withNegativeAmount_throwsException() {
        // Given
        BigDecimal negativeAmount = BigDecimal.valueOf(-10.00);
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.openSession(registerId, employeeId, negativeAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void openSession_withNullAmount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.openSession(registerId, employeeId, null)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void openSession_withNonExistentRegister_throwsException() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.openSession(registerId, employeeId, openingAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void openSession_withInactiveRegister_throwsException() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        register.setStatus(CashRegisterStatus.INACTIVE);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
            .thenReturn(Optional.of(register));
        
        // When & Then
        assertThrows(IllegalStateException.class, () ->
            cashSessionService.openSession(registerId, employeeId, openingAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void openSession_withExistingOpenSession_throwsException() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        register.setStatus(CashRegisterStatus.ACTIVE);
        CashSession existingSession = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(50.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
            .thenReturn(Optional.of(register));
        when(cashSessionRepository.findFirstByTenantIdAndRegisterIdAndStatusOrderByOpenedAtDesc(
            tenantId, registerId, CashSessionStatus.OPEN))
            .thenReturn(Optional.of(existingSession));
        
        // When & Then
        assertThrows(IllegalStateException.class, () ->
            cashSessionService.openSession(registerId, employeeId, openingAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void closeSession_withValidRequest_closesSessionAndCalculatesVariance() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        BigDecimal actualAmount = BigDecimal.valueOf(250.00);
        
        CashSession session = new CashSession(tenantId, registerId, employeeId, openingAmount);
        
        // Create movements: opening (100), sale (100), deposit (50), withdrawal (20)
        // Expected = 100 + 100 + 50 - 20 = 230
        // Variance = 250 - 230 = 20
        CashMovement opening = new CashMovement(tenantId, sessionId, MovementType.OPENING, BigDecimal.valueOf(100.00));
        CashMovement sale = new CashMovement(tenantId, sessionId, MovementType.SALE, BigDecimal.valueOf(100.00));
        CashMovement deposit = new CashMovement(tenantId, sessionId, MovementType.DEPOSIT, BigDecimal.valueOf(50.00));
        CashMovement withdrawal = new CashMovement(tenantId, sessionId, MovementType.WITHDRAWAL, BigDecimal.valueOf(20.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId))
            .thenReturn(List.of(opening, sale, deposit, withdrawal));
        when(cashSessionRepository.save(any(CashSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMovementRepository.save(any(CashMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CashSession closedSession = cashSessionService.closeSession(sessionId, actualAmount);
        
        // Then
        assertNotNull(closedSession);
        assertEquals(CashSessionStatus.CLOSED, closedSession.getStatus());
        assertEquals(BigDecimal.valueOf(230.00), closedSession.getExpectedClose());
        assertEquals(actualAmount, closedSession.getActualClose());
        assertEquals(BigDecimal.valueOf(20.00), closedSession.getVariance());
        assertNotNull(closedSession.getClosedAt());
        assertTrue(closedSession.isClosed());
        
        verify(cashSessionRepository, times(1)).save(any(CashSession.class));
        verify(cashMovementRepository, times(1)).save(any(CashMovement.class));
    }
    
    @Test
    void closeSession_withNegativeVariance_calculatesCorrectly() {
        // Given
        BigDecimal openingAmount = BigDecimal.valueOf(100.00);
        BigDecimal actualAmount = BigDecimal.valueOf(80.00);
        
        CashSession session = new CashSession(tenantId, registerId, employeeId, openingAmount);
        
        // Expected = 100 (opening only)
        // Variance = 80 - 100 = -20
        CashMovement opening = new CashMovement(tenantId, sessionId, MovementType.OPENING, openingAmount);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId))
            .thenReturn(List.of(opening));
        when(cashSessionRepository.save(any(CashSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(cashMovementRepository.save(any(CashMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CashSession closedSession = cashSessionService.closeSession(sessionId, actualAmount);
        
        // Then
        assertEquals(BigDecimal.valueOf(-20.00), closedSession.getVariance());
    }
    
    @Test
    void closeSession_withNullAmount_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.closeSession(sessionId, null)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void closeSession_withNegativeAmount_throwsException() {
        // Given
        BigDecimal negativeAmount = BigDecimal.valueOf(-10.00);
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.closeSession(sessionId, negativeAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void closeSession_withNonExistentSession_throwsException() {
        // Given
        BigDecimal actualAmount = BigDecimal.valueOf(100.00);
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.closeSession(sessionId, actualAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void closeSession_withAlreadyClosedSession_throwsException() {
        // Given
        BigDecimal actualAmount = BigDecimal.valueOf(100.00);
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        session.setStatus(CashSessionStatus.CLOSED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        
        // When & Then
        assertThrows(IllegalStateException.class, () ->
            cashSessionService.closeSession(sessionId, actualAmount)
        );
        
        verify(cashSessionRepository, never()).save(any(CashSession.class));
    }
    
    @Test
    void recordMovement_withDeposit_createsMovement() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String reason = "Cash deposit from safe";
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        when(cashMovementRepository.save(any(CashMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CashMovement movement = cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, amount, reason);
        
        // Then
        assertNotNull(movement);
        assertEquals(tenantId, movement.getTenantId());
        assertEquals(sessionId, movement.getSessionId());
        assertEquals(MovementType.DEPOSIT, movement.getMovementType());
        assertEquals(amount, movement.getAmount());
        assertEquals(reason, movement.getReason());
        assertTrue(movement.isDeposit());
        
        verify(cashMovementRepository, times(1)).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withWithdrawal_createsMovement() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(30.00);
        String reason = "Cash withdrawal to safe";
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        when(cashMovementRepository.save(any(CashMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CashMovement movement = cashSessionService.recordMovement(sessionId, MovementType.WITHDRAWAL, amount, reason);
        
        // Then
        assertNotNull(movement);
        assertEquals(MovementType.WITHDRAWAL, movement.getMovementType());
        assertTrue(movement.isWithdrawal());
        
        verify(cashMovementRepository, times(1)).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withInvalidType_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String reason = "Invalid movement";
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then - SALE is not a manual movement type
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.SALE, amount, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withNullAmount_throwsException() {
        // Given
        String reason = "Test movement";
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, null, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withZeroAmount_throwsException() {
        // Given
        BigDecimal zeroAmount = BigDecimal.ZERO;
        String reason = "Test movement";
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, zeroAmount, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withNegativeAmount_throwsException() {
        // Given
        BigDecimal negativeAmount = BigDecimal.valueOf(-10.00);
        String reason = "Test movement";
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, negativeAmount, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withNonExistentSession_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String reason = "Test movement";
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, amount, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void recordMovement_withClosedSession_throwsException() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String reason = "Test movement";
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        session.setStatus(CashSessionStatus.CLOSED);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        
        // When & Then
        assertThrows(IllegalStateException.class, () ->
            cashSessionService.recordMovement(sessionId, MovementType.DEPOSIT, amount, reason)
        );
        
        verify(cashMovementRepository, never()).save(any(CashMovement.class));
    }
    
    @Test
    void getSessionSummary_withValidSession_returnsSummary() {
        // Given
        CashSession session = new CashSession(tenantId, registerId, employeeId, BigDecimal.valueOf(100.00));
        CashMovement movement1 = new CashMovement(tenantId, sessionId, MovementType.SALE, BigDecimal.valueOf(50.00));
        CashMovement movement2 = new CashMovement(tenantId, sessionId, MovementType.DEPOSIT, BigDecimal.valueOf(20.00));
        List<CashMovement> movements = List.of(movement1, movement2);
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, sessionId))
            .thenReturn(movements);
        
        // When
        CashSessionService.CashSessionSummary summary = cashSessionService.getSessionSummary(sessionId);
        
        // Then
        assertNotNull(summary);
        assertEquals(session, summary.getSession());
        assertEquals(movements, summary.getMovements());
        assertEquals(2, summary.getMovements().size());
    }
    
    @Test
    void getSessionSummary_withNonExistentSession_throwsException() {
        // Given
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
        when(cashSessionRepository.findByIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            cashSessionService.getSessionSummary(sessionId)
        );
    }
}
