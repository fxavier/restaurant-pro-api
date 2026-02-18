package com.restaurantpos.identityaccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for rate limiting on authentication endpoints.
 * 
 * Verifies that the RateLimitingFilter correctly blocks requests
 * after exceeding the rate limit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldBlockAuthenticationRequestsAfterRateLimit() throws Exception {
        String loginRequest = """
            {
                "username": "testuser",
                "password": "testpass"
            }
            """;
        
        // Make 5 requests (should all be processed, though they may fail authentication)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.100.1"))
                .andExpect(status().is(not(429))); // Not rate limited
        }
        
        // 6th request should be rate limited
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
                .header("X-Forwarded-For", "192.168.100.1"))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.title").value("Too Many Requests"))
            .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Maximum 5 authentication attempts per minute allowed."));
    }
    
    @Test
    void shouldTrackRateLimitPerIpAddress() throws Exception {
        String loginRequest = """
            {
                "username": "testuser",
                "password": "testpass"
            }
            """;
        
        // Make 5 requests from IP 1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.100.10"))
                .andExpect(status().is(not(429)));
        }
        
        // Make 4 requests from IP 2 (should also be allowed)
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.100.20"))
                .andExpect(status().is(not(429)));
        }
        
        // 6th request from IP 1 should be blocked
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
                .header("X-Forwarded-For", "192.168.100.10"))
            .andExpect(status().isTooManyRequests());
        
        // But IP 2 should still be allowed (5th request)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
                .header("X-Forwarded-For", "192.168.100.20"))
            .andExpect(status().is(not(429)));
    }
    
    @Test
    void shouldNotApplyRateLimitingToNonAuthEndpoints() throws Exception {
        // Make 10 requests to a non-auth endpoint (should all pass through filter)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .header("X-Forwarded-For", "192.168.100.30"))
                .andExpect(status().is(not(429))); // Not rate limited (will fail for other reasons like auth)
        }
    }
}
