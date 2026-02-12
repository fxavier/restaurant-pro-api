package com.restaurantpos.diningroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.diningroom.entity.BlacklistEntry;
import com.restaurantpos.diningroom.entity.DiningTable;
import com.restaurantpos.diningroom.model.EntityType;
import com.restaurantpos.diningroom.model.TableStatus;
import com.restaurantpos.diningroom.repository.BlacklistEntryRepository;
import com.restaurantpos.diningroom.repository.DiningTableRepository;
import com.restaurantpos.diningroom.service.TableManagementService;
import com.restaurantpos.identityaccess.tenant.TenantContext;


/**
 * Unit tests for TableManagementService.
 * 
 * Tests requirements: 3.1, 3.4, 3.5, 3.6, 3.8
 */
@ExtendWith(MockitoExtension.class)
class TableManagementServiceTest {
    
    @Mock
    private DiningTableRepository tableRepository;
    
    @Mock
    private BlacklistEntryRepository blacklistRepository;
    
    @InjectMocks
    private TableManagementService service;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID tableId;
    private DiningTable table;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        
        TenantContext.setTenantId(tenantId);
        
        table = new DiningTable(tenantId, siteId, "T1", "Main", 4);
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    void getTableMap_shouldReturnAllTablesForSite() {
        // Given
        DiningTable table1 = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        DiningTable table2 = new DiningTable(tenantId, siteId, "T2", "Main", 2);
        List<DiningTable> tables = List.of(table1, table2);
        
        when(tableRepository.findByTenantIdAndSiteId(tenantId, siteId)).thenReturn(tables);
        
        // When
        List<DiningTable> result = service.getTableMap(siteId);
        
        // Then
        assertEquals(2, result.size());
        verify(tableRepository).findByTenantIdAndSiteId(tenantId, siteId);
    }
    
    @Test
    void openTable_shouldTransitionToOccupied() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, tableId.toString())).thenReturn(false);
        
        // When
        service.openTable(tableId);
        
        // Then
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
        verify(tableRepository).save(table);
    }
    
    @Test
    void openTable_shouldThrowExceptionIfTableNotFound() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.openTable(tableId));
    }
    
    @Test
    void openTable_shouldThrowExceptionIfTableBlocked() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, tableId.toString())).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> service.openTable(tableId));
    }
    
    @Test
    void closeTable_shouldTransitionToAvailable() {
        // Given
        table.setStatus(TableStatus.OCCUPIED);
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        
        // When
        service.closeTable(tableId);
        
        // Then
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        verify(tableRepository).save(table);
    }
    
    @Test
    void closeTable_shouldThrowExceptionIfTableNotFound() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.closeTable(tableId));
    }
    
    @Test
    void transferTable_shouldUpdateBothTableStates() {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        DiningTable fromTable = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        fromTable.setStatus(TableStatus.OCCUPIED);
        
        DiningTable toTable = new DiningTable(tenantId, siteId, "T2", "Main", 2);
        toTable.setStatus(TableStatus.AVAILABLE);
        
        when(tableRepository.findByIdAndTenantId(fromTableId, tenantId)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByIdAndTenantId(toTableId, tenantId)).thenReturn(Optional.of(toTable));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, toTableId.toString())).thenReturn(false);
        
        // When
        service.transferTable(fromTableId, toTableId);
        
        // Then
        assertEquals(TableStatus.AVAILABLE, fromTable.getStatus());
        assertEquals(TableStatus.OCCUPIED, toTable.getStatus());
        verify(tableRepository).save(fromTable);
        verify(tableRepository).save(toTable);
    }
    
    @Test
    void transferTable_shouldThrowExceptionIfSourceTableNotFound() {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        when(tableRepository.findByIdAndTenantId(fromTableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> service.transferTable(fromTableId, toTableId));
    }
    
    @Test
    void transferTable_shouldThrowExceptionIfDestinationTableNotFound() {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        DiningTable fromTable = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        
        when(tableRepository.findByIdAndTenantId(fromTableId, tenantId)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByIdAndTenantId(toTableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> service.transferTable(fromTableId, toTableId));
    }
    
    @Test
    void transferTable_shouldThrowExceptionIfDestinationTableBlocked() {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        DiningTable fromTable = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        DiningTable toTable = new DiningTable(tenantId, siteId, "T2", "Main", 2);
        
        when(tableRepository.findByIdAndTenantId(fromTableId, tenantId)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByIdAndTenantId(toTableId, tenantId)).thenReturn(Optional.of(toTable));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, toTableId.toString())).thenReturn(true);
        
        // When & Then
        assertThrows(IllegalStateException.class, 
                () -> service.transferTable(fromTableId, toTableId));
    }
    
    @Test
    void transferTable_shouldThrowExceptionIfDestinationTableNotAvailable() {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        DiningTable fromTable = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        DiningTable toTable = new DiningTable(tenantId, siteId, "T2", "Main", 2);
        toTable.setStatus(TableStatus.OCCUPIED);
        
        when(tableRepository.findByIdAndTenantId(fromTableId, tenantId)).thenReturn(Optional.of(fromTable));
        when(tableRepository.findByIdAndTenantId(toTableId, tenantId)).thenReturn(Optional.of(toTable));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, toTableId.toString())).thenReturn(false);
        
        // When & Then
        assertThrows(IllegalStateException.class, 
                () -> service.transferTable(fromTableId, toTableId));
    }
    
    @Test
    void blockTable_shouldAddToBlacklistAndUpdateStatus() {
        // Given
        String reason = "Broken chair";
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, tableId.toString())).thenReturn(false);
        
        // When
        service.blockTable(tableId, reason);
        
        // Then
        assertEquals(TableStatus.BLOCKED, table.getStatus());
        verify(blacklistRepository).save(any(BlacklistEntry.class));
        verify(tableRepository).save(table);
    }
    
    @Test
    void blockTable_shouldNotDuplicateBlacklistEntry() {
        // Given
        String reason = "Broken chair";
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        when(blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, tableId.toString())).thenReturn(true);
        
        // When
        service.blockTable(tableId, reason);
        
        // Then
        assertEquals(TableStatus.BLOCKED, table.getStatus());
        verify(blacklistRepository, never()).save(any(BlacklistEntry.class));
        verify(tableRepository).save(table);
    }
    
    @Test
    void blockTable_shouldThrowExceptionIfTableNotFound() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> service.blockTable(tableId, "Reason"));
    }
    
    @Test
    void unblockTable_shouldRemoveFromBlacklistAndUpdateStatus() {
        // Given
        table.setStatus(TableStatus.BLOCKED);
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.of(table));
        
        // When
        service.unblockTable(tableId);
        
        // Then
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        verify(blacklistRepository).deleteByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, tableId.toString());
        verify(tableRepository).save(table);
    }
    
    @Test
    void unblockTable_shouldThrowExceptionIfTableNotFound() {
        // Given
        when(tableRepository.findByIdAndTenantId(tableId, tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.unblockTable(tableId));
    }
    
    @Test
    void shouldThrowExceptionIfTenantContextNotSet() {
        // Given
        TenantContext.clear();
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> service.getTableMap(siteId));
    }
}
