package com.restaurantpos.identityaccess.logging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to log all API requests with method, path, status code, duration, and tenant_id.
 * 
 * This filter wraps the request and response to capture timing and status information,
 * then logs the details after the request completes.
 * 
 * Requirements: 14.2
 */
@Component
@Order(2) // Execute after LoggingFilter (which populates MDC)
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Skip logging for actuator endpoints to reduce noise
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Record start time
        long startTime = System.currentTimeMillis();
        
        // Wrap request and response for content caching if needed
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            // Continue with the filter chain
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;
            
            // Log the request details
            logRequest(wrappedRequest, wrappedResponse, duration);
            
            // Copy response body back to the original response
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    /**
     * Logs API request details including method, path, status code, duration, and tenant_id.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param duration request processing duration in milliseconds
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int statusCode = response.getStatus();
        String tenantId = MDC.get("tenant_id"); // Get tenant_id from MDC (populated by LoggingFilter)
        
        // Log with structured format
        logger.info("API Request: method={}, path={}, status_code={}, duration_ms={}, tenant_id={}",
                method, path, statusCode, duration, tenantId != null ? tenantId : "N/A");
    }
}
