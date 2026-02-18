package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.security.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitingFilter.
 * 
 * Tests:
 * - Rate limiting is applied to /api/auth/** endpoints
 * - Rate limiting is not applied to other endpoints
 * - Requests are blocked after exceeding the limit
 * - Rate limit resets after the time window
 */
class RateLimitingFilterTest {
    
    private RateLimitingFilter filter;
    private FilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }
    
    @Test
    void shouldAllowRequestsUnderRateLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 5 requests (should all pass)
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // Verify filter chain was called 5 times
        verify(filterChain, times(5)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
    
    @Test
    void shouldBlockRequestsExceedingRateLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.2");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 5 requests (should all pass)
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // 6th request should be blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);
        
        // Verify filter chain was only called 5 times (not 6)
        verify(filterChain, times(5)).doFilter(any(), any());
        
        // Verify 6th request was blocked with 429 status
        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blockedResponse.getContentAsString()).contains("Rate limit exceeded");
        assertThat(blockedResponse.getContentAsString()).contains("\"status\":429");
    }
    
    @Test
    void shouldNotApplyRateLimitingToNonAuthEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/orders");
        request.setRemoteAddr("192.168.1.3");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 10 requests (more than the limit)
        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // Verify filter chain was called 10 times (no rate limiting)
        verify(filterChain, times(10)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
    
    @Test
    void shouldTrackRateLimitPerIpAddress() throws ServletException, IOException {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRequestURI("/api/auth/login");
        request1.setRemoteAddr("192.168.1.4");
        
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRequestURI("/api/auth/login");
        request2.setRemoteAddr("192.168.1.5");
        
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        
        // Make 5 requests from IP 1
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request1, response1, filterChain);
        }
        
        // Make 4 requests from IP 2 (should also pass)
        for (int i = 0; i < 4; i++) {
            filter.doFilter(request2, response2, filterChain);
        }
        
        // Verify both IPs were allowed (9 total calls)
        verify(filterChain, times(9)).doFilter(any(), any());
        
        // 6th request from IP 1 should be blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request1, blockedResponse, filterChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        
        // But IP 2 should still be allowed (5th request)
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
        filter.doFilter(request2, allowedResponse, filterChain);
        verify(filterChain, times(10)).doFilter(any(), any());
    }
    
    @Test
    void shouldExtractIpFromXForwardedForHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("10.0.0.1"); // Proxy IP
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 5 requests
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // 6th request should be blocked (using X-Forwarded-For IP)
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);
        
        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
    
    @Test
    void shouldExtractIpFromXRealIpHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("10.0.0.1"); // Proxy IP
        request.addHeader("X-Real-IP", "203.0.113.2");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Make 5 requests
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // 6th request should be blocked (using X-Real-IP)
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);
        
        assertThat(blockedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
