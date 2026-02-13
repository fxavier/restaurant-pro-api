package com.restaurantpos.identityaccess;

import com.restaurantpos.identityaccess.aspect.TenantAspect;
import com.restaurantpos.identityaccess.exception.TenantContextException;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantAspect.
 * Tests tenant context enforcement on repository operations.
 */
class TenantAspectTest {
    
    private TenantAspect aspect;
    private ProceedingJoinPoint joinPoint;
    
    @BeforeEach
    void setUp() {
        aspect = new TenantAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("TestRepository.findAll()");
        when(joinPoint.getSignature()).thenReturn(signature);
    }
    
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }
    
    @Test
    void enforceTenantContext_shouldProceedWhenTenantContextIsSet() throws Throwable {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        Object result = aspect.enforceTenantContext(joinPoint);
        
        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
    }
    
    @Test
    void enforceTenantContext_shouldThrowExceptionWhenTenantContextNotSet() {
        TenantContextException exception = assertThrows(
            TenantContextException.class,
            () -> aspect.enforceTenantContext(joinPoint)
        );
        
        assertEquals("Tenant context is required for repository operations", exception.getMessage());
    }
    
    @Test
    void enforceTenantContext_shouldPropagateRepositoryException() throws Throwable {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        
        RuntimeException expectedException = new RuntimeException("Repository error");
        when(joinPoint.proceed()).thenThrow(expectedException);
        
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> aspect.enforceTenantContext(joinPoint)
        );
        
        assertEquals(expectedException, exception);
    }
}
