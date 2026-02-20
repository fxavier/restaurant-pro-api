package com.restaurantpos.fiscalexport.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.fiscalexport.service.SaftExportService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for SAF-T PT fiscal export operations.
 * Provides endpoints for generating SAF-T PT XML exports with audit logging.
 * 
 * Requirements: 17.1, 17.4, 17.5
 */
@RestController
@RequestMapping("/api/exports")
public class SaftExportController {
    
    private static final Logger logger = LoggerFactory.getLogger(SaftExportController.class);
    
    private final SaftExportService saftExportService;
    
    public SaftExportController(SaftExportService saftExportService) {
        this.saftExportService = saftExportService;
    }
    
    /**
     * Generates a SAF-T PT XML export for the specified tenant and date range.
     * Returns the XML file as a downloadable attachment.
     * Logs the export operation with user, timestamp, and date range for audit purposes.
     * 
     * POST /api/exports/saft-pt
     * 
     * Requirements: 17.1, 17.4, 17.5
     */
    @PostMapping("/saft-pt")
    public ResponseEntity<String> generateSaftExport(@Valid @RequestBody SaftExportRequest request) {
        try {
            UUID tenantId = extractTenantIdFromAuthentication();
            UUID userId = extractUserIdFromAuthentication();
            
            // Log export operation for audit trail (Requirement 17.5)
            logger.info("SAF-T PT export requested by user {} for tenant {} from {} to {}", 
                userId, tenantId, request.startDate(), request.endDate());
            
            // Generate export (Requirements 17.1, 17.4)
            String xmlContent = saftExportService.generateExport(
                tenantId,
                request.startDate(),
                request.endDate()
            );
            
            // Log successful export
            logger.info("SAF-T PT export completed successfully for tenant {} from {} to {}: {} bytes", 
                tenantId, request.startDate(), request.endDate(), xmlContent.length());
            
            // Return XML as downloadable file
            String filename = String.format("saft-pt-%s-%s-%s.xml", 
                tenantId, request.startDate(), request.endDate());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlContent);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid SAF-T export request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error generating SAF-T export: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Extracts the tenant ID from the current authentication context.
     */
    private UUID extractTenantIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String tenantIdClaim = jwt.getClaimAsString("tenant_id");
            if (tenantIdClaim != null) {
                try {
                    return UUID.fromString(tenantIdClaim);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Invalid tenant ID in JWT: " + tenantIdClaim, e);
                }
            }
        }
        
        throw new IllegalStateException("Unable to extract tenant ID from authentication");
    }
    
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
    
    /**
     * Request DTO for SAF-T PT export.
     */
    public record SaftExportRequest(
        @NotNull(message = "Start date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @NotNull(message = "End date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {}
}
