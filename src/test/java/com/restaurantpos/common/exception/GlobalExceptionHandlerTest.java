package com.restaurantpos.common.exception;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import com.restaurantpos.identityaccess.exception.AuthenticationException;
import com.restaurantpos.identityaccess.exception.AuthorizationException;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Tests for GlobalExceptionHandler.
 * 
 * Verifies that all exception types are properly handled and return
 * RFC 7807 Problem Details format with appropriate HTTP status codes.
 * 
 * Requirements: 13.1
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler handler;
    private WebRequest request;
    
    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
    }
    
    @Test
    void shouldHandleValidationException_Returns400() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("order", "amount", "must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        
        // When
        ProblemDetail problemDetail = handler.handleValidationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(problemDetail.getDetail()).contains("Validation failed");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
        assertThat(problemDetail.getProperties()).containsKey("fieldErrors");
        
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) problemDetail.getProperties().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("amount", "must be positive");
    }
    
    @Test
    void shouldHandleConstraintViolationException_Returns400() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("email");
        when(violation.getMessage()).thenReturn("must be a valid email");
        violations.add(violation);
        
        ConstraintViolationException ex = new ConstraintViolationException(violations);
        
        // When
        ProblemDetail problemDetail = handler.handleConstraintViolationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("violations");
    }
    
    @Test
    void shouldHandleAuthenticationException_Returns401() {
        // Given
        AuthenticationException ex = new AuthenticationException("Invalid credentials");
        
        // When
        ProblemDetail problemDetail = handler.handleAuthenticationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Authentication Failed");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid credentials");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleAuthorizationException_Returns403() {
        // Given
        AuthorizationException ex = new AuthorizationException("Insufficient permissions");
        
        // When
        ProblemDetail problemDetail = handler.handleAuthorizationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Access Denied");
        assertThat(problemDetail.getDetail()).isEqualTo("Insufficient permissions");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleResourceNotFoundException_Returns404() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Order", "123");
        
        // When
        ProblemDetail problemDetail = handler.handleResourceNotFoundException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).contains("Order").contains("123").contains("not found");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleConflictException_Returns409() {
        // Given
        ConflictException ex = new ConflictException("Duplicate entry");
        
        // When
        ProblemDetail problemDetail = handler.handleConflictException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo("Duplicate entry");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleOptimisticLockException_Returns409() {
        // Given
        OptimisticLockException ex = new OptimisticLockException();
        
        // When
        ProblemDetail problemDetail = handler.handleConflictException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).contains("modified by another user");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
    }
    
    @Test
    void shouldHandleDataIntegrityViolationException_Returns409() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");
        
        // When
        ProblemDetail problemDetail = handler.handleDataIntegrityViolationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Data Integrity Violation");
        assertThat(problemDetail.getDetail()).contains("Data integrity violation");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleBusinessRuleViolationException_Returns422() {
        // Given
        BusinessRuleViolationException ex = new BusinessRuleViolationException("Cannot void paid order");
        
        // When
        ProblemDetail problemDetail = handler.handleBusinessRuleViolationException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Business Rule Violation");
        assertThat(problemDetail.getDetail()).isEqualTo("Cannot void paid order");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldHandleGenericException_Returns500() {
        // Given
        Exception ex = new RuntimeException("Unexpected error");
        
        // When
        ProblemDetail problemDetail = handler.handleGenericException(ex, request);
        
        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problemDetail.getDetail()).contains("unexpected error occurred");
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }
    
    @Test
    void shouldIncludeTraceIdInAllResponses() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Test resource not found");
        
        // When
        ProblemDetail problemDetail = handler.handleResourceNotFoundException(ex, request);
        
        // Then
        assertThat(problemDetail.getProperties()).containsKey("traceId");
        String traceId = (String) problemDetail.getProperties().get("traceId");
        assertThat(traceId).isNotNull().isNotEmpty();
    }
}
