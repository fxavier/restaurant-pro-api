package com.restaurantpos.identityaccess;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.identityaccess.controller.AuthenticationController;
import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.exception.AuthenticationException;
import com.restaurantpos.identityaccess.service.AuthenticationService;

/**
 * Controller test for registration endpoint.
 */
@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerRegistrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AuthenticationService authenticationService;
    
    @Test
    void register_withValidRequest_returnsCreated() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String username = "newuser";
        String password = "SecurePassword123!";
        String email = "newuser@example.com";
        String role = "WAITER";
        
        AuthResponse mockResponse = new AuthResponse(
                "access-token",
                "refresh-token",
                userId,
                username,
                tenantId,
                role
        );
        
        when(authenticationService.register(any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);
        
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "username": "%s",
                    "password": "%s",
                    "email": "%s",
                    "role": "%s"
                }
                """, tenantId, username, password, email, role);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value(role));
    }
    
    @Test
    void register_withMissingUsername_returnsBadRequest() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "password": "SecurePassword123!",
                    "email": "test@example.com",
                    "role": "WAITER"
                }
                """, tenantId);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_withShortPassword_returnsBadRequest() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "username": "testuser",
                    "password": "short",
                    "email": "test@example.com",
                    "role": "WAITER"
                }
                """, tenantId);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_withDuplicateUsername_returnsBadRequest() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        
        when(authenticationService.register(any(), any(), any(), any(), any()))
                .thenThrow(new AuthenticationException("Username already exists"));
        
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "username": "existinguser",
                    "password": "SecurePassword123!",
                    "email": "test@example.com",
                    "role": "WAITER"
                }
                """, tenantId);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_withoutEmail_returnsCreated() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String username = "noemailuser";
        
        AuthResponse mockResponse = new AuthResponse(
                "access-token",
                "refresh-token",
                userId,
                username,
                tenantId,
                "WAITER"
        );
        
        when(authenticationService.register(any(), any(), any(), isNull(), any()))
                .thenReturn(mockResponse);
        
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "username": "%s",
                    "password": "SecurePassword123!",
                    "role": "WAITER"
                }
                """, tenantId, username);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
    }
    
    @Test
    void register_withInvalidRole_returnsBadRequest() throws Exception {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        
        when(authenticationService.register(any(), any(), any(), any(), any()))
                .thenThrow(new AuthenticationException("Invalid role specified"));
        
        String requestBody = String.format("""
                {
                    "tenantId": "%s",
                    "username": "testuser",
                    "password": "SecurePassword123!",
                    "email": "test@example.com",
                    "role": "INVALID_ROLE"
                }
                """, tenantId);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
