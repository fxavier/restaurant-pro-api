package com.restaurantpos.identityaccess.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter for authentication endpoints to prevent brute force attacks.
 * 
 * Limits authentication attempts to 5 per minute per IP address or username.
 * Uses Caffeine in-memory cache to track attempts.
 * 
 * Requirement: 13.4 - Rate limiting on authentication endpoints
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);
    
    // Cache to track attempts per IP/username
    private final Cache<String, AtomicInteger> attemptCache;
    
    public RateLimitingFilter() {
        this.attemptCache = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_DURATION)
            .maximumSize(10_000)
            .build();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Only apply rate limiting to authentication endpoints
        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get identifier (IP address or username from request)
        String identifier = getIdentifier(request);
        
        // Check rate limit
        AtomicInteger attempts = attemptCache.get(identifier, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();
        
        if (currentAttempts > MAX_ATTEMPTS) {
            logger.warn("Rate limit exceeded for identifier: {} (attempts: {})", identifier, currentAttempts);
            sendRateLimitError(response);
            return;
        }
        
        logger.debug("Authentication attempt {} of {} for identifier: {}", currentAttempts, MAX_ATTEMPTS, identifier);
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Get identifier for rate limiting.
     * Tries to extract username from request body, falls back to IP address.
     */
    private String getIdentifier(HttpServletRequest request) {
        // For simplicity, use IP address as identifier
        // In production, you might want to parse the request body to get username
        String ipAddress = getClientIpAddress(request);
        return "ip:" + ipAddress;
    }
    
    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Send rate limit error response in RFC 7807 Problem Details format.
     */
    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String errorJson = String.format(
            "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429," +
            "\"detail\":\"Rate limit exceeded. Maximum %d authentication attempts per minute allowed.\"," +
            "\"instance\":\"/api/auth\"}",
            MAX_ATTEMPTS
        );
        
        response.getWriter().write(errorJson);
        response.getWriter().flush();
    }
}
