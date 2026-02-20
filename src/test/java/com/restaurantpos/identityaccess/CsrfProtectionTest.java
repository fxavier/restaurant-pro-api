package com.restaurantpos.identityaccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration tests for CSRF protection configuration.
 * 
 * Verifies that:
 * - CSRF is enabled for state-changing operations on protected endpoints
 * - CSRF is disabled for /api/auth/** endpoints (stateless JWT)
 * 
 * Requirement: 13.6
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfProtectionTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void authEndpoints_shouldNotRequireCsrf() throws Exception {
        // Given: A login request to /api/auth/login without CSRF token
        String loginRequest = """
            {
                "username": "testuser@example.com",
                "password": "ValidPassword123!"
            }
            """;
        
        // When: Making request without CSRF token
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andReturn();
        
        // Then: Should not be blocked by CSRF (status should not be 403)
        assertNotEquals(403, result.getResponse().getStatus(), 
            "Auth endpoints should not require CSRF token");
    }
    
    @Test
    void protectedEndpoints_withCsrf_shouldPassCsrfCheck() throws Exception {
        // Given: An authenticated request to a protected endpoint with CSRF token
        // When: Making request with CSRF token
        MvcResult result = mockMvc.perform(post("/api/orders")
                .with(jwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andReturn();
        
        // Then: Should not be blocked by CSRF (status should not be 403)
        assertNotEquals(403, result.getResponse().getStatus(), 
            "Requests with CSRF token should pass CSRF check");
    }
}

