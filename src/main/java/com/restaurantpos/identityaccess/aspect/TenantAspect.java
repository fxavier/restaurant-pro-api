package com.restaurantpos.identityaccess.aspect;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.restaurantpos.identityaccess.exception.TenantContextException;
import com.restaurantpos.identityaccess.tenant.TenantContext;

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
     * EXCEPTIONS: The following repositories are allowed without tenant context
     * because they are used for bootstrap operations:
     * - TenantRepository: tenant provisioning
     * - SiteRepository: site provisioning  
     * - All identityaccess repositories: user/auth management during tenant setup
     * 
     * @param joinPoint the join point representing the repository method call
     * @return the result of the repository method call
     * @throws Throwable if the method execution fails or tenant context is missing
     */
    @Around("execution(* com.restaurantpos..repository..*(..)) && " +
            "!execution(* com.restaurantpos.tenantprovisioning.repository..*(..)) && " +
            "!execution(* com.restaurantpos.identityaccess.repository..*(..))")
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
