package com.restaurantpos.diningroom.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.diningroom.entity.DiningTable;
import com.restaurantpos.diningroom.model.TableStatus;
import com.restaurantpos.diningroom.service.TableManagementService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for table management operations.
 * Provides endpoints for viewing table maps, opening/closing tables,
 * transferring orders between tables, and managing table blacklist.
 * 
 * Requirements: 3.1, 3.4, 3.5, 3.6
 */
@RestController
@RequestMapping("/api/tables")
public class TableController {
    
    private static final Logger logger = LoggerFactory.getLogger(TableController.class);
    
    private final TableManagementService tableManagementService;
    
    public TableController(TableManagementService tableManagementService) {
        this.tableManagementService = tableManagementService;
    }
    
    /**
     * Gets the table map for a site showing all tables with their current status.
     * 
     * GET /api/tables?siteId={siteId}
     * 
     * Requirements: 3.1
     */
    @GetMapping
    public ResponseEntity<List<TableResponse>> getTableMap(@RequestParam UUID siteId) {
        try {
            logger.debug("Fetching table map for site: {}", siteId);
            
            List<DiningTable> tables = tableManagementService.getTableMap(siteId);
            List<TableResponse> response = tables.stream()
                    .map(this::toTableResponse)
                    .toList();
            
            logger.debug("Retrieved {} tables for site {}", response.size(), siteId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Failed to fetch table map: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Error fetching table map: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Opens a table by transitioning it to OCCUPIED status.
     * 
     * POST /api/tables/{id}/open
     * 
     * Requirements: 3.4
     */
    @PostMapping("/{id}/open")
    public ResponseEntity<Void> openTable(@PathVariable UUID id) {
        try {
            logger.info("Opening table: {}", id);
            
            tableManagementService.openTable(id);
            
            logger.info("Table opened successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Table not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot open table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error opening table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Closes a table by transitioning it to AVAILABLE status.
     * 
     * POST /api/tables/{id}/close
     * 
     * Requirements: 3.8
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeTable(@PathVariable UUID id) {
        try {
            logger.info("Closing table: {}", id);
            
            tableManagementService.closeTable(id);
            
            logger.info("Table closed successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Table not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error closing table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Transfers orders from one table to another.
     * 
     * POST /api/tables/{id}/transfer
     * 
     * Requirements: 3.5
     */
    @PostMapping("/{id}/transfer")
    public ResponseEntity<Void> transferTable(
            @PathVariable UUID id,
            @Valid @RequestBody TransferTableRequest request) {
        try {
            logger.info("Transferring table {} to {}", id, request.toTableId());
            
            tableManagementService.transferTable(id, request.toTableId());
            
            logger.info("Table transfer completed successfully from {} to {}", id, request.toTableId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Table transfer failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot transfer table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error transferring table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Blocks a table by adding it to the blacklist.
     * 
     * POST /api/tables/{id}/block
     * 
     * Requirements: 3.6
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> blockTable(
            @PathVariable UUID id,
            @Valid @RequestBody BlockTableRequest request) {
        try {
            logger.info("Blocking table: {}", id);
            
            tableManagementService.blockTable(id, request.reason());
            
            logger.info("Table blocked successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Table not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error blocking table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Unblocks a table by removing it from the blacklist.
     * 
     * DELETE /api/tables/{id}/block
     * 
     * Requirements: 3.6
     */
    @DeleteMapping("/{id}/block")
    public ResponseEntity<Void> unblockTable(@PathVariable UUID id) {
        try {
            logger.info("Unblocking table: {}", id);
            
            tableManagementService.unblockTable(id);
            
            logger.info("Table unblocked successfully: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Table not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error unblocking table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    private TableResponse toTableResponse(DiningTable table) {
        return new TableResponse(
                table.getId(),
                table.getTenantId(),
                table.getSiteId(),
                table.getTableNumber(),
                table.getArea(),
                table.getStatus(),
                table.getCapacity(),
                table.getVersion()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for transferring a table.
     */
    public record TransferTableRequest(
            @NotNull UUID toTableId
    ) {}
    
    /**
     * Request DTO for blocking a table.
     */
    public record BlockTableRequest(
            @NotBlank String reason
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for table details.
     */
    public record TableResponse(
            UUID id,
            UUID tenantId,
            UUID siteId,
            String tableNumber,
            String area,
            TableStatus status,
            Integer capacity,
            Integer version
    ) {}
}
