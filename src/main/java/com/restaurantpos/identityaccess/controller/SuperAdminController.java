package com.restaurantpos.identityaccess.controller;

import com.restaurantpos.identityaccess.dto.AuthResponse;
import com.restaurantpos.identityaccess.dto.CreateTenantRequest;
import com.restaurantpos.identityaccess.dto.LoginRequest;
import com.restaurantpos.identityaccess.dto.SuperAdminRegisterRequest;
import com.restaurantpos.identityaccess.service.AuthenticationService;
import com.restaurantpos.tenantprovisioning.entity.Tenant;
import com.restaurantpos.tenantprovisioning.service.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for super admin operations.
 * Super admins can register, login, and create tenants.
 */
@RestController
@RequestMapping("/api/super-admin")
@Tag(name = "Super Admin", description = "Super admin authentication and tenant management")
public class SuperAdminController {
    
    private final AuthenticationService authenticationService;
    private final TenantProvisioningService tenantProvisioningService;
    
    public SuperAdminController(
            AuthenticationService authenticationService,
            TenantProvisioningService tenantProvisioningService) {
        this.authenticationService = authenticationService;
        this.tenantProvisioningService = tenantProvisioningService;
    }
    
    /**
     * Register a new super admin account.
     * This endpoint should be protected in production (e.g., only accessible during initial setup).
     */
    @PostMapping("/register")
    @Operation(summary = "Register super admin", description = "Register a new super admin account with system-wide access")
    public ResponseEntity<AuthResponse> registerSuperAdmin(@Valid @RequestBody SuperAdminRegisterRequest request) {
        AuthResponse response = authenticationService.registerSuperAdmin(
                request.username(),
                request.password(),
                request.email()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Login as super admin.
     */
    @PostMapping("/login")
    @Operation(summary = "Super admin login", description = "Authenticate as super admin and receive JWT tokens")
    public ResponseEntity<AuthResponse> loginSuperAdmin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.loginSuperAdmin(
                request.username(),
                request.password()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new tenant.
     * Only super admins can create tenants.
     */
    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create tenant", description = "Create a new tenant (super admin only)")
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantProvisioningService.provisionTenant(
                request.name(),
                request.subscriptionPlan()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }
}
