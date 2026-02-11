package com.restaurantpos.tenantprovisioning;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.tenantprovisioning.TenantProvisioningService.SiteDetails;
import com.restaurantpos.tenantprovisioning.TenantProvisioningService.TenantSettings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for tenant provisioning and management.
 * Provides endpoints for creating tenants, managing sites, and updating settings.
 * 
 * Requirements: 1.8
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantProvisioningController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantProvisioningController.class);
    
    private final TenantProvisioningService tenantProvisioningService;
    
    public TenantProvisioningController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }
    
    /**
     * Creates a new tenant.
     * Only accessible by ADMIN role.
     * 
     * POST /api/tenants
     * 
     * Requirements: 1.8
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        try {
            logger.info("Creating new tenant: {}", request.name());
            
            Tenant tenant = tenantProvisioningService.provisionTenant(
                    request.name(),
                    request.subscriptionPlan()
            );
            
            TenantResponse response = toTenantResponse(tenant);
            logger.info("Tenant created successfully: {}", tenant.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create tenant: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gets tenant details by ID.
     * 
     * GET /api/tenants/{id}
     * 
     * Requirements: 1.8
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        try {
            logger.debug("Fetching tenant: {}", id);
            
            Tenant tenant = tenantProvisioningService.getTenant(id);
            TenantResponse response = toTenantResponse(tenant);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Tenant not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching tenant: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Creates a new site for a tenant.
     * 
     * POST /api/tenants/{id}/sites
     * 
     * Requirements: 1.8
     */
    @PostMapping("/{id}/sites")
    public ResponseEntity<SiteResponse> createSite(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSiteRequest request) {
        try {
            logger.info("Creating site for tenant {}: {}", id, request.name());
            
            SiteDetails siteDetails = new SiteDetails(
                    request.name(),
                    request.address(),
                    request.timezone(),
                    request.settings()
            );
            
            Site site = tenantProvisioningService.createSite(id, siteDetails);
            
            SiteResponse response = toSiteResponse(site);
            logger.info("Site created successfully: {}", site.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create site: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Updates tenant settings.
     * 
     * PUT /api/tenants/{id}/settings
     * 
     * Requirements: 1.8
     */
    @PutMapping("/{id}/settings")
    public ResponseEntity<TenantResponse> updateTenantSettings(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantSettingsRequest request) {
        try {
            logger.info("Updating settings for tenant: {}", id);
            
            TenantSettings settings = new TenantSettings(
                    request.name(),
                    request.subscriptionPlan(),
                    request.status()
            );
            
            Tenant tenant = tenantProvisioningService.updateTenantSettings(id, settings);
            
            TenantResponse response = toTenantResponse(tenant);
            logger.info("Tenant settings updated successfully: {}", tenant.getId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update tenant settings: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Helper methods to convert entities to response DTOs
    
    private TenantResponse toTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubscriptionPlan(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
    
    private SiteResponse toSiteResponse(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getTenantId(),
                site.getName(),
                site.getAddress(),
                site.getTimezone(),
                site.getSettings(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for creating a tenant.
     */
    public record CreateTenantRequest(
            @NotBlank String name,
            String subscriptionPlan
    ) {}
    
    /**
     * Request DTO for creating a site.
     */
    public record CreateSiteRequest(
            @NotBlank String name,
            String address,
            String timezone,
            String settings
    ) {}
    
    /**
     * Request DTO for updating tenant settings.
     */
    public record UpdateTenantSettingsRequest(
            String name,
            String subscriptionPlan,
            TenantStatus status
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for tenant details.
     */
    public record TenantResponse(
            UUID id,
            String name,
            String subscriptionPlan,
            TenantStatus status,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}
    
    /**
     * Response DTO for site details.
     */
    public record SiteResponse(
            UUID id,
            UUID tenantId,
            String name,
            String address,
            String timezone,
            String settings,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}
}
