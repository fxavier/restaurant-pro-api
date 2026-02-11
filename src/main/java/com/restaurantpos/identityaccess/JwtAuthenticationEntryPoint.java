package com.restaurantpos.identityaccess;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom authentication entry point that returns RFC 7807 Problem Details format
 * when authentication fails.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("Authentication failed: {}", authException.getMessage());
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        
        ProblemDetail problem = new ProblemDetail(
                "https://restaurant-pos-saas.com/problems/unauthorized",
                "Unauthorized",
                HttpServletResponse.SC_UNAUTHORIZED,
                "Authentication is required to access this resource",
                request.getRequestURI()
        );
        
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
    
    /**
     * RFC 7807 Problem Details representation.
     */
    private record ProblemDetail(
            String type,
            String title,
            int status,
            String detail,
            String instance
    ) {}
}
