package com.restaurantpos.identityaccess;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.restaurantpos.identityaccess.logging.RequestLoggingFilter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

/**
 * Unit tests for RequestLoggingFilter.
 * 
 * Verifies that API requests are logged with method, path, status_code, duration_ms, and tenant_id.
 * 
 * Requirements: 14.2
 */
class RequestLoggingFilterTest {
    
    private RequestLoggingFilter filter;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    
    @Mock
    private FilterChain filterChain;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        filter = new RequestLoggingFilter();
        
        // Set up a list appender to capture log events
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        MDC.clear();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        MDC.clear();
        
        if (listAppender != null) {
            listAppender.stop();
        }
        
        if (mocks != null) {
            mocks.close();
        }
    }
    
    @Test
    void shouldLogApiRequestWithAllFields() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/orders");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        
        UUID tenantId = UUID.randomUUID();
        MDC.put("tenant_id", tenantId.toString());
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(), any());
        
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        assertTrue(message.contains("method=POST"));
        assertTrue(message.contains("path=/api/orders"));
        assertTrue(message.contains("status_code=201"));
        assertTrue(message.contains("duration_ms="));
        assertTrue(message.contains("tenant_id=" + tenantId));
    }
    
    @Test
    void shouldLogGetRequest() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/tables");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        UUID tenantId = UUID.randomUUID();
        MDC.put("tenant_id", tenantId.toString());
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        assertTrue(message.contains("method=GET"));
        assertTrue(message.contains("path=/api/tables"));
        assertTrue(message.contains("status_code=200"));
    }
    
    @Test
    void shouldLogErrorStatusCode() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("DELETE");
        request.setRequestURI("/api/orders/123");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        assertTrue(message.contains("status_code=404"));
    }
    
    @Test
    void shouldLogWithoutTenantId() throws ServletException, IOException {
        // Given - no tenant_id in MDC
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        assertTrue(message.contains("tenant_id=N/A"));
    }
    
    @Test
    void shouldMeasureDuration() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/catalog/menu");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        // Simulate processing delay using doAnswer
        org.mockito.Mockito.doAnswer(invocation -> {
            Thread.sleep(50); // 50ms delay
            return null;
        }).when(filterChain).doFilter(any(), any());
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        // Extract duration from message
        String durationPart = message.substring(message.indexOf("duration_ms=") + 12);
        String durationStr = durationPart.substring(0, durationPart.indexOf(","));
        long duration = Long.parseLong(durationStr);
        
        // Duration should be at least 50ms
        assertTrue(duration >= 50, "Duration should be at least 50ms, but was " + duration);
    }
    
    @Test
    void shouldNotLogActuatorEndpoints() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/actuator/health");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(any(), any());
        assertEquals(0, listAppender.list.size(), "Actuator endpoints should not be logged");
    }
    
    @Test
    void shouldLogDifferentHttpMethods() throws ServletException, IOException {
        // Test PUT
        MockHttpServletRequest putRequest = new MockHttpServletRequest();
        putRequest.setMethod("PUT");
        putRequest.setRequestURI("/api/catalog/items/123");
        MockHttpServletResponse putResponse = new MockHttpServletResponse();
        putResponse.setStatus(200);
        
        filter.doFilter(putRequest, putResponse, filterChain);
        
        // Test PATCH
        MockHttpServletRequest patchRequest = new MockHttpServletRequest();
        patchRequest.setMethod("PATCH");
        patchRequest.setRequestURI("/api/tables/456");
        MockHttpServletResponse patchResponse = new MockHttpServletResponse();
        patchResponse.setStatus(200);
        
        filter.doFilter(patchRequest, patchResponse, filterChain);
        
        // Then
        assertEquals(2, listAppender.list.size());
        
        String message1 = listAppender.list.get(0).getFormattedMessage();
        assertTrue(message1.contains("method=PUT"));
        
        String message2 = listAppender.list.get(1).getFormattedMessage();
        assertTrue(message2.contains("method=PATCH"));
    }
    
    @Test
    void shouldLogEvenWhenFilterChainThrowsException() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/orders");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        
        org.mockito.Mockito.doThrow(new ServletException("Simulated error"))
            .when(filterChain).doFilter(any(), any());
        
        // When/Then
        try {
            filter.doFilter(request, response, filterChain);
        } catch (ServletException e) {
            // Expected
        }
        
        // Log should still be created in finally block
        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        assertTrue(message.contains("method=POST"));
        assertTrue(message.contains("path=/api/orders"));
    }
}
