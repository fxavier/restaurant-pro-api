package com.restaurantpos.diningroom.service;

import com.restaurantpos.diningroom.entity.BlacklistEntry;
import com.restaurantpos.diningroom.entity.DiningTable;
import com.restaurantpos.diningroom.model.EntityType;
import com.restaurantpos.diningroom.model.TableStatus;
import com.restaurantpos.diningroom.repository.BlacklistEntryRepository;
import com.restaurantpos.diningroom.repository.DiningTableRepository;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing dining room tables and their operations.
 * Handles table status transitions, transfers, and blacklist management.
 * 
 * Requirements: 3.1, 3.4, 3.5, 3.6, 3.8
 */
@Service
@Transactional
public class TableManagementService {
    
    private final DiningTableRepository tableRepository;
    private final BlacklistEntryRepository blacklistRepository;
    
    public TableManagementService(
            DiningTableRepository tableRepository,
            BlacklistEntryRepository blacklistRepository) {
        this.tableRepository = tableRepository;
        this.blacklistRepository = blacklistRepository;
    }
    
    /**
     * Gets the table map for a site showing all tables with their current status.
     * 
     * Requirement: 3.1 - Display real-time table map
     * 
     * @param siteId the site ID
     * @return list of all tables for the site
     */
    @Transactional(readOnly = true)
    public List<DiningTable> getTableMap(UUID siteId) {
        UUID tenantId = getTenantId();
        return tableRepository.findByTenantIdAndSiteId(tenantId, siteId);
    }
    
    /**
     * Opens a table by transitioning it to OCCUPIED status.
     * 
     * Requirement: 3.4 - Transition table to OCCUPIED when opened
     * 
     * @param tableId the table ID
     * @throws IllegalStateException if table is blocked
     * @throws IllegalArgumentException if table not found
     */
    public void openTable(UUID tableId) {
        UUID tenantId = getTenantId();
        
        DiningTable table = tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        
        // Check if table is blacklisted
        if (isTableBlocked(tableId)) {
            throw new IllegalStateException("Cannot open blocked table: " + tableId);
        }
        
        table.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);
    }
    
    /**
     * Closes a table by transitioning it to AVAILABLE status.
     * 
     * Requirement: 3.8 - Transition table to AVAILABLE when all orders closed
     * 
     * @param tableId the table ID
     * @throws IllegalArgumentException if table not found
     */
    public void closeTable(UUID tableId) {
        UUID tenantId = getTenantId();
        
        DiningTable table = tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        
        table.setStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }
    
    /**
     * Transfers orders from one table to another.
     * Updates both table states accordingly.
     * 
     * Requirement: 3.5 - Move all order lines and update both table states
     * 
     * Note: Actual order transfer logic will be implemented in the orders module.
     * This method handles the table state transitions.
     * 
     * @param fromTableId the source table ID
     * @param toTableId the destination table ID
     * @throws IllegalStateException if destination table is blocked or not available
     * @throws IllegalArgumentException if either table not found
     */
    public void transferTable(UUID fromTableId, UUID toTableId) {
        UUID tenantId = getTenantId();
        
        DiningTable fromTable = tableRepository.findByIdAndTenantId(fromTableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Source table not found: " + fromTableId));
        
        DiningTable toTable = tableRepository.findByIdAndTenantId(toTableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Destination table not found: " + toTableId));
        
        // Check if destination table is blacklisted
        if (isTableBlocked(toTableId)) {
            throw new IllegalStateException("Cannot transfer to blocked table: " + toTableId);
        }
        
        // Check if destination table is available
        if (!toTable.isAvailable()) {
            throw new IllegalStateException("Destination table is not available: " + toTableId);
        }
        
        // TODO: Transfer orders from fromTable to toTable (will be implemented in orders module)
        // For now, we just update the table states
        
        // Update table states
        fromTable.setStatus(TableStatus.AVAILABLE);
        toTable.setStatus(TableStatus.OCCUPIED);
        
        tableRepository.save(fromTable);
        tableRepository.save(toTable);
    }
    
    /**
     * Blocks a table by adding it to the blacklist.
     * Prevents any operations on the table.
     * 
     * Requirement: 3.6 - Prevent operations on blacklisted tables
     * 
     * @param tableId the table ID
     * @param reason the reason for blocking
     * @throws IllegalArgumentException if table not found
     */
    public void blockTable(UUID tableId, String reason) {
        UUID tenantId = getTenantId();
        
        // Verify table exists
        DiningTable table = tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        
        // Add to blacklist
        String entityValue = tableId.toString();
        
        // Check if already blacklisted
        if (!blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, entityValue)) {
            
            BlacklistEntry entry = new BlacklistEntry(
                    tenantId,
                    EntityType.TABLE,
                    entityValue,
                    reason,
                    null // createdBy will be set by audit aspect if implemented
            );
            
            blacklistRepository.save(entry);
        }
        
        // Update table status to BLOCKED
        table.setStatus(TableStatus.BLOCKED);
        tableRepository.save(table);
    }
    
    /**
     * Unblocks a table by removing it from the blacklist.
     * 
     * Requirement: 3.6 - Allow unblocking tables
     * 
     * @param tableId the table ID
     * @throws IllegalArgumentException if table not found
     */
    public void unblockTable(UUID tableId) {
        UUID tenantId = getTenantId();
        
        // Verify table exists
        DiningTable table = tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        
        // Remove from blacklist
        String entityValue = tableId.toString();
        blacklistRepository.deleteByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, entityValue);
        
        // Update table status to AVAILABLE
        table.setStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }
    
    /**
     * Checks if a table is blocked.
     * 
     * @param tableId the table ID
     * @return true if the table is blacklisted
     */
    private boolean isTableBlocked(UUID tableId) {
        UUID tenantId = getTenantId();
        String entityValue = tableId.toString();
        return blacklistRepository.existsByTenantIdAndEntityTypeAndEntityValue(
                tenantId, EntityType.TABLE, entityValue);
    }
    
    /**
     * Gets the current tenant ID from the thread-local context.
     * 
     * @return the tenant ID
     * @throws IllegalStateException if tenant context is not set
     */
    private UUID getTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenantId;
    }
}
