package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.tenant.TenantContext;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TenantContext.
 * Tests the thread-local tenant ID storage and retrieval.
 */
class TenantContextTest {
    
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }
    
    @Test
    void setTenantId_shouldStoreTenantId() {
        UUID tenantId = UUID.randomUUID();
        
        TenantContext.setTenantId(tenantId);
        
        assertEquals(tenantId, TenantContext.getTenantId());
    }
    
    @Test
    void getTenantId_shouldReturnNullWhenNotSet() {
        assertNull(TenantContext.getTenantId());
    }
    
    @Test
    void clear_shouldRemoveTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        
        TenantContext.clear();
        
        assertNull(TenantContext.getTenantId());
    }
    
    @Test
    void setTenantId_shouldOverwritePreviousValue() {
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        TenantContext.setTenantId(tenantId1);
        TenantContext.setTenantId(tenantId2);
        
        assertEquals(tenantId2, TenantContext.getTenantId());
    }
    
    @Test
    void tenantContext_shouldBeThreadLocal() throws InterruptedException {
        UUID mainThreadTenantId = UUID.randomUUID();
        UUID otherThreadTenantId = UUID.randomUUID();
        
        TenantContext.setTenantId(mainThreadTenantId);
        
        Thread otherThread = new Thread(() -> {
            TenantContext.setTenantId(otherThreadTenantId);
            assertEquals(otherThreadTenantId, TenantContext.getTenantId());
        });
        
        otherThread.start();
        otherThread.join();
        
        // Main thread should still have its own tenant ID
        assertEquals(mainThreadTenantId, TenantContext.getTenantId());
    }
}
