package com.restaurantpos.identityaccess.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.restaurantpos.identityaccess.tenant.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to populate MDC (Mapped Diagnostic Context) with contextual information
 * for structured logging.
 * 
 * Populates MDC with:
 * - tenant_id: Current tenant from TenantContext
 * - user_id: Current authenticated user ID from JWT
 * - trace_id: Correlation ID for request tracing
 * 
 * Requirements: 14.1, 14.2, 14.6
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {
    
    private static final String MDC_TENANT_ID = "tenant_id";
    private static final String MDC_USER_ID = "user_id";
    private static final String MDC_TRACE_ID = "trace_id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Populate MDC with contextual information
            populateMDC(request);
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Clear MDC to prevent memory leaks and cross-request contamination
            clearMDC();
        }
    }
    
    /**
     * Populates MDC with tenant_id, user_id, and trace_id.
     */
    private void populateMDC(HttpServletRequest request) {
        // Add tenant_id from TenantContext
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            MDC.put(MDC_TENANT_ID, tenantId.toString());
        }
        
        // Add user_id from authentication context
        UUID userId = extractUserId();
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId.toString());
        }
        
        // Add trace_id (correlation ID) from header or generate new one
        String traceId = extractOrGenerateTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
    }
    
    /**
     * Extracts user ID from the current authentication context.
     * 
     * @return user ID if authenticated, null otherwise
     */
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        // Handle JWT authentication
        if (principal instanceof Jwt jwt) {
            String userIdClaim = jwt.getClaimAsString("user_id");
            if (userIdClaim != null) {
                try {
                    return UUID.fromString(userIdClaim);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID format, return null
                    return null;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extracts trace ID from request header or generates a new one.
     * 
     * @param request the HTTP request
     * @return trace ID for correlation
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        
        if (traceId == null || traceId.isBlank()) {
            // Generate new trace ID if not provided
            traceId = UUID.randomUUID().toString();
        }
        
        return traceId;
    }
    
    /**
     * Clears all MDC entries to prevent memory leaks.
     */
    private void clearMDC() {
        MDC.remove(MDC_TENANT_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_TRACE_ID);
    }
}
