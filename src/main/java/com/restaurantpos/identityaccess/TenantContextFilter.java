package com.restaurantpos.identityaccess;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that extracts the tenant ID from the authentication context and sets it in the TenantContext.
 * This filter runs once per request and ensures the tenant context is properly cleaned up.
 * 
 * The tenant ID can be extracted from:
 * 1. JWT token claim "tenant_id" (when JWT authentication is configured)
 * 2. Custom authentication principal that implements TenantAware interface
 * 3. Request header "X-Tenant-ID" (for testing/development)
 * 
 * Requirements: 1.4, 1.7
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantContextFilter.class);
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            UUID tenantId = extractTenantId(request);
            
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                logger.debug("Set tenant context: {}", tenantId);
            } else {
                logger.debug("No tenant ID found in request");
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear the tenant context to prevent memory leaks
            TenantContext.clear();
            logger.debug("Cleared tenant context");
        }
    }
    
    /**
     * Extracts the tenant ID from various sources in order of precedence:
     * 1. JWT token claim
     * 2. TenantAware authentication principal
     * 3. Request header (for testing)
     */
    private UUID extractTenantId(HttpServletRequest request) {
        // Try to extract from authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            
            // Check if principal implements TenantAware
            if (principal instanceof TenantAware tenantAware) {
                UUID tenantId = tenantAware.getTenantId();
                if (tenantId != null) {
                    return tenantId;
                }
            }
            
            // Try to extract from JWT token (when JWT support is added)
            try {
                // Use reflection to avoid compile-time dependency on JWT classes
                Class<?> jwtClass = Class.forName("org.springframework.security.oauth2.jwt.Jwt");
                if (jwtClass.isInstance(principal)) {
                    Object tenantIdClaim = jwtClass.getMethod("getClaimAsString", String.class)
                            .invoke(principal, TENANT_ID_CLAIM);
                    if (tenantIdClaim instanceof String tenantIdStr) {
                        return UUID.fromString(tenantIdStr);
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | 
                     java.lang.reflect.InvocationTargetException | IllegalArgumentException e) {
                // JWT support not available or claim not found, continue to next method
                logger.trace("Could not extract tenant from JWT: {}", e.getMessage());
            }
        }
        
        // Fallback: Try to extract from request header (for testing/development)
        String tenantIdHeader = request.getHeader(TENANT_HEADER);
        if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
            try {
                return UUID.fromString(tenantIdHeader);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid tenant ID format in header: {}", tenantIdHeader);
            }
        }
        
        return null;
    }
}
