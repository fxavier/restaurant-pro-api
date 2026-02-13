package com.restaurantpos.identityaccess.aspect;

import com.restaurantpos.identityaccess.exception.AuthorizationException;
import com.restaurantpos.identityaccess.model.Permission;
import com.restaurantpos.identityaccess.security.RequirePermission;
import com.restaurantpos.identityaccess.service.AuthorizationService;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces the @RequirePermission annotation.
 * Intercepts method calls annotated with @RequirePermission and verifies
 * that the current user has the required permission.
 * 
 * Requirements: 2.5
 */
@Aspect
@Component
public class RequirePermissionAspect {
    
    private final AuthorizationService authorizationService;
    
    public RequirePermissionAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    /**
     * Intercepts methods annotated with @RequirePermission and checks permissions.
     * 
     * @param joinPoint the join point representing the method call
     * @throws AuthorizationException if the user does not have the required permission
     */
    @Before("@annotation(com.restaurantpos.identityaccess.security.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // Get the method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        // Get the @RequirePermission annotation
        RequirePermission annotation = signature.getMethod().getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return;
        }
        
        Permission requiredPermission = annotation.value();
        
        // Get the current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthorizationException("No authenticated user found");
        }
        
        // Extract user ID from JWT
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        // Check permission
        authorizationService.requirePermission(userId, requiredPermission);
    }
    
    /**
     * Extracts the user ID from the Spring Security authentication object.
     * 
     * @param authentication the authentication object
     * @return the user ID
     * @throws AuthorizationException if the user ID cannot be extracted
     */
    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    throw new AuthorizationException("Invalid user ID in JWT: " + subject, e);
                }
            }
        }
        
        throw new AuthorizationException("Unable to extract user ID from authentication");
    }
}
