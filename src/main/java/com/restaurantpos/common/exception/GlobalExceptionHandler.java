package com.restaurantpos.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.restaurantpos.identityaccess.exception.AuthenticationException;
import com.restaurantpos.identityaccess.exception.AuthorizationException;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler that returns RFC 7807 Problem Details format for all errors.
 * 
 * Handles:
 * - Validation errors (400)
 * - Authorization errors (403)
 * - Not found errors (404)
 * - Conflicts (409)
 * - Business rule violations (422)
 * - Server errors (500)
 * 
 * All error responses include a traceId for correlation.
 * 
 * Requirements: 13.1
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from @Valid annotations.
     * Returns 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        // Collect field errors
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problemDetail.setProperty("fieldErrors", fieldErrors);
        
        logger.warn("Validation error [traceId={}]: {}", traceId, fieldErrors);
        
        return problemDetail;
    }
    
    /**
     * Handle constraint violation exceptions from Bean Validation.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint violation"
        );
        
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        // Collect constraint violations
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (v1, v2) -> v1 // Keep first value in case of duplicates
                ));
        problemDetail.setProperty("violations", violations);
        
        logger.warn("Constraint violation [traceId={}]: {}", traceId, violations);
        
        return problemDetail;
    }
    
    /**
     * Handle authentication failures.
     * Returns 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/authentication-failed"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Authentication failed [traceId={}]: {}", traceId, ex.getMessage());
        
        return problemDetail;
    }
    
    /**
     * Handle authorization failures (insufficient permissions).
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(AuthorizationException.class)
    public ProblemDetail handleAuthorizationException(
            AuthorizationException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Authorization failed [traceId={}]: {}", traceId, ex.getMessage());
        
        return problemDetail;
    }
    
    /**
     * Handle resource not found errors.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/resource-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Resource not found [traceId={}]: {}", traceId, ex.getMessage());
        
        return problemDetail;
    }
    
    /**
     * Handle conflict errors (duplicate keys, optimistic locking conflicts).
     * Returns 409 Conflict.
     */
    @ExceptionHandler({ConflictException.class, OptimisticLockException.class})
    public ProblemDetail handleConflictException(
            Exception ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        String message = ex instanceof OptimisticLockException
                ? "The resource was modified by another user. Please refresh and try again."
                : ex.getMessage();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                message
        );
        
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/conflict"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Conflict [traceId={}]: {}", traceId, message);
        
        return problemDetail;
    }
    
    /**
     * Handle data integrity violations (unique constraint violations, foreign key violations).
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        String message = "Data integrity violation. The operation conflicts with existing data.";
        
        // Try to provide more specific message for common cases
        String rootCauseMessage = ex.getMostSpecificCause().getMessage();
        if (rootCauseMessage != null) {
            if (rootCauseMessage.contains("duplicate key")) {
                message = "A record with this identifier already exists.";
            } else if (rootCauseMessage.contains("foreign key")) {
                message = "The operation references a non-existent resource.";
            }
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                message
        );
        
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/data-integrity-violation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Data integrity violation [traceId={}]: {}", traceId, rootCauseMessage);
        
        return problemDetail;
    }
    
    /**
     * Handle business rule violations.
     * Returns 422 Unprocessable Entity.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolationException(
            BusinessRuleViolationException ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/business-rule-violation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.warn("Business rule violation [traceId={}]: {}", traceId, ex.getMessage());
        
        return problemDetail;
    }
    
    /**
     * Handle all other unexpected exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex,
            WebRequest request) {
        
        String traceId = getOrCreateTraceId();
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support with the trace ID."
        );
        
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://restaurantpos.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        
        logger.error("Unexpected error [traceId={}]", traceId, ex);
        
        return problemDetail;
    }
    
    /**
     * Get or create a trace ID for the current request.
     * First checks MDC, then generates a new UUID if not present.
     */
    private String getOrCreateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
}
