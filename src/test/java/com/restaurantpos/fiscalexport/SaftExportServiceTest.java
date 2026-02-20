package com.restaurantpos.fiscalexport;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.fiscalexport.service.SaftExportService;
import com.restaurantpos.paymentsbilling.repository.FiscalDocumentRepository;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;

/**
 * Unit tests for SaftExportService.
 * Tests XML generation and schema validation.
 * 
 * Requirements: 17.1, 17.2, 17.3
 */
@ExtendWith(MockitoExtension.class)
class SaftExportServiceTest {
    
    @Mock
    private FiscalDocumentRepository fiscalDocumentRepository;
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private ItemRepository itemRepository;
    
    private SaftExportService saftExportService;
    
    private UUID tenantId;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @BeforeEach
    void setUp() {
        saftExportService = new SaftExportService(
            fiscalDocumentRepository,
            paymentRepository,
            customerRepository,
            itemRepository
        );
        
        tenantId = UUID.randomUUID();
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
    }
    
    @Test
    void generateExport_withEmptyData_shouldGenerateValidXml() {
        // Given - no fiscal documents, payments, customers, or items
        when(fiscalDocumentRepository.findByTenantIdAndIssuedAtBetween(eq(tenantId), any(), any()))
            .thenReturn(List.of());
        when(paymentRepository.findByTenantIdAndOrderIdIn(eq(tenantId), any()))
            .thenReturn(List.of());
        when(customerRepository.findByTenantId(tenantId))
            .thenReturn(List.of());
        when(itemRepository.findByTenantId(tenantId))
            .thenReturn(List.of());
        
        // When
        String xml = saftExportService.generateExport(tenantId, startDate, endDate);
        
        // Then - should generate valid XML structure that passes schema validation
        assertThat(xml).isNotNull();
        assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<AuditFile xmlns=\"urn:OECD:StandardAuditFile-Tax:PT_1.04_01\">");
        assertThat(xml).contains("<Header>");
        assertThat(xml).contains("<MasterFiles>");
        assertThat(xml).contains("<SourceDocuments>");
        assertThat(xml).contains("<NumberOfEntries>0</NumberOfEntries>");
        assertThat(xml).contains("</AuditFile>");
    }
    
    @Test
    void generateExport_withEmptyData_shouldPassSchemaValidation() {
        // Given - no fiscal documents, payments, customers, or items
        when(fiscalDocumentRepository.findByTenantIdAndIssuedAtBetween(eq(tenantId), any(), any()))
            .thenReturn(List.of());
        when(paymentRepository.findByTenantIdAndOrderIdIn(eq(tenantId), any()))
            .thenReturn(List.of());
        when(customerRepository.findByTenantId(tenantId))
            .thenReturn(List.of());
        when(itemRepository.findByTenantId(tenantId))
            .thenReturn(List.of());
        
        // When - generateExport internally validates against schema
        String xml = saftExportService.generateExport(tenantId, startDate, endDate);
        
        // Then - if we get here without exception, schema validation passed
        assertThat(xml).isNotNull();
    }
    
    @Test
    void saftExportService_shouldLoadSchemaOnConstruction() {
        // When - service is constructed (in setUp)
        // Then - no exception should be thrown, meaning schema loaded successfully
        assertThat(saftExportService).isNotNull();
    }
}
