package com.restaurantpos.diningroom;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.diningroom.controller.TableController;
import com.restaurantpos.diningroom.entity.DiningTable;
import com.restaurantpos.diningroom.service.TableManagementService;

/**
 * Unit tests for TableController.
 * Tests REST endpoints for table management operations.
 */
@WebMvcTest(controllers = TableController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    })
class TableControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TableManagementService tableManagementService;
    
    @Test
    void getTableMap_withValidSiteId_returnsOk() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        
        DiningTable table1 = new DiningTable(tenantId, siteId, "T1", "Main", 4);
        DiningTable table2 = new DiningTable(tenantId, siteId, "T2", "Main", 2);
        
        when(tableManagementService.getTableMap(siteId))
            .thenReturn(List.of(table1, table2));
        
        // When & Then
        mockMvc.perform(get("/api/tables")
                .param("siteId", siteId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].tableNumber").value("T1"))
            .andExpect(jsonPath("$[0].area").value("Main"))
            .andExpect(jsonPath("$[0].capacity").value(4))
            .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
            .andExpect(jsonPath("$[1].tableNumber").value("T2"));
    }
    
    @Test
    void getTableMap_withoutTenantContext_returnsUnauthorized() throws Exception {
        // Given
        UUID siteId = UUID.randomUUID();
        
        when(tableManagementService.getTableMap(siteId))
            .thenThrow(new IllegalStateException("Tenant context not set"));
        
        // When & Then
        mockMvc.perform(get("/api/tables")
                .param("siteId", siteId.toString()))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void openTable_withValidId_returnsOk() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/open", tableId))
            .andExpect(status().isOk());
        
        verify(tableManagementService).openTable(tableId);
    }
    
    @Test
    void openTable_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Table not found"))
            .when(tableManagementService).openTable(tableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/open", tableId))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void openTable_whenBlocked_returnsUnprocessableEntity() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        doThrow(new IllegalStateException("Cannot open blocked table"))
            .when(tableManagementService).openTable(tableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/open", tableId))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void closeTable_withValidId_returnsOk() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/close", tableId))
            .andExpect(status().isOk());
        
        verify(tableManagementService).closeTable(tableId);
    }
    
    @Test
    void closeTable_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Table not found"))
            .when(tableManagementService).closeTable(tableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/close", tableId))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void transferTable_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        var request = new TableController.TransferTableRequest(toTableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/transfer", fromTableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        verify(tableManagementService).transferTable(fromTableId, toTableId);
    }
    
    @Test
    void transferTable_withNonExistentTable_returnsNotFound() throws Exception {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Table not found"))
            .when(tableManagementService).transferTable(fromTableId, toTableId);
        
        var request = new TableController.TransferTableRequest(toTableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/transfer", fromTableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void transferTable_whenDestinationBlocked_returnsUnprocessableEntity() throws Exception {
        // Given
        UUID fromTableId = UUID.randomUUID();
        UUID toTableId = UUID.randomUUID();
        
        doThrow(new IllegalStateException("Cannot transfer to blocked table"))
            .when(tableManagementService).transferTable(fromTableId, toTableId);
        
        var request = new TableController.TransferTableRequest(toTableId);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/transfer", fromTableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    void blockTable_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        String reason = "Maintenance required";
        
        var request = new TableController.BlockTableRequest(reason);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/block", tableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        
        verify(tableManagementService).blockTable(tableId, reason);
    }
    
    @Test
    void blockTable_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        String reason = "Maintenance required";
        
        doThrow(new IllegalArgumentException("Table not found"))
            .when(tableManagementService).blockTable(tableId, reason);
        
        var request = new TableController.BlockTableRequest(reason);
        
        // When & Then
        mockMvc.perform(post("/api/tables/{id}/block", tableId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void unblockTable_withValidId_returnsOk() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(delete("/api/tables/{id}/block", tableId))
            .andExpect(status().isOk());
        
        verify(tableManagementService).unblockTable(tableId);
    }
    
    @Test
    void unblockTable_withNonExistentId_returnsNotFound() throws Exception {
        // Given
        UUID tableId = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException("Table not found"))
            .when(tableManagementService).unblockTable(tableId);
        
        // When & Then
        mockMvc.perform(delete("/api/tables/{id}/block", tableId))
            .andExpect(status().isNotFound());
    }
}
