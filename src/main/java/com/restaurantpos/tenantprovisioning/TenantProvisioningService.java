package com.restaurantpos.tenantprovisioning;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for tenant provisioning operations.
 * Handles tenant onboarding, site creation, and configuration management.
 * 
 * Requirements: 1.8
 */
@Service
public class TenantProvisioningService {
    
    private final TenantRepository tenantRepository;
    private final SiteRepository siteRepository;
    
    public TenantProvisioningService(
            TenantRepository tenantRepository,
            SiteRepository siteRepository) {
        this.tenantRepository = tenantRepository;
        this.siteRepository = siteRepository;
    }
    
    /**
     * Provisions a new tenant with initial configuration.
     * Creates tenant record and default configuration in a single transaction.
     * 
     * @param name the tenant name
     * @param plan the subscription plan
     * @return the created tenant
     * @throws IllegalArgumentException if tenant name already exists
     * 
     * Requirements: 1.8
     * Property 4: Tenant Provisioning Atomicity
     */
    @Transactional
    public Tenant provisionTenant(String name, String plan) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        
        if (tenantRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tenant with name '" + name + "' already exists");
        }
        
        Tenant tenant = new Tenant(name, plan);
        return tenantRepository.save(tenant);
    }
    
    /**
     * Creates a new site for an existing tenant.
     * 
     * @param tenantId the tenant ID
     * @param siteDetails the site details
     * @return the created site
     * @throws IllegalArgumentException if tenant doesn't exist or site name already exists
     * 
     * Requirements: 1.8
     */
    @Transactional
    public Site createSite(UUID tenantId, SiteDetails siteDetails) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (siteDetails == null) {
            throw new IllegalArgumentException("Site details cannot be null");
        }
        
        if (siteDetails.name() == null || siteDetails.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Site name cannot be null or empty");
        }
        
        // Verify tenant exists
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found with ID: " + tenantId));
        
        if (!tenant.isActive()) {
            throw new IllegalArgumentException("Cannot create site for inactive tenant");
        }
        
        // Check for duplicate site name within tenant
        if (siteRepository.existsByTenantIdAndName(tenantId, siteDetails.name())) {
            throw new IllegalArgumentException(
                "Site with name '" + siteDetails.name() + "' already exists for this tenant");
        }
        
        Site site = new Site(
            tenantId,
            siteDetails.name(),
            siteDetails.address(),
            siteDetails.timezone()
        );
        
        if (siteDetails.settings() != null) {
            site.setSettings(siteDetails.settings());
        }
        
        return siteRepository.save(site);
    }
    
    /**
     * Gets tenant details by ID.
     * 
     * @param tenantId the tenant ID
     * @return the tenant
     * @throws IllegalArgumentException if tenant doesn't exist
     * 
     * Requirements: 1.8
     */
    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found with ID: " + tenantId));
    }
    
    /**
     * Updates tenant settings.
     * 
     * @param tenantId the tenant ID
     * @param settings the new settings
     * @return the updated tenant
     * @throws IllegalArgumentException if tenant doesn't exist
     * 
     * Requirements: 1.8
     */
    @Transactional
    public Tenant updateTenantSettings(UUID tenantId, TenantSettings settings) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found with ID: " + tenantId));
        
        if (settings.name() != null && !settings.name().trim().isEmpty()) {
            // Check if new name conflicts with existing tenant
            if (!settings.name().equals(tenant.getName()) && 
                tenantRepository.existsByName(settings.name())) {
                throw new IllegalArgumentException("Tenant with name '" + settings.name() + "' already exists");
            }
            tenant.setName(settings.name());
        }
        
        if (settings.subscriptionPlan() != null) {
            tenant.setSubscriptionPlan(settings.subscriptionPlan());
        }
        
        if (settings.status() != null) {
            tenant.setStatus(settings.status());
        }
        
        return tenantRepository.save(tenant);
    }
    
    /**
     * DTO for site creation details.
     */
    public record SiteDetails(
        String name,
        String address,
        String timezone,
        String settings
    ) {}
    
    /**
     * DTO for tenant settings update.
     */
    public record TenantSettings(
        String name,
        String subscriptionPlan,
        TenantStatus status
    ) {}
}
