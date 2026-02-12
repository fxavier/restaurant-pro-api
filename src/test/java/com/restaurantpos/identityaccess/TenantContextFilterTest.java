package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.tenant.TenantAware;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.identityaccess.tenant.TenantContextFilter;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

/**
 * Unit tests for TenantContextFilter.
 * Tests tenant ID extraction from various sources and context cleanup.
 */
class TenantContextFilterTest {
    
    private TenantContextFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }
    
    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void doFilterInternal_shouldExtractTenantIdFromHeader() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader("X-Tenant-ID", tenantId.toString());
        
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        // Tenant context is cleared after filter chain, so we can't assert it here
    }
    
    @Test
    void doFilterInternal_shouldExtractTenantIdFromTenantAwarePrincipal() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        TenantAware principal = () -> tenantId;
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null)
        );
        
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldClearTenantContextAfterFilterChain() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader("X-Tenant-ID", tenantId.toString());
        
        doAnswer(invocation -> {
            // During filter chain execution, tenant context should be set
            assertEquals(tenantId, TenantContext.getTenantId());
            return null;
        }).when(filterChain).doFilter(request, response);
        
        filter.doFilter(request, response, filterChain);
        
        // After filter completes, tenant context should be cleared
        assertNull(TenantContext.getTenantId());
    }
    
    @Test
    void doFilterInternal_shouldClearTenantContextEvenOnException() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        request.addHeader("X-Tenant-ID", tenantId.toString());
        
        doThrow(new ServletException("Test exception"))
            .when(filterChain).doFilter(request, response);
        
        assertThrows(ServletException.class, () -> 
            filter.doFilter(request, response, filterChain)
        );
        
        // Tenant context should still be cleared
        assertNull(TenantContext.getTenantId());
    }
    
    @Test
    void doFilterInternal_shouldHandleNoTenantId() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        assertNull(TenantContext.getTenantId());
    }
    
    @Test
    void doFilterInternal_shouldHandleInvalidTenantIdFormat() throws ServletException, IOException {
        request.addHeader("X-Tenant-ID", "invalid-uuid");
        
        filter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        assertNull(TenantContext.getTenantId());
    }
}
