package com.restaurantpos.identityaccess;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom access denied handler that returns RFC 7807 Problem Details format
 * when authorization fails.
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        logger.warn("Access denied: {}", accessDeniedException.getMessage());
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        
        ProblemDetail problem = new ProblemDetail(
                "https://restaurant-pos-saas.com/problems/forbidden",
                "Forbidden",
                HttpServletResponse.SC_FORBIDDEN,
                "You do not have permission to access this resource",
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
