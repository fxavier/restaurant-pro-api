package com.restaurantpos.tenantprovisioning;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.tenantprovisioning.TenantProvisioningService.SiteDetails;
import com.restaurantpos.tenantprovisioning.TenantProvisioningService.TenantSettings;

/**
 * Unit tests for TenantProvisioningController.
 * Tests REST endpoints for tenant management operations.
 */
@WebMvcTest(controllers = TenantProvisioningController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    })
class TenantProvisioningControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TenantProvisioningService tenantProvisioningService;
    
    @Test
    void createTenant_withValidRequest_returnsCreated() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        String tenantName = "Test Restaurant";
        String subscriptionPlan = "PREMIUM";
        
        Tenant tenant = new Tenant(tenantName, subscriptionPlan);
        // Use reflection to set the ID since it's normally set by JPA
        java.lang.reflect.Field idField = Tenant.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(tenant, tenantId);
        
        when(tenantProvisioningService.provisionTenant(tenantName, subscriptionPlan))
            .thenReturn(tenant);
        
        var request = new TenantProvisioningController.CreateTenantRequest(
            tenantName,
            subscriptionPlan
        );
        
        // When & Then
        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(tenantId.toString()))
            .andExpect(jsonPath("$.name").value(tenantName))
            .andExpect(jsonPath("$.subscriptionPlan").value(subscriptionPlan))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    
    @Test
    void createTenant_withInvalidRequest_returnsBadRequest() throws Exception {
        // Given
        when(tenantProvisioningService.provisionTenant(any(), any()))
            .thenThrow(new IllegalArgumentException("Tenant name already exists"));
        
        var request = new TenantProvisioningController.CreateTenantRequest(
            "Existing Restaurant",
            "PREMIUM"
        );
        
        // When & Then
        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void getTenant_withValidId_returnsOk() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        String tenantName = "Test Restaurant";
        
        Tenant tenant = new Tenant(tenantName, "PREMIUM");
        java.lang.reflect.Field idField = Tenant.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(tenant, tenantId);
        
        when(tenantProvisioningService.getTenant(tenantId))
            .thenReturn(tenant);
        
        // When & Then
        mockMvc.perform(get("/api/tenants/{id}", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tenantId.toString()))
            .andExpect(jsonPath("$.name").value(tenantName));
    }
    
    @Test
    void getTenant_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        
        when(tenantProvisioningService.getTenant(tenantId))
            .thenThrow(new IllegalArgumentException("Tenant not found"));
        
        // When & Then
        mockMvc.perform(get("/api/tenants/{id}", tenantId))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void createSite_withValidRequest_returnsCreated() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        String siteName = "Downtown Location";
        String address = "123 Main St";
        String timezone = "America/New_York";
        
        Site site = new Site(tenantId, siteName, address, timezone);
        java.lang.reflect.Field idField = Site.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(site, siteId);
        
        when(tenantProvisioningService.createSite(eq(tenantId), any(SiteDetails.class)))
            .thenReturn(site);
        
        var request = new TenantProvisioningController.CreateSiteRequest(
            siteName,
            address,
            timezone,
            null
        );
        
        // When & Then
        mockMvc.perform(post("/api/tenants/{id}/sites", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(siteId.toString()))
            .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
            .andExpect(jsonPath("$.name").value(siteName))
            .andExpect(jsonPath("$.address").value(address))
            .andExpect(jsonPath("$.timezone").value(timezone));
    }
    
    @Test
    void createSite_withInvalidTenantId_returnsBadRequest() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        
        when(tenantProvisioningService.createSite(eq(tenantId), any(SiteDetails.class)))
            .thenThrow(new IllegalArgumentException("Tenant not found"));
        
        var request = new TenantProvisioningController.CreateSiteRequest(
            "Downtown Location",
            "123 Main St",
            "America/New_York",
            null
        );
        
        // When & Then
        mockMvc.perform(post("/api/tenants/{id}/sites", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void updateTenantSettings_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        String newName = "Updated Restaurant";
        String newPlan = "ENTERPRISE";
        
        Tenant tenant = new Tenant(newName, newPlan);
        java.lang.reflect.Field idField = Tenant.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(tenant, tenantId);
        
        when(tenantProvisioningService.updateTenantSettings(eq(tenantId), any(TenantSettings.class)))
            .thenReturn(tenant);
        
        var request = new TenantProvisioningController.UpdateTenantSettingsRequest(
            newName,
            newPlan,
            TenantStatus.ACTIVE
        );
        
        // When & Then
        mockMvc.perform(put("/api/tenants/{id}/settings", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tenantId.toString()))
            .andExpect(jsonPath("$.name").value(newName))
            .andExpect(jsonPath("$.subscriptionPlan").value(newPlan));
    }
    
    @Test
    void updateTenantSettings_withNonExistentTenant_returnsBadRequest() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        
        when(tenantProvisioningService.updateTenantSettings(eq(tenantId), any(TenantSettings.class)))
            .thenThrow(new IllegalArgumentException("Tenant not found"));
        
        var request = new TenantProvisioningController.UpdateTenantSettingsRequest(
            "Updated Restaurant",
            "ENTERPRISE",
            TenantStatus.ACTIVE
        );
        
        // When & Then
        mockMvc.perform(put("/api/tenants/{id}/settings", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
