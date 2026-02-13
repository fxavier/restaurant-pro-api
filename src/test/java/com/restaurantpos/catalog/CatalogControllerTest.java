package com.restaurantpos.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import com.restaurantpos.catalog.controller.CatalogController;
import com.restaurantpos.catalog.dto.FamilyDetails;
import com.restaurantpos.catalog.dto.ItemDetails;
import com.restaurantpos.catalog.dto.MenuStructure;
import com.restaurantpos.catalog.entity.Family;
import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.service.CatalogManagementService;
import com.restaurantpos.identityaccess.tenant.TenantContext;

/**
 * Unit tests for CatalogController.
 * Tests REST endpoints for catalog management operations.
 */
@WebMvcTest(controllers = CatalogController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    })
class CatalogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CatalogManagementService catalogManagementService;
    
    private UUID tenantId;
    private UUID siteId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }
    
    @Test
    void getMenuStructure_withValidTenant_returnsOk() throws Exception {
        // Given
        MenuStructure.ItemNode item = new MenuStructure.ItemNode(
            UUID.randomUUID(),
            "Coffee",
            "Hot coffee",
            new BigDecimal("2.50"),
            true,
            null
        );
        
        MenuStructure.SubfamilyNode subfamily = new MenuStructure.SubfamilyNode(
            UUID.randomUUID(),
            "Hot Drinks",
            1,
            true,
            List.of(item)
        );
        
        MenuStructure.FamilyNode family = new MenuStructure.FamilyNode(
            UUID.randomUUID(),
            "Beverages",
            1,
            true,
            List.of(subfamily)
        );
        
        MenuStructure menuStructure = new MenuStructure(List.of(family));
        
        when(catalogManagementService.getMenuStructure(tenantId, siteId))
            .thenReturn(menuStructure);
        
        // When & Then
        mockMvc.perform(get("/api/catalog/menu")
                .param("siteId", siteId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.families.length()").value(1))
            .andExpect(jsonPath("$.families[0].name").value("Beverages"))
            .andExpect(jsonPath("$.families[0].subfamilies[0].name").value("Hot Drinks"))
            .andExpect(jsonPath("$.families[0].subfamilies[0].items[0].name").value("Coffee"))
            .andExpect(jsonPath("$.families[0].subfamilies[0].items[0].basePrice").value(2.50));
    }
    
    @Test
    void getMenuStructure_withoutSiteId_returnsOk() throws Exception {
        // Given
        MenuStructure menuStructure = new MenuStructure(List.of());
        
        when(catalogManagementService.getMenuStructure(eq(tenantId), any()))
            .thenReturn(menuStructure);
        
        // When & Then
        mockMvc.perform(get("/api/catalog/menu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.families.length()").value(0));
    }
    
    @Test
    void getMenuStructure_withoutTenantContext_returnsUnauthorized() throws Exception {
        // Given
        TenantContext.clear();
        
        // When & Then
        mockMvc.perform(get("/api/catalog/menu"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void createFamily_withValidRequest_returnsCreated() throws Exception {
        // Given
        String familyName = "Beverages";
        Integer displayOrder = 1;
        
        Family family = new Family(tenantId, familyName, displayOrder);
        
        when(catalogManagementService.createFamily(eq(tenantId), any(FamilyDetails.class)))
            .thenReturn(family);
        
        String requestBody = """
            {
                "name": "%s",
                "displayOrder": %d
            }
            """.formatted(familyName, displayOrder);
        
        // When & Then
        mockMvc.perform(post("/api/catalog/families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(familyName))
            .andExpect(jsonPath("$.displayOrder").value(displayOrder))
            .andExpect(jsonPath("$.active").value(true));
        
        verify(catalogManagementService).createFamily(eq(tenantId), any(FamilyDetails.class));
    }
    
    @Test
    void createFamily_withoutTenantContext_returnsUnauthorized() throws Exception {
        // Given
        TenantContext.clear();
        
        String requestBody = """
            {
                "name": "Beverages",
                "displayOrder": 1
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/catalog/families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void createFamily_withDuplicateName_returnsBadRequest() throws Exception {
        // Given
        when(catalogManagementService.createFamily(eq(tenantId), any(FamilyDetails.class)))
            .thenThrow(new IllegalArgumentException("Family with name 'Beverages' already exists"));
        
        String requestBody = """
            {
                "name": "Beverages",
                "displayOrder": 1
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/catalog/families")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void createItem_withValidRequest_returnsCreated() throws Exception {
        // Given
        UUID subfamilyId = UUID.randomUUID();
        String itemName = "Coffee";
        String description = "Hot coffee";
        BigDecimal basePrice = new BigDecimal("2.50");
        
        Item item = new Item(tenantId, subfamilyId, itemName, description, basePrice);
        
        when(catalogManagementService.createItem(eq(tenantId), any(ItemDetails.class)))
            .thenReturn(item);
        
        String requestBody = """
            {
                "subfamilyId": "%s",
                "name": "%s",
                "description": "%s",
                "basePrice": %s
            }
            """.formatted(subfamilyId, itemName, description, basePrice);
        
        // When & Then
        mockMvc.perform(post("/api/catalog/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(itemName))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.basePrice").value(2.50))
            .andExpect(jsonPath("$.available").value(true));
        
        verify(catalogManagementService).createItem(eq(tenantId), any(ItemDetails.class));
    }
    
    @Test
    void createItem_withoutTenantContext_returnsUnauthorized() throws Exception {
        // Given
        TenantContext.clear();
        
        String requestBody = """
            {
                "subfamilyId": "%s",
                "name": "Coffee",
                "description": "Hot coffee",
                "basePrice": 2.50
            }
            """.formatted(UUID.randomUUID());
        
        // When & Then
        mockMvc.perform(post("/api/catalog/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void createItem_withInvalidSubfamily_returnsBadRequest() throws Exception {
        // Given
        UUID subfamilyId = UUID.randomUUID();
        
        when(catalogManagementService.createItem(eq(tenantId), any(ItemDetails.class)))
            .thenThrow(new IllegalArgumentException("Subfamily not found or does not belong to tenant"));
        
        String requestBody = """
            {
                "subfamilyId": "%s",
                "name": "Coffee",
                "description": "Hot coffee",
                "basePrice": 2.50
            }
            """.formatted(subfamilyId);
        
        // When & Then
        mockMvc.perform(post("/api/catalog/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void updateItem_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID itemId = UUID.randomUUID();
        UUID subfamilyId = UUID.randomUUID();
        String itemName = "Espresso";
        String description = "Strong coffee";
        BigDecimal basePrice = new BigDecimal("3.00");
        
        Item item = new Item(tenantId, subfamilyId, itemName, description, basePrice);
        
        when(catalogManagementService.updateItem(eq(itemId), any(ItemDetails.class)))
            .thenReturn(item);
        
        String requestBody = """
            {
                "subfamilyId": "%s",
                "name": "%s",
                "description": "%s",
                "basePrice": %s
            }
            """.formatted(subfamilyId, itemName, description, basePrice);
        
        // When & Then
        mockMvc.perform(put("/api/catalog/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(itemName))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.basePrice").value(3.00));
        
        verify(catalogManagementService).updateItem(eq(itemId), any(ItemDetails.class));
    }
    
    @Test
    void updateItem_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID itemId = UUID.randomUUID();
        UUID subfamilyId = UUID.randomUUID();
        
        when(catalogManagementService.updateItem(eq(itemId), any(ItemDetails.class)))
            .thenThrow(new IllegalArgumentException("Item not found"));
        
        String requestBody = """
            {
                "subfamilyId": "%s",
                "name": "Coffee",
                "description": "Hot coffee",
                "basePrice": 2.50
            }
            """.formatted(subfamilyId);
        
        // When & Then
        mockMvc.perform(put("/api/catalog/items/{id}", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void updateItemAvailability_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID itemId = UUID.randomUUID();
        UUID subfamilyId = UUID.randomUUID();
        
        Item item = new Item(tenantId, subfamilyId, "Coffee", "Hot coffee", new BigDecimal("2.50"));
        item.setAvailable(false);
        
        when(catalogManagementService.updateItemAvailability(itemId, false))
            .thenReturn(item);
        
        String requestBody = """
            {
                "available": false
            }
            """;
        
        // When & Then
        mockMvc.perform(put("/api/catalog/items/{id}/availability", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
        
        verify(catalogManagementService).updateItemAvailability(itemId, false);
    }
    
    @Test
    void updateItemAvailability_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID itemId = UUID.randomUUID();
        
        when(catalogManagementService.updateItemAvailability(itemId, false))
            .thenThrow(new IllegalArgumentException("Item not found"));
        
        String requestBody = """
            {
                "available": false
            }
            """;
        
        // When & Then
        mockMvc.perform(put("/api/catalog/items/{id}/availability", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
    }
}
