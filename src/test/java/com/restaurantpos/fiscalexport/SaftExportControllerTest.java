package com.restaurantpos.fiscalexport;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.restaurantpos.fiscalexport.controller.SaftExportController;
import com.restaurantpos.fiscalexport.service.SaftExportService;

/**
 * Unit tests for SaftExportController.
 * Tests SAF-T PT export endpoint with audit logging.
 * 
 * Requirements: 17.1, 17.4, 17.5
 */
@ExtendWith(MockitoExtension.class)
class SaftExportControllerTest {
    
    @Mock
    private SaftExportService saftExportService;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private Jwt jwt;
    
    private SaftExportController controller;
    
    private UUID tenantId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        controller = new SaftExportController(saftExportService);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(tenantId.toString());
        when(jwt.getSubject()).thenReturn(userId.toString());
    }
    
    @Test
    void generateSaftExport_withValidRequest_returnsXmlFile() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String xmlContent = "<?xml version=\"1.0\"?><AuditFile>test</AuditFile>";
        
        when(saftExportService.generateExport(tenantId, startDate, endDate))
            .thenReturn(xmlContent);
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(startDate, endDate);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(xmlContent, response.getBody());
        assertEquals(MediaType.APPLICATION_XML, response.getHeaders().getContentType());
        
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.startsWith("attachment; filename="));
        assertTrue(contentDisposition.contains("saft-pt-"));
        assertTrue(contentDisposition.contains(tenantId.toString()));
        assertTrue(contentDisposition.contains("2024-01-01"));
        assertTrue(contentDisposition.contains("2024-12-31"));
        
        verify(saftExportService).generateExport(tenantId, startDate, endDate);
    }
    
    @Test
    void generateSaftExport_withInvalidDateRange_returnsBadRequest() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        
        when(saftExportService.generateExport(tenantId, startDate, endDate))
            .thenThrow(new IllegalArgumentException("Invalid date range"));
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(startDate, endDate);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(saftExportService).generateExport(tenantId, startDate, endDate);
    }
    
    @Test
    void generateSaftExport_withServiceError_returnsInternalServerError() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        
        when(saftExportService.generateExport(tenantId, startDate, endDate))
            .thenThrow(new RuntimeException("Database error"));
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(startDate, endDate);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(saftExportService).generateExport(tenantId, startDate, endDate);
    }
    
    @Test
    void generateSaftExport_extractsTenantFromJwt() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String xmlContent = "<?xml version=\"1.0\"?><AuditFile>test</AuditFile>";
        
        when(saftExportService.generateExport(tenantId, startDate, endDate))
            .thenReturn(xmlContent);
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(startDate, endDate);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jwt).getClaimAsString("tenant_id");
        verify(jwt).getSubject();
        verify(saftExportService).generateExport(tenantId, startDate, endDate);
    }
    
    @Test
    void generateSaftExport_withLargeXml_returnsCompleteContent() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        
        // Generate large XML content (simulating real export)
        StringBuilder largeXml = new StringBuilder("<?xml version=\"1.0\"?><AuditFile>");
        for (int i = 0; i < 1000; i++) {
            largeXml.append("<Invoice><InvoiceNo>INV-").append(i).append("</InvoiceNo></Invoice>");
        }
        largeXml.append("</AuditFile>");
        String xmlContent = largeXml.toString();
        
        when(saftExportService.generateExport(tenantId, startDate, endDate))
            .thenReturn(xmlContent);
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(startDate, endDate);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(xmlContent, response.getBody());
        assertTrue(response.getBody().length() > 10000);
        verify(saftExportService).generateExport(tenantId, startDate, endDate);
    }
    
    @Test
    void generateSaftExport_withSameDayRange_succeeds() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 6, 15);
        String xmlContent = "<?xml version=\"1.0\"?><AuditFile>single day</AuditFile>";
        
        when(saftExportService.generateExport(tenantId, date, date))
            .thenReturn(xmlContent);
        
        SaftExportController.SaftExportRequest request = 
            new SaftExportController.SaftExportRequest(date, date);
        
        // Act
        ResponseEntity<String> response = controller.generateSaftExport(request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(xmlContent, response.getBody());
        assertNotNull(response.getBody());
        verify(saftExportService).generateExport(tenantId, date, date);
    }
}
