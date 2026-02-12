package com.restaurantpos.catalog.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.catalog.dto.FamilyDetails;
import com.restaurantpos.catalog.dto.ItemDetails;
import com.restaurantpos.catalog.dto.MenuStructure;
import com.restaurantpos.catalog.entity.Family;
import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.service.CatalogManagementService;
import com.restaurantpos.identityaccess.tenant.TenantContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for catalog management operations.
 * Provides endpoints for managing menu structure including families, subfamilies, and items.
 * 
 * Requirements: 4.1, 4.3
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);
    
    private final CatalogManagementService catalogManagementService;
    
    public CatalogController(CatalogManagementService catalogManagementService) {
        this.catalogManagementService = catalogManagementService;
    }
    
    /**
     * Gets the complete menu structure for the current tenant.
     * 
     * GET /api/catalog/menu?siteId={siteId}
     * 
     * Requirements: 4.1
     */
    @GetMapping("/menu")
    public ResponseEntity<MenuStructure> getMenuStructure(@RequestParam(required = false) UUID siteId) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.debug("Fetching menu structure for tenant: {}, site: {}", tenantId, siteId);
            
            MenuStructure menuStructure = catalogManagementService.getMenuStructure(tenantId, siteId);
            
            logger.debug("Retrieved menu structure with {} families", menuStructure.families().size());
            return ResponseEntity.ok(menuStructure);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to fetch menu structure: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error fetching menu structure: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Creates a new family for the current tenant.
     * 
     * POST /api/catalog/families
     * 
     * Requirements: 4.1
     */
    @PostMapping("/families")
    public ResponseEntity<FamilyResponse> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Creating family for tenant {}: {}", tenantId, request.name());
            
            FamilyDetails familyDetails = new FamilyDetails(request.name(), request.displayOrder());
            Family family = catalogManagementService.createFamily(tenantId, familyDetails);
            
            FamilyResponse response = toFamilyResponse(family);
            logger.info("Family created successfully: {}", family.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create family: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating family: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Creates a new item for the current tenant.
     * 
     * POST /api/catalog/items
     * 
     * Requirements: 4.1
     */
    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Creating item for tenant {}: {}", tenantId, request.name());
            
            ItemDetails itemDetails = new ItemDetails(
                request.subfamilyId(),
                request.name(),
                request.description(),
                request.basePrice(),
                request.imageUrl()
            );
            
            Item item = catalogManagementService.createItem(tenantId, itemDetails);
            
            ItemResponse response = toItemResponse(item);
            logger.info("Item created successfully: {}", item.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create item: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating item: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Updates an existing item.
     * 
     * PUT /api/catalog/items/{id}
     * 
     * Requirements: 4.1
     */
    @PutMapping("/items/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequest request) {
        try {
            logger.info("Updating item: {}", id);
            
            ItemDetails itemDetails = new ItemDetails(
                request.subfamilyId(),
                request.name(),
                request.description(),
                request.basePrice(),
                request.imageUrl()
            );
            
            Item item = catalogManagementService.updateItem(id, itemDetails);
            
            ItemResponse response = toItemResponse(item);
            logger.info("Item updated successfully: {}", item.getId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update item: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating item: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Toggles the availability status of an item.
     * 
     * PUT /api/catalog/items/{id}/availability
     * 
     * Requirements: 4.3
     */
    @PutMapping("/items/{id}/availability")
    public ResponseEntity<ItemResponse> updateItemAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        try {
            logger.info("Updating availability for item {}: {}", id, request.available());
            
            Item item = catalogManagementService.updateItemAvailability(id, request.available());
            
            ItemResponse response = toItemResponse(item);
            logger.info("Item availability updated successfully: {}", item.getId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update item availability: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating item availability: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    private FamilyResponse toFamilyResponse(Family family) {
        return new FamilyResponse(
            family.getId(),
            family.getTenantId(),
            family.getName(),
            family.getDisplayOrder(),
            family.getActive(),
            family.getCreatedAt(),
            family.getUpdatedAt()
        );
    }
    
    private ItemResponse toItemResponse(Item item) {
        return new ItemResponse(
            item.getId(),
            item.getTenantId(),
            item.getSubfamilyId(),
            item.getName(),
            item.getDescription(),
            item.getBasePrice(),
            item.getAvailable(),
            item.getImageUrl(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for creating a family.
     */
    public record CreateFamilyRequest(
        @NotBlank String name,
        Integer displayOrder
    ) {}
    
    /**
     * Request DTO for creating an item.
     */
    public record CreateItemRequest(
        @NotNull UUID subfamilyId,
        @NotBlank String name,
        String description,
        @NotNull BigDecimal basePrice,
        String imageUrl
    ) {}
    
    /**
     * Request DTO for updating an item.
     */
    public record UpdateItemRequest(
        @NotNull UUID subfamilyId,
        @NotBlank String name,
        String description,
        @NotNull BigDecimal basePrice,
        String imageUrl
    ) {}
    
    /**
     * Request DTO for updating item availability.
     */
    public record UpdateAvailabilityRequest(
        @NotNull Boolean available
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for family details.
     */
    public record FamilyResponse(
        UUID id,
        UUID tenantId,
        String name,
        Integer displayOrder,
        Boolean active,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {}
    
    /**
     * Response DTO for item details.
     */
    public record ItemResponse(
        UUID id,
        UUID tenantId,
        UUID subfamilyId,
        String name,
        String description,
        BigDecimal basePrice,
        Boolean available,
        String imageUrl,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {}
}
