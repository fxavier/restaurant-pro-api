package com.restaurantpos.identityaccess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new tenant.
 * Only super admins can create tenants.
 */
public record CreateTenantRequest(
    @NotBlank(message = "Tenant name is required")
    @Size(min = 1, max = 255, message = "Tenant name must be between 1 and 255 characters")
    String name,
    
    @Size(max = 50, message = "Subscription plan must not exceed 50 characters")
    String subscriptionPlan
) {}
