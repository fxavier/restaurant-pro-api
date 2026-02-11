package com.restaurantpos.identityaccess;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that enforces tenant isolation on repository operations.
 * This aspect intercepts repository method calls and ensures that the tenant context is set.
 * 
 * Requirements: 1.4, 1.7
 */
@Aspect
@Component
public class TenantAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantAspect.class);
    
    /**
     * Intercepts all repository method calls to enforce tenant context.
     * If no tenant context is set, throws an exception to prevent unauthorized data access.
     * 
     * @param joinPoint the join point representing the repository method call
     * @return the result of the repository method call
     * @throws Throwable if the method execution fails or tenant context is missing
     */
    @Around("execution(* com.restaurantpos..repository..*(..))")
    public Object enforceTenantContext(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID tenantId = TenantContext.getTenantId();
        
        // Check if tenant context is set
        if (tenantId == null) {
            String methodName = joinPoint.getSignature().toShortString();
            logger.error("Tenant context not set for repository operation: {}", methodName);
            throw new TenantContextException("Tenant context is required for repository operations");
        }
        
        logger.trace("Executing repository operation with tenant context: {}", tenantId);
        
        // Proceed with the repository operation
        return joinPoint.proceed();
    }
}
