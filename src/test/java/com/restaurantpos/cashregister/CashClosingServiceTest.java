package com.restaurantpos.cashregister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.cashregister.entity.CashClosing;
import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashRegister;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashSessionStatus;
import com.restaurantpos.cashregister.model.ClosingType;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.repository.CashClosingRepository;
import com.restaurantpos.cashregister.repository.CashMovementRepository;
import com.restaurantpos.cashregister.repository.CashRegisterRepository;
import com.restaurantpos.cashregister.repository.CashSessionRepository;
import com.restaurantpos.cashregister.service.CashClosingService;
import com.restaurantpos.cashregister.service.CashClosingService.ClosingReport;
import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;

/**
 * Unit tests for CashClosingService.
 * 
 * Requirements: 10.7, 10.8, 10.9
 */
@ExtendWith(MockitoExtension.class)
class CashClosingServiceTest {
    
    @Mock
    private CashClosingRepository cashClosingRepository;
    
    @Mock
    private CashSessionRepository cashSessionRepository;
    
    @Mock
    private CashMovementRepository cashMovementRepository;
    
    @Mock
    private CashRegisterRepository cashRegisterRepository;
    
    @Mock
    private AuthorizationApi authorizationApi;
    
    @InjectMocks
    private CashClosingService cashClosingService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID registerId;
    private UUID sessionId;
    private UUID userId;
    private Instant periodStart;
    private Instant periodEnd;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        registerId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        periodStart = Instant.now().minus(1, ChronoUnit.DAYS);
        periodEnd = Instant.now();
        
