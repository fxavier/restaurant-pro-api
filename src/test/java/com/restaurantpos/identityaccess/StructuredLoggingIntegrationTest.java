package com.restaurantpos.identityaccess;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import com.restaurantpos.identityaccess.tenant.TenantContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Integration tests for structured logging with MDC fields.
 * 
 * Verifies that logs include tenant_id, user_id, and trace_id from MDC.
 * 
 * Requirements: 14.1, 14.2, 14.6
 */
class StructuredLoggingIntegrationTest {
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    
    @BeforeEach
    void setUp() {
        // Set up a list appender to capture log events
        logger = (Logger) LoggerFactory.getLogger("com.restaurantpos.test");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        // Clear contexts
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        MDC.clear();
        
        if (listAppender != null) {
            listAppender.stop();
        }
    }
    
    @Test
    void shouldIncludeTenantIdInLogMdc() {
        // Given
        UUID tenantId = UUID.randomUUID();
        MDC.put("tenant_id", tenantId.toString());
        
        // When
        logger.info("Test log message");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(tenantId.toString(), event.getMDCPropertyMap().get("tenant_id"));
    }
    
    @Test
    void shouldIncludeUserIdInLogMdc() {
        // Given
        UUID userId = UUID.randomUUID();
        MDC.put("user_id", userId.toString());
        
        // When
        logger.info("Test log message");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(userId.toString(), event.getMDCPropertyMap().get("user_id"));
    }
    
    @Test
    void shouldIncludeTraceIdInLogMdc() {
        // Given
        String traceId = "trace-123-abc";
        MDC.put("trace_id", traceId);
        
        // When
        logger.info("Test log message");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(traceId, event.getMDCPropertyMap().get("trace_id"));
    }
    
    @Test
    void shouldIncludeAllMdcFieldsInLog() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String traceId = "correlation-xyz";
        
        MDC.put("tenant_id", tenantId.toString());
        MDC.put("user_id", userId.toString());
        MDC.put("trace_id", traceId);
        
        // When
        logger.info("Processing order");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        
        assertEquals(tenantId.toString(), mdcMap.get("tenant_id"));
        assertEquals(userId.toString(), mdcMap.get("user_id"));
        assertEquals(traceId, mdcMap.get("trace_id"));
        assertEquals("Processing order", event.getFormattedMessage());
    }
    
    @Test
    void shouldHandleLogWithoutMdcFields() {
        // Given - no MDC fields set
        
        // When
        logger.info("Log without context");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        
        assertNull(mdcMap.get("tenant_id"));
        assertNull(mdcMap.get("user_id"));
        assertNull(mdcMap.get("trace_id"));
    }
    
    @Test
    void shouldMaintainMdcAcrossMultipleLogs() {
        // Given
        UUID tenantId = UUID.randomUUID();
        String traceId = "trace-456";
        
        MDC.put("tenant_id", tenantId.toString());
        MDC.put("trace_id", traceId);
        
        // When
        logger.info("First log");
        logger.info("Second log");
        logger.info("Third log");
        
        // Then
        assertEquals(3, listAppender.list.size());
        
        for (ILoggingEvent event : listAppender.list) {
            Map<String, String> mdcMap = event.getMDCPropertyMap();
            assertEquals(tenantId.toString(), mdcMap.get("tenant_id"));
            assertEquals(traceId, mdcMap.get("trace_id"));
        }
    }
    
    @Test
    void shouldSupportMdcUpdate() {
        // Given
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        MDC.put("tenant_id", tenantId1.toString());
        logger.info("First tenant log");
        
        // When - update MDC
        MDC.put("tenant_id", tenantId2.toString());
        logger.info("Second tenant log");
        
        // Then
        assertEquals(2, listAppender.list.size());
        
        ILoggingEvent event1 = listAppender.list.get(0);
        assertEquals(tenantId1.toString(), event1.getMDCPropertyMap().get("tenant_id"));
        
        ILoggingEvent event2 = listAppender.list.get(1);
        assertEquals(tenantId2.toString(), event2.getMDCPropertyMap().get("tenant_id"));
    }
    
    @Test
    void shouldIncludeStandardLogFields() {
        // Given
        MDC.put("tenant_id", UUID.randomUUID().toString());
        
        // When
        logger.info("Test message");
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        
        assertNotNull(event.getTimeStamp());
        assertNotNull(event.getLevel());
        assertNotNull(event.getLoggerName());
        assertNotNull(event.getThreadName());
        assertEquals("Test message", event.getFormattedMessage());
    }
}
