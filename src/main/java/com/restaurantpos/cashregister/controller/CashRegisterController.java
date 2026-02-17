package com.restaurantpos.cashregister.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * REST controller for cash register operations.
 * Provides endpoints for cash session management, cash movements, and cash closings.
 * 
 * Requirements: 10.2, 10.3, 10.5, 10.6, 10.7, 10.8, 10.9
 */
@RestController
@RequestMapping("/api/cash")
public class CashRegisterController {
    
    private static final Logger logger = LoggerFactory.getLogger(CashRegisterController.class);
    
    private final CashSessionService cashSessionService;
    private final CashClosingService cashClosingService;
    
    public CashRegisterController(
            CashSessionService cashSessionService,
            CashClosingService cashClosingService) {
        this.cashSessionService = cashSessionService;
        this.cashClosingService = cashClosingService;
    }
    
    /**
     * Opens a new cash session.
     * 
     * POST /api/cash/sessions
     * 
     * Requirements: 10.2
     */
    @PostMapping("/sessions")
    public ResponseEntity<CashSessionResponse> openSession(
            @Valid @RequestBody OpenSessionRequest request) {
        try {
            UUID employeeId = extractUserIdFromAuthentication();
            
            logger.info("Opening cash session for register {}: employee {}, opening amount {}", 
                request.registerId(), employeeId, request.openingAmount());
            
            CashSession session = cashSessionService.openSession(
                request.registerId(),
                employeeId,
                request.openingAmount()
            );
            
            CashSessionResponse response = toCashSessionResponse(session);
            logger.info("Cash session opened successfully: {}", session.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to open cash session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cash session opening failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error opening cash session: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Closes a cash session.
     * 
     * POST /api/cash/sessions/{id}/close
     * 
     * Requirements: 10.3, 10.6
     */
    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<CashSessionResponse> closeSession(
            @PathVariable UUID id,
            @Valid @RequestBody CloseSessionRequest request) {
        try {
            logger.info("Closing cash session {}: actual amount {}", id, request.actualAmount());
            
            CashSession session = cashSessionService.closeSession(id, request.actualAmount());
            
            CashSessionResponse response = toCashSessionResponse(session);
            logger.info("Cash session closed successfully: {}, variance: {}", 
                session.getId(), session.getVariance());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to close cash session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cash session closing failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error closing cash session: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Records a manual cash movement (deposit or withdrawal).
     * 
     * POST /api/cash/sessions/{id}/movements
     * 
     * Requirements: 10.5
     */
    @PostMapping("/sessions/{id}/movements")
    public ResponseEntity<CashMovementResponse> recordMovement(
            @PathVariable UUID id,
            @Valid @RequestBody RecordMovementRequest request) {
        try {
            logger.info("Recording cash movement for session {}: type {}, amount {}", 
                id, request.type(), request.amount());
            
            CashMovement movement = cashSessionService.recordMovement(
                id,
                request.type(),
                request.amount(),
                request.reason()
            );
            
            CashMovementResponse response = toCashMovementResponse(movement);
            logger.info("Cash movement recorded successfully: {}", movement.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to record cash movement: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cash movement recording failed due to state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error recording cash movement: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets a cash session summary with all movements.
     * 
     * GET /api/cash/sessions/{id}
     * 
     * Requirements: 10.5
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<CashSessionSummaryResponse> getSessionSummary(@PathVariable UUID id) {
        try {
            logger.debug("Fetching cash session summary: {}", id);
            
            CashSessionSummary summary = cashSessionService.getSessionSummary(id);
            CashSessionSummaryResponse response = toCashSessionSummaryResponse(summary);
            
            logger.debug("Retrieved cash session summary for {}: {} movements", 
                id, response.movements().size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to fetch cash session summary: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching cash session summary: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Closes a cash register for a specific period.
     * 
     * POST /api/cash/closings/register
     * 
     * Requirements: 10.7
     */
    @PostMapping("/closings/register")
    public ResponseEntity<CashClosingResponse> closeRegister(
            @Valid @RequestBody CloseRegisterRequest request) {
        try {
            logger.info("Closing register {}: period {} to {}", 
                request.registerId(), request.periodStart(), request.periodEnd());
            
            CashClosing closing = cashClosingService.closeRegister(
                request.registerId(),
                request.periodStart(),
                request.periodEnd()
            );
            
            CashClosingResponse response = toCashClosingResponse(closing);
            logger.info("Register closed successfully: {}", closing.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to close register: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error closing register: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Closes a day for a specific site.
     * 
     * POST /api/cash/closings/day
     * 
     * Requirements: 10.7
     */
    @PostMapping("/closings/day")
    public ResponseEntity<CashClosingResponse> closeDay(
            @Valid @RequestBody CloseDayRequest request) {
        try {
            logger.info("Closing day for site {}: date {}", request.siteId(), request.date());
            
            CashClosing closing = cashClosingService.closeDay(
                request.siteId(),
                request.date()
            );
            
            CashClosingResponse response = toCashClosingResponse(closing);
            logger.info("Day closed successfully: {}", closing.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to close day: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error closing day: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets a closing report.
     * 
     * GET /api/cash/closings/{id}/report
     * 
     * Requirements: 10.8
     */
    @GetMapping("/closings/{id}/report")
    public ResponseEntity<ClosingReportResponse> getClosingReport(@PathVariable UUID id) {
        try {
            logger.debug("Fetching closing report: {}", id);
            
            ClosingReport report = cashClosingService.generateClosingReport(id);
            ClosingReportResponse response = toClosingReportResponse(report);
            
            logger.debug("Retrieved closing report for {}: {} sessions, {} movements", 
                id, response.sessions().size(), response.movements().size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to fetch closing report: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching closing report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Reprints a closing report.
     * Requires REPRINT_DOCUMENT permission.
     * 
     * POST /api/cash/closings/{id}/reprint
     * 
     * Requirements: 10.9
     */
    @PostMapping("/closings/{id}/reprint")
    public ResponseEntity<ClosingReportResponse> reprintClosingReport(@PathVariable UUID id) {
        try {
            logger.info("Reprinting closing report: {}", id);
            
            ClosingReport report = cashClosingService.reprintClosingReport(id);
            ClosingReportResponse response = toClosingReportResponse(report);
            
            logger.info("Closing report reprinted successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to reprint closing report: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("permission")) {
                logger.warn("Permission denied for reprinting closing report: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            logger.error("Error reprinting closing report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error reprinting closing report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    /**
     * Extracts the user ID from the current authentication context.
     */
    private UUID extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Invalid user ID in JWT: " + subject, e);
                }
            }
        }
        
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
    
    private CashSessionResponse toCashSessionResponse(CashSession session) {
        return new CashSessionResponse(
            session.getId(),
            session.getTenantId(),
            session.getRegisterId(),
            session.getEmployeeId(),
            session.getOpeningAmount(),
            session.getExpectedClose(),
            session.getActualClose(),
            session.getVariance(),
            session.getStatus(),
            session.getOpenedAt(),
            session.getClosedAt(),
            session.getVersion()
        );
    }
    
    private CashMovementResponse toCashMovementResponse(CashMovement movement) {
        return new CashMovementResponse(
            movement.getId(),
            movement.getTenantId(),
            movement.getSessionId(),
            movement.getMovementType(),
            movement.getAmount(),
            movement.getReason(),
            movement.getPaymentId(),
            movement.getCreatedAt(),
            movement.getCreatedBy()
        );
    }
    
    private CashSessionSummaryResponse toCashSessionSummaryResponse(CashSessionSummary summary) {
        CashSessionResponse sessionResponse = toCashSessionResponse(summary.getSession());
        List<CashMovementResponse> movementResponses = summary.getMovements().stream()
            .map(this::toCashMovementResponse)
            .toList();
        
        return new CashSessionSummaryResponse(sessionResponse, movementResponses);
    }
    
    private CashClosingResponse toCashClosingResponse(CashClosing closing) {
        return new CashClosingResponse(
            closing.getId(),
            closing.getTenantId(),
            closing.getClosingType(),
            closing.getPeriodStart(),
            closing.getPeriodEnd(),
            closing.getTotalSales(),
            closing.getTotalRefunds(),
            closing.getVariance(),
            closing.getClosedAt(),
            closing.getClosedBy()
        );
    }
    
    private ClosingReportResponse toClosingReportResponse(ClosingReport report) {
        CashClosingResponse closingResponse = toCashClosingResponse(report.getClosing());
        List<CashSessionResponse> sessionResponses = report.getSessions().stream()
            .map(this::toCashSessionResponse)
            .toList();
        List<CashMovementResponse> movementResponses = report.getMovements().stream()
            .map(this::toCashMovementResponse)
            .toList();
        
        return new ClosingReportResponse(
            closingResponse,
            sessionResponses,
            movementResponses,
            report.getOpeningBalance(),
            report.getExpectedClose(),
            report.getActualClose()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for opening a cash session.
     */
    public record OpenSessionRequest(
        @NotNull UUID registerId,
        @NotNull @PositiveOrZero BigDecimal openingAmount
    ) {}
    
    /**
     * Request DTO for closing a cash session.
     */
    public record CloseSessionRequest(
        @NotNull @PositiveOrZero BigDecimal actualAmount
    ) {}
    
    /**
     * Request DTO for recording a cash movement.
     */
    public record RecordMovementRequest(
        @NotNull MovementType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull String reason
    ) {}
    
    /**
     * Request DTO for closing a register.
     */
    public record CloseRegisterRequest(
        @NotNull UUID registerId,
        @NotNull Instant periodStart,
        @NotNull Instant periodEnd
    ) {}
    
    /**
     * Request DTO for closing a day.
     */
    public record CloseDayRequest(
        @NotNull UUID siteId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for cash session details.
     */
    public record CashSessionResponse(
        UUID id,
        UUID tenantId,
        UUID registerId,
        UUID employeeId,
        BigDecimal openingAmount,
        BigDecimal expectedClose,
        BigDecimal actualClose,
        BigDecimal variance,
        CashSessionStatus status,
        Instant openedAt,
        Instant closedAt,
        Integer version
    ) {}
    
    /**
     * Response DTO for cash movement details.
     */
    public record CashMovementResponse(
        UUID id,
        UUID tenantId,
        UUID sessionId,
        MovementType movementType,
        BigDecimal amount,
        String reason,
        UUID paymentId,
        Instant createdAt,
        UUID createdBy
    ) {}
    
    /**
     * Response DTO for cash session summary.
     */
    public record CashSessionSummaryResponse(
        CashSessionResponse session,
        List<CashMovementResponse> movements
    ) {}
    
    /**
     * Response DTO for cash closing details.
     */
    public record CashClosingResponse(
        UUID id,
        UUID tenantId,
        ClosingType closingType,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal totalSales,
        BigDecimal totalRefunds,
        BigDecimal variance,
        Instant closedAt,
        UUID closedBy
    ) {}
    
    /**
     * Response DTO for closing report.
     */
    public record ClosingReportResponse(
        CashClosingResponse closing,
        List<CashSessionResponse> sessions,
        List<CashMovementResponse> movements,
        BigDecimal openingBalance,
        BigDecimal expectedClose,
        BigDecimal actualClose
    ) {}
}
