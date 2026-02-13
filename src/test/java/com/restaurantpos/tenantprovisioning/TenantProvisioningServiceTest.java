package com.restaurantpos.tenantprovisioning;

import com.restaurantpos.tenantprovisioning.entity.Site;
import com.restaurantpos.tenantprovisioning.entity.Tenant;
import com.restaurantpos.tenantprovisioning.model.TenantStatus;
import com.restaurantpos.tenantprovisioning.repository.TenantRepository;
import com.restaurantpos.tenantprovisioning.service.TenantProvisioningService;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests for TenantProvisioningService.
 * 
 * Requirements: 1.8
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantProvisioningServiceTest {
    
    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Test
    void provisionTenant_withValidData_createsTenant() {
        // Given
        String name = "Test Restaurant " + UUID.randomUUID();
        String plan = "PREMIUM";
        
        // When
        Tenant tenant = tenantProvisioningService.provisionTenant(name, plan);
        
        // Then
        assertThat(tenant).isNotNull();
        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getName()).isEqualTo(name);
        assertThat(tenant.getSubscriptionPlan()).isEqualTo(plan);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void provisionTenant_withNullName_throwsException() {
        assertThatThrownBy(() -> tenantProvisioningService.provisionTenant(null, "BASIC"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant name cannot be null or empty");
    }
    
    @Test
    void provisionTenant_withEmptyName_throwsException() {
        assertThatThrownBy(() -> tenantProvisioningService.provisionTenant("  ", "BASIC"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant name cannot be null or empty");
    }
    
    @Test
    void provisionTenant_withDuplicateName_throwsException() {
        // Given
        String name = "Duplicate Restaurant " + UUID.randomUUID();
        tenantProvisioningService.provisionTenant(name, "BASIC");
        
        // When/Then
        assertThatThrownBy(() -> tenantProvisioningService.provisionTenant(name, "PREMIUM"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    void createSite_withValidData_createsSite() {
        // Given
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        var siteDetails = new TenantProvisioningService.SiteDetails(
            "Main Location",
            "123 Main St, City",
            "Europe/Lisbon",
            "{\"currency\":\"EUR\"}"
        );
        
        // When
        Site site = tenantProvisioningService.createSite(tenant.getId(), siteDetails);
        
        // Then
        assertThat(site).isNotNull();
        assertThat(site.getId()).isNotNull();
        assertThat(site.getTenantId()).isEqualTo(tenant.getId());
        assertThat(site.getName()).isEqualTo("Main Location");
        assertThat(site.getAddress()).isEqualTo("123 Main St, City");
        assertThat(site.getTimezone()).isEqualTo("Europe/Lisbon");
        assertThat(site.getSettings()).isEqualTo("{\"currency\":\"EUR\"}");
        assertThat(site.getCreatedAt()).isNotNull();
        assertThat(site.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void createSite_withNullTenantId_throwsException() {
        var siteDetails = new TenantProvisioningService.SiteDetails(
            "Main Location",
            "123 Main St",
            "Europe/Lisbon",
            null
        );
        
        assertThatThrownBy(() -> tenantProvisioningService.createSite(null, siteDetails))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
    }
    
    @Test
    void createSite_withNonExistentTenant_throwsException() {
        UUID nonExistentTenantId = UUID.randomUUID();
        var siteDetails = new TenantProvisioningService.SiteDetails(
            "Main Location",
            "123 Main St",
            "Europe/Lisbon",
            null
        );
        
        assertThatThrownBy(() -> tenantProvisioningService.createSite(nonExistentTenantId, siteDetails))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant not found");
    }
    
    @Test
    void createSite_withInactiveTenant_throwsException() {
        // Given
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        
        var siteDetails = new TenantProvisioningService.SiteDetails(
            "Main Location",
            "123 Main St",
            "Europe/Lisbon",
            null
        );
        
        // When/Then
        assertThatThrownBy(() -> tenantProvisioningService.createSite(tenant.getId(), siteDetails))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot create site for inactive tenant");
    }
    
    @Test
    void createSite_withDuplicateName_throwsException() {
        // Given
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        var siteDetails = new TenantProvisioningService.SiteDetails(
            "Main Location",
            "123 Main St",
            "Europe/Lisbon",
            null
        );
        tenantProvisioningService.createSite(tenant.getId(), siteDetails);
        
        // When/Then
        assertThatThrownBy(() -> tenantProvisioningService.createSite(tenant.getId(), siteDetails))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists for this tenant");
    }
    
    @Test
    void createSite_withNullSiteName_throwsException() {
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        var siteDetails = new TenantProvisioningService.SiteDetails(
            null,
            "123 Main St",
            "Europe/Lisbon",
            null
        );
        
        assertThatThrownBy(() -> tenantProvisioningService.createSite(tenant.getId(), siteDetails))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Site name cannot be null or empty");
    }
    
    @Test
    void updateTenantSettings_withValidData_updatesSettings() {
        // Given
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        var settings = new TenantProvisioningService.TenantSettings(
            "Updated Restaurant Name",
            "PREMIUM",
            TenantStatus.ACTIVE
        );
        
        // When
        Tenant updated = tenantProvisioningService.updateTenantSettings(tenant.getId(), settings);
        
        // Then
        assertThat(updated.getId()).isEqualTo(tenant.getId());
        assertThat(updated.getName()).isEqualTo("Updated Restaurant Name");
        assertThat(updated.getSubscriptionPlan()).isEqualTo("PREMIUM");
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }
    
    @Test
    void updateTenantSettings_withNullTenantId_throwsException() {
        var settings = new TenantProvisioningService.TenantSettings(
            "Updated Name",
            "PREMIUM",
            TenantStatus.ACTIVE
        );
        
        assertThatThrownBy(() -> tenantProvisioningService.updateTenantSettings(null, settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
    }
    
    @Test
    void updateTenantSettings_withNonExistentTenant_throwsException() {
        UUID nonExistentTenantId = UUID.randomUUID();
        var settings = new TenantProvisioningService.TenantSettings(
            "Updated Name",
            "PREMIUM",
            TenantStatus.ACTIVE
        );
        
        assertThatThrownBy(() -> tenantProvisioningService.updateTenantSettings(nonExistentTenantId, settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant not found");
    }
    
    @Test
    void updateTenantSettings_withDuplicateName_throwsException() {
        // Given
        String existingName = "Existing Restaurant " + UUID.randomUUID();
        tenantProvisioningService.provisionTenant(existingName, "BASIC");
        
        Tenant tenant = tenantProvisioningService.provisionTenant("Another Restaurant " + UUID.randomUUID(), "BASIC");
        var settings = new TenantProvisioningService.TenantSettings(
            existingName,
            "PREMIUM",
            TenantStatus.ACTIVE
        );
        
        // When/Then
        assertThatThrownBy(() -> tenantProvisioningService.updateTenantSettings(tenant.getId(), settings))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    void updateTenantSettings_withPartialUpdate_updatesOnlyProvidedFields() {
        // Given
        Tenant tenant = tenantProvisioningService.provisionTenant("Restaurant " + UUID.randomUUID(), "BASIC");
        String originalName = tenant.getName();
        
        var settings = new TenantProvisioningService.TenantSettings(
            null,  // Don't update name
            "PREMIUM",  // Update plan
            null  // Don't update status
        );
        
        // When
        Tenant updated = tenantProvisioningService.updateTenantSettings(tenant.getId(), settings);
        
        // Then
        assertThat(updated.getName()).isEqualTo(originalName);  // Name unchanged
        assertThat(updated.getSubscriptionPlan()).isEqualTo("PREMIUM");  // Plan updated
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.ACTIVE);  // Status unchanged
    }
}
