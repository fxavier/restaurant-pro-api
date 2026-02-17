package com.restaurantpos.cashregister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurantpos.cashregister.controller.CashRegisterController;
import com.restaurantpos.cashregister.entity.CashClosing;
import com.restaurantpos.cashregister.entity.CashMovement;
import com.restaurantpos.cashregister.entity.CashSession;
import com.restaurantpos.cashregister.model.CashSessionStatus;
import com.restaurantpos.cashregister.model.ClosingType;
import com.restaurantpos.cashregister.model.MovementType;
import com.restaurantpos.cashregister.service.CashClosingService;
import com.restaurantpos.cashregister.service.CashClosingService.ClosingReport;
import com.restaurantpos.cashregister.service.CashSessionService;
import com.restaurantpos.cashregister.service.CashSessionService.CashSessionSummary;

/**
 * Unit tests for CashRegisterController.
 * Tests REST endpoints for cash register operations.
 */
@WebMvcTest(controllers = CashRegisterController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
class CashRegisterControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CashSessionService cashSessionService;
    
    @MockBean
    private CashClosingService cashClosingService;
    
    private UUID tenantId;
    private UUID registerId;
    private UUID sessionId;
    private UUID employeeId;
    private UUID siteId;
    private UUID closingId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        registerId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        closingId = UUID.randomUUID();
    }
    
    // Cash Session Tests
    
    @Test
    void openSession_withValidRequest_returnsCreated() throws Exception {
        // Given
        CashSession session = new CashSession(tenantId, registerId, employeeId, new BigDecimal("100.00"));
        
        when(cashSessionService.openSession(eq(registerId), any(UUID.class), eq(new BigDecimal("100.00"))))
            .thenReturn(session);
        
        String requestBody = String.format("""
            {
                "registerId": "%s",
                "openingAmount": 100.00
            }
            """, registerId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.registerId").value(registerId.toString()))
            .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
            .andExpect(jsonPath("$.openingAmount").value(100.00))
            .andExpect(jsonPath("$.status").value("OPEN"));
        
        verify(cashSessionService).openSession(eq(registerId), any(UUID.class), eq(new BigDecimal("100.00")));
    }
    
    @Test
    void openSession_withNegativeAmount_returnsBadRequest() throws Exception {
        // Given
        when(cashSessionService.openSession(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Opening amount must be non-negative"));
        
        String requestBody = String.format("""
            {
                "registerId": "%s",
                "openingAmount": -10.00
            }
            """, registerId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void openSession_withExistingOpenSession_returnsUnprocessableEntity() throws Exception {
        // Given
        when(cashSessionService.openSession(any(), any(), any()))
            .thenThrow(new IllegalStateException("Register already has an open session"));
        
        String requestBody = String.format("""
            {
                "registerId": "%s",
                "openingAmount": 100.00
            }
            """, registerId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void closeSession_withValidRequest_returnsOk() throws Exception {
        // Given
        CashSession session = new CashSession(tenantId, registerId, employeeId, new BigDecimal("100.00"));
        session.setExpectedClose(new BigDecimal("150.00"));
        session.setActualClose(new BigDecimal("148.00"));
        session.setVariance(new BigDecimal("-2.00"));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        
        when(cashSessionService.closeSession(eq(sessionId), eq(new BigDecimal("148.00"))))
            .thenReturn(session);
        
        String requestBody = """
            {
                "actualAmount": 148.00
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions/" + sessionId + "/close")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.expectedClose").value(150.00))
            .andExpect(jsonPath("$.actualClose").value(148.00))
            .andExpect(jsonPath("$.variance").value(-2.00));
        
        verify(cashSessionService).closeSession(eq(sessionId), eq(new BigDecimal("148.00")));
    }
    
    @Test
    void closeSession_withAlreadyClosedSession_returnsUnprocessableEntity() throws Exception {
        // Given
        when(cashSessionService.closeSession(any(), any()))
            .thenThrow(new IllegalStateException("Session is already closed"));
        
        String requestBody = """
            {
                "actualAmount": 148.00
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions/" + sessionId + "/close")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void recordMovement_withValidRequest_returnsCreated() throws Exception {
        // Given
        CashMovement movement = new CashMovement(tenantId, sessionId, MovementType.DEPOSIT, new BigDecimal("50.00"));
        movement.setReason("Bank deposit");
        
        when(cashSessionService.recordMovement(eq(sessionId), eq(MovementType.DEPOSIT), 
                eq(new BigDecimal("50.00")), eq("Bank deposit")))
            .thenReturn(movement);
        
        String requestBody = """
            {
                "type": "DEPOSIT",
                "amount": 50.00,
                "reason": "Bank deposit"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions/" + sessionId + "/movements")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
            .andExpect(jsonPath("$.movementType").value("DEPOSIT"))
            .andExpect(jsonPath("$.amount").value(50.00))
            .andExpect(jsonPath("$.reason").value("Bank deposit"));
        
        verify(cashSessionService).recordMovement(eq(sessionId), eq(MovementType.DEPOSIT), 
            eq(new BigDecimal("50.00")), eq("Bank deposit"));
    }
    
    @Test
    void recordMovement_withInvalidType_returnsBadRequest() throws Exception {
        // Given
        when(cashSessionService.recordMovement(any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Only DEPOSIT and WITHDRAWAL movements can be recorded manually"));
        
        String requestBody = """
            {
                "type": "SALE",
                "amount": 50.00,
                "reason": "Invalid"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/cash/sessions/" + sessionId + "/movements")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void getSessionSummary_withValidSession_returnsOk() throws Exception {
        // Given
        CashSession session = new CashSession(tenantId, registerId, employeeId, new BigDecimal("100.00"));
        CashMovement movement1 = new CashMovement(tenantId, sessionId, MovementType.OPENING, new BigDecimal("100.00"));
        CashMovement movement2 = new CashMovement(tenantId, sessionId, MovementType.SALE, new BigDecimal("50.00"));
        
        CashSessionSummary summary = new CashSessionSummary(session, List.of(movement1, movement2));
        
        when(cashSessionService.getSessionSummary(eq(sessionId)))
            .thenReturn(summary);
        
        // When & Then
        mockMvc.perform(get("/api/cash/sessions/" + sessionId)
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.session.registerId").value(registerId.toString()))
            .andExpect(jsonPath("$.session.openingAmount").value(100.00))
            .andExpect(jsonPath("$.movements").isArray())
            .andExpect(jsonPath("$.movements.length()").value(2));
        
        verify(cashSessionService).getSessionSummary(eq(sessionId));
    }
    
    @Test
    void getSessionSummary_withNonExistentSession_returnsNotFound() throws Exception {
        // Given
        when(cashSessionService.getSessionSummary(any()))
            .thenThrow(new IllegalArgumentException("Cash session not found"));
        
        // When & Then
        mockMvc.perform(get("/api/cash/sessions/" + sessionId)
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isNotFound());
    }
    
    // Cash Closing Tests
    
    @Test
    void closeRegister_withValidRequest_returnsCreated() throws Exception {
        // Given
        Instant periodStart = Instant.parse("2024-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2024-01-01T23:59:59Z");
        
        CashClosing closing = new CashClosing(
            tenantId,
            ClosingType.REGISTER,
            periodStart,
            periodEnd,
            new BigDecimal("500.00"),
            new BigDecimal("20.00"),
            new BigDecimal("-5.00"),
            employeeId
        );
        
        when(cashClosingService.closeRegister(eq(registerId), eq(periodStart), eq(periodEnd)))
            .thenReturn(closing);
        
        String requestBody = String.format("""
            {
                "registerId": "%s",
                "periodStart": "2024-01-01T00:00:00Z",
                "periodEnd": "2024-01-01T23:59:59Z"
            }
            """, registerId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/closings/register")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.closingType").value("REGISTER"))
            .andExpect(jsonPath("$.totalSales").value(500.00))
            .andExpect(jsonPath("$.totalRefunds").value(20.00))
            .andExpect(jsonPath("$.variance").value(-5.00));
        
        verify(cashClosingService).closeRegister(eq(registerId), eq(periodStart), eq(periodEnd));
    }
    
    @Test
    void closeRegister_withInvalidPeriod_returnsBadRequest() throws Exception {
        // Given
        when(cashClosingService.closeRegister(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Period start must be before period end"));
        
        String requestBody = String.format("""
            {
                "registerId": "%s",
                "periodStart": "2024-01-02T00:00:00Z",
                "periodEnd": "2024-01-01T00:00:00Z"
            }
            """, registerId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/closings/register")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void closeDay_withValidRequest_returnsCreated() throws Exception {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 1);
        Instant periodStart = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant periodEnd = date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        
        CashClosing closing = new CashClosing(
            tenantId,
            ClosingType.DAY,
            periodStart,
            periodEnd,
            new BigDecimal("1500.00"),
            new BigDecimal("50.00"),
            new BigDecimal("-10.00"),
            employeeId
        );
        
        when(cashClosingService.closeDay(eq(siteId), eq(date)))
            .thenReturn(closing);
        
        String requestBody = String.format("""
            {
                "siteId": "%s",
                "date": "2024-01-01"
            }
            """, siteId);
        
        // When & Then
        mockMvc.perform(post("/api/cash/closings/day")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.closingType").value("DAY"))
            .andExpect(jsonPath("$.totalSales").value(1500.00))
            .andExpect(jsonPath("$.totalRefunds").value(50.00));
        
        verify(cashClosingService).closeDay(eq(siteId), eq(date));
    }
    
    @Test
    void getClosingReport_withValidClosing_returnsOk() throws Exception {
        // Given
        Instant periodStart = Instant.parse("2024-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2024-01-01T23:59:59Z");
        
        CashClosing closing = new CashClosing(
            tenantId,
            ClosingType.REGISTER,
            periodStart,
            periodEnd,
            new BigDecimal("500.00"),
            new BigDecimal("20.00"),
            new BigDecimal("-5.00"),
            employeeId
        );
        
        CashSession session = new CashSession(tenantId, registerId, employeeId, new BigDecimal("100.00"));
        session.setExpectedClose(new BigDecimal("150.00"));
        session.setActualClose(new BigDecimal("148.00"));
        session.setVariance(new BigDecimal("-2.00"));
        session.setStatus(CashSessionStatus.CLOSED);
        
        CashMovement movement = new CashMovement(tenantId, session.getId(), MovementType.SALE, new BigDecimal("50.00"));
        
        ClosingReport report = new ClosingReport(closing, List.of(session), List.of(movement));
        
        when(cashClosingService.generateClosingReport(eq(closingId)))
            .thenReturn(report);
        
        // When & Then
        mockMvc.perform(get("/api/cash/closings/" + closingId + "/report")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closing.closingType").value("REGISTER"))
            .andExpect(jsonPath("$.sessions").isArray())
            .andExpect(jsonPath("$.sessions.length()").value(1))
            .andExpect(jsonPath("$.movements").isArray())
            .andExpect(jsonPath("$.movements.length()").value(1))
            .andExpect(jsonPath("$.openingBalance").value(100.00))
            .andExpect(jsonPath("$.expectedClose").value(150.00))
            .andExpect(jsonPath("$.actualClose").value(148.00));
        
        verify(cashClosingService).generateClosingReport(eq(closingId));
    }
    
    @Test
    void getClosingReport_withNonExistentClosing_returnsNotFound() throws Exception {
        // Given
        when(cashClosingService.generateClosingReport(any()))
            .thenThrow(new IllegalArgumentException("Cash closing not found"));
        
        // When & Then
        mockMvc.perform(get("/api/cash/closings/" + closingId + "/report")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void reprintClosingReport_withValidClosing_returnsOk() throws Exception {
        // Given
        Instant periodStart = Instant.parse("2024-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2024-01-01T23:59:59Z");
        
        CashClosing closing = new CashClosing(
            tenantId,
            ClosingType.REGISTER,
            periodStart,
            periodEnd,
            new BigDecimal("500.00"),
            new BigDecimal("20.00"),
            new BigDecimal("-5.00"),
            employeeId
        );
        
        ClosingReport report = new ClosingReport(closing, List.of(), List.of());
        
        when(cashClosingService.reprintClosingReport(eq(closingId)))
            .thenReturn(report);
        
        // When & Then
        mockMvc.perform(post("/api/cash/closings/" + closingId + "/reprint")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closing.closingType").value("REGISTER"));
        
        verify(cashClosingService).reprintClosingReport(eq(closingId));
    }
    
    @Test
    void reprintClosingReport_withoutPermission_returnsForbidden() throws Exception {
        // Given
        when(cashClosingService.reprintClosingReport(any()))
            .thenThrow(new RuntimeException("User does not have permission: REPRINT_DOCUMENT"));
        
        // When & Then
        mockMvc.perform(post("/api/cash/closings/" + closingId + "/reprint")
                .with(jwt().jwt(builder -> builder.subject(employeeId.toString()))))
            .andExpect(status().isForbidden());
    }
}