        when(authorizationApi.getTenantContext()).thenReturn(tenantId);
    }
    
    @Test
    void closeRegister_shouldCreateRegisterClosing() {
        // Arrange
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        CashSession session = new CashSession(tenantId, registerId, userId, new BigDecimal("100.00"));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setExpectedClose(new BigDecimal("150.00"));
        session.setActualClose(new BigDecimal("148.00"));
        session.setVariance(new BigDecimal("-2.00"));
        
        CashMovement sale1 = new CashMovement(tenantId, sessionId, MovementType.SALE, new BigDecimal("30.00"));
        CashMovement sale2 = new CashMovement(tenantId, sessionId, MovementType.SALE, new BigDecimal("20.00"));
        
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
                .thenReturn(Optional.of(register));
        when(cashSessionRepository.findByTenantIdAndRegisterIdAndOpenedAtBetween(
                tenantId, registerId, periodStart, periodEnd))
                .thenReturn(List.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session.getId()))
                .thenReturn(List.of(sale1, sale2));
        when(cashClosingRepository.save(any(CashClosing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        CashClosing result = cashClosingService.closeRegister(registerId, periodStart, periodEnd);
        
        // Assert
        assertNotNull(result);
        assertEquals(ClosingType.REGISTER, result.getClosingType());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(periodStart, result.getPeriodStart());
        assertEquals(periodEnd, result.getPeriodEnd());
        assertEquals(new BigDecimal("50.00"), result.getTotalSales());
        assertEquals(BigDecimal.ZERO, result.getTotalRefunds());
        assertEquals(new BigDecimal("-2.00"), result.getVariance());
        
        verify(cashClosingRepository).save(any(CashClosing.class));
    }
    
    @Test
    void closeRegister_shouldThrowExceptionWhenRegisterNotFound() {
        // Arrange
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                cashClosingService.closeRegister(registerId, periodStart, periodEnd));
    }
    
    @Test
    void closeRegister_shouldThrowExceptionWhenPeriodInvalid() {
        // Arrange
        Instant invalidEnd = periodStart.minus(1, ChronoUnit.DAYS);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                cashClosingService.closeRegister(registerId, periodStart, invalidEnd));
    }
    
    @Test
    void closeDay_shouldCreateDayClosing() {
        // Arrange
        LocalDate date = LocalDate.now();
        
        UUID register1Id = UUID.randomUUID();
        UUID register2Id = UUID.randomUUID();
        UUID session1Id = UUID.randomUUID();
        UUID session2Id = UUID.randomUUID();
        
        CashRegister register1 = new CashRegister(tenantId, siteId, "REG-001");
        setRegisterId(register1, register1Id);
        CashRegister register2 = new CashRegister(tenantId, siteId, "REG-002");
        setRegisterId(register2, register2Id);
        
        CashSession session1 = new CashSession(tenantId, register1Id, userId, new BigDecimal("100.00"));
        session1.setStatus(CashSessionStatus.CLOSED);
        session1.setVariance(new BigDecimal("-1.00"));
        setSessionId(session1, session1Id);
        
        CashSession session2 = new CashSession(tenantId, register2Id, userId, new BigDecimal("100.00"));
        session2.setStatus(CashSessionStatus.CLOSED);
        session2.setVariance(new BigDecimal("2.00"));
        setSessionId(session2, session2Id);
        
        CashMovement sale1 = new CashMovement(tenantId, session1Id, MovementType.SALE, new BigDecimal("50.00"));
        CashMovement sale2 = new CashMovement(tenantId, session2Id, MovementType.SALE, new BigDecimal("75.00"));
        CashMovement refund = new CashMovement(tenantId, session2Id, MovementType.REFUND, new BigDecimal("10.00"));
        
        when(cashRegisterRepository.findByTenantIdAndSiteId(tenantId, siteId))
                .thenReturn(List.of(register1, register2));
        when(cashSessionRepository.findByTenantIdAndRegisterIdInAndOpenedAtBetween(
                eq(tenantId), anyList(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(session1, session2));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session1Id))
                .thenReturn(List.of(sale1));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session2Id))
                .thenReturn(List.of(sale2, refund));
        when(cashClosingRepository.save(any(CashClosing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        CashClosing result = cashClosingService.closeDay(siteId, date);
        
        // Assert
        assertNotNull(result);
        assertEquals(ClosingType.DAY, result.getClosingType());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(new BigDecimal("125.00"), result.getTotalSales());
        assertEquals(new BigDecimal("10.00"), result.getTotalRefunds());
        assertEquals(new BigDecimal("1.00"), result.getVariance()); // -1 + 2
        
        verify(cashClosingRepository).save(any(CashClosing.class));
    }
    
    private void setRegisterId(CashRegister register, UUID id) {
        try {
            var field = CashRegister.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(register, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set register ID", e);
        }
    }
    
    @Test
    void closeDay_shouldThrowExceptionWhenNoRegistersFound() {
        // Arrange
        LocalDate date = LocalDate.now();
        when(cashRegisterRepository.findByTenantIdAndSiteId(tenantId, siteId))
                .thenReturn(List.of());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                cashClosingService.closeDay(siteId, date));
    }
    
    @Test
    void closeFinancialPeriod_shouldCreateFinancialPeriodClosing() {
        // Arrange
        CashSession session = new CashSession(tenantId, registerId, userId, new BigDecimal("100.00"));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setVariance(new BigDecimal("5.00"));
        
        CashMovement sale = new CashMovement(tenantId, sessionId, MovementType.SALE, new BigDecimal("200.00"));
        CashMovement refund = new CashMovement(tenantId, sessionId, MovementType.REFUND, new BigDecimal("20.00"));
        
        when(cashSessionRepository.findByTenantIdAndOpenedAtBetween(tenantId, periodStart, periodEnd))
                .thenReturn(List.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session.getId()))
                .thenReturn(List.of(sale, refund));
        when(cashClosingRepository.save(any(CashClosing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        CashClosing result = cashClosingService.closeFinancialPeriod(tenantId, periodStart, periodEnd);
        
        // Assert
        assertNotNull(result);
        assertEquals(ClosingType.FINANCIAL_PERIOD, result.getClosingType());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(periodStart, result.getPeriodStart());
        assertEquals(periodEnd, result.getPeriodEnd());
        assertEquals(new BigDecimal("200.00"), result.getTotalSales());
        assertEquals(new BigDecimal("20.00"), result.getTotalRefunds());
        assertEquals(new BigDecimal("5.00"), result.getVariance());
        
        verify(cashClosingRepository).save(any(CashClosing.class));
    }
    
    @Test
    void closeFinancialPeriod_shouldThrowExceptionWhenTenantMismatch() {
        // Arrange
        UUID differentTenantId = UUID.randomUUID();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                cashClosingService.closeFinancialPeriod(differentTenantId, periodStart, periodEnd));
    }
    
    @Test
    void generateClosingReport_shouldReturnReportWithAllData() {
        // Arrange
        UUID closingId = UUID.randomUUID();
        CashClosing closing = new CashClosing(
                tenantId, ClosingType.REGISTER, periodStart, periodEnd,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("2.00"), userId);
        
        CashSession session = new CashSession(tenantId, registerId, userId, new BigDecimal("50.00"));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setExpectedClose(new BigDecimal("100.00"));
        session.setActualClose(new BigDecimal("102.00"));
        
        CashMovement movement = new CashMovement(tenantId, sessionId, MovementType.SALE, new BigDecimal("50.00"));
        
        when(cashClosingRepository.findByIdAndTenantId(closingId, tenantId))
                .thenReturn(Optional.of(closing));
        when(cashSessionRepository.findByTenantIdAndOpenedAtBetween(tenantId, periodStart, periodEnd))
                .thenReturn(List.of(session));
        when(cashMovementRepository.findByTenantIdAndSessionIdIn(eq(tenantId), anyList()))
                .thenReturn(List.of(movement));
        
        // Act
        ClosingReport result = cashClosingService.generateClosingReport(closingId);
        
        // Assert
        assertNotNull(result);
        assertEquals(closing, result.getClosing());
        assertEquals(1, result.getSessions().size());
        assertEquals(1, result.getMovements().size());
        assertEquals(new BigDecimal("50.00"), result.getOpeningBalance());
        assertEquals(new BigDecimal("100.00"), result.getExpectedClose());
        assertEquals(new BigDecimal("102.00"), result.getActualClose());
    }
    
    @Test
    void generateClosingReport_shouldThrowExceptionWhenClosingNotFound() {
        // Arrange
        UUID closingId = UUID.randomUUID();
        when(cashClosingRepository.findByIdAndTenantId(closingId, tenantId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                cashClosingService.generateClosingReport(closingId));
    }
    
    @Test
    void reprintClosingReport_shouldRequirePermission() {
        // Arrange
        UUID closingId = UUID.randomUUID();
        CashClosing closing = new CashClosing(
                tenantId, ClosingType.REGISTER, periodStart, periodEnd,
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("2.00"), userId);
        
        when(cashClosingRepository.findByIdAndTenantId(closingId, tenantId))
                .thenReturn(Optional.of(closing));
        when(cashSessionRepository.findByTenantIdAndOpenedAtBetween(tenantId, periodStart, periodEnd))
                .thenReturn(List.of());
        
        // Act
        ClosingReport result = cashClosingService.reprintClosingReport(closingId);
        
        // Assert
        assertNotNull(result);
        verify(authorizationApi).requirePermission(any(UUID.class), eq(PermissionType.REPRINT_DOCUMENT));
    }
    
    @Test
    void closeRegister_shouldHandleMultipleSessions() {
        // Arrange
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        
        UUID session1Id = UUID.randomUUID();
        UUID session2Id = UUID.randomUUID();
        
        CashSession session1 = new CashSession(tenantId, registerId, userId, new BigDecimal("100.00"));
        session1.setStatus(CashSessionStatus.CLOSED);
        session1.setVariance(new BigDecimal("1.00"));
        // Use reflection to set the ID for testing
        setSessionId(session1, session1Id);
        
        CashSession session2 = new CashSession(tenantId, registerId, userId, new BigDecimal("150.00"));
        session2.setStatus(CashSessionStatus.CLOSED);
        session2.setVariance(new BigDecimal("-0.50"));
        setSessionId(session2, session2Id);
        
        CashMovement sale1 = new CashMovement(tenantId, session1Id, MovementType.SALE, new BigDecimal("50.00"));
        CashMovement sale2 = new CashMovement(tenantId, session2Id, MovementType.SALE, new BigDecimal("75.00"));
        
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
                .thenReturn(Optional.of(register));
        when(cashSessionRepository.findByTenantIdAndRegisterIdAndOpenedAtBetween(
                tenantId, registerId, periodStart, periodEnd))
                .thenReturn(List.of(session1, session2));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session1Id))
                .thenReturn(List.of(sale1));
        when(cashMovementRepository.findByTenantIdAndSessionId(tenantId, session2Id))
                .thenReturn(List.of(sale2));
        when(cashClosingRepository.save(any(CashClosing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        CashClosing result = cashClosingService.closeRegister(registerId, periodStart, periodEnd);
        
        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("125.00"), result.getTotalSales());
        assertEquals(new BigDecimal("0.50"), result.getVariance()); // 1.00 + (-0.50)
    }
    
    private void setSessionId(CashSession session, UUID id) {
        try {
            var field = CashSession.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(session, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set session ID", e);
        }
    }
    
    @Test
    void closeRegister_shouldHandleNoSessions() {
        // Arrange
        CashRegister register = new CashRegister(tenantId, siteId, "REG-001");
        
        when(cashRegisterRepository.findByIdAndTenantId(registerId, tenantId))
                .thenReturn(Optional.of(register));
        when(cashSessionRepository.findByTenantIdAndRegisterIdAndOpenedAtBetween(
                tenantId, registerId, periodStart, periodEnd))
                .thenReturn(List.of());
        when(cashClosingRepository.save(any(CashClosing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        CashClosing result = cashClosingService.closeRegister(registerId, periodStart, periodEnd);
        
        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalSales());
        assertEquals(BigDecimal.ZERO, result.getTotalRefunds());
        assertEquals(BigDecimal.ZERO, result.getVariance());
    }
}
