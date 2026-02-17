package com.restaurantpos.customers.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.service.CustomerService;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.orders.entity.Order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST controller for customer management operations.
 * Provides endpoints for searching customers by phone, creating/updating customers,
 * and retrieving customer order history.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * Searches for customers by phone number or suffix.
     * Supports both full phone number search and suffix-based search.
     * 
     * GET /api/customers/search?phone={phone}
     * GET /api/customers/search?suffix={suffix}
     * 
     * Requirements: 8.1, 8.2
     */
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String suffix) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Validate that exactly one search parameter is provided
            if ((phone == null && suffix == null) || (phone != null && suffix != null)) {
                logger.warn("Invalid search parameters: must provide either phone or suffix, not both");
                return ResponseEntity.badRequest().build();
            }
            
            List<Customer> customers;
            if (phone != null) {
                logger.debug("Searching customers by phone: {} for tenant: {}", phone, tenantId);
                customers = customerService.searchByPhone(tenantId, phone);
            } else {
                logger.debug("Searching customers by suffix: {} for tenant: {}", suffix, tenantId);
                customers = customerService.searchByPhoneSuffix(tenantId, suffix);
            }
            
            List<CustomerResponse> response = customers.stream()
                .map(this::toCustomerResponse)
                .toList();
            
            logger.debug("Found {} customers", response.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to search customers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error searching customers: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Creates a new customer for the current tenant.
     * 
     * POST /api/customers
     * 
     * Requirements: 8.3
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Creating customer for tenant {}: name={}, phone={}", 
                tenantId, request.name(), request.phone());
            
            Customer customer = customerService.createCustomer(
                tenantId,
                request.name(),
                request.phone(),
                request.address(),
                request.deliveryNotes()
            );
            
            CustomerResponse response = toCustomerResponse(customer);
            logger.info("Customer created successfully: {}", customer.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create customer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Updates an existing customer's information.
     * 
     * PUT /api/customers/{id}
     * 
     * Requirements: 8.3
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Updating customer {}: name={}, phone={}", 
                id, request.name(), request.phone());
            
            Customer customer = customerService.updateCustomer(
                tenantId,
                id,
                request.name(),
                request.phone(),
                request.address(),
                request.deliveryNotes()
            );
            
            CustomerResponse response = toCustomerResponse(customer);
            logger.info("Customer updated successfully: {}", customer.getId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update customer: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error updating customer: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets the order history for a specific customer.
     * 
     * GET /api/customers/{id}/orders
     * 
     * Requirements: 8.4
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(@PathVariable UUID id) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                logger.warn("No tenant context available");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.debug("Fetching order history for customer: {}", id);
            
            List<Order> orders = customerService.getOrderHistory(tenantId, id);
            List<OrderHistoryResponse> response = orders.stream()
                .map(this::toOrderHistoryResponse)
                .toList();
            
            logger.debug("Retrieved {} orders for customer {}", response.size(), id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to fetch order history: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error fetching order history: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    private CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getTenantId(),
            customer.getName(),
            customer.getPhone(),
            customer.getAddress(),
            customer.getDeliveryNotes(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
    
    private OrderHistoryResponse toOrderHistoryResponse(Order order) {
        return new OrderHistoryResponse(
            order.getId(),
            order.getOrderType(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for creating a customer.
     */
    public record CreateCustomerRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name,
        
        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,
        
        @Size(max = 1000, message = "Address must not exceed 1000 characters")
        String address,
        
        @Size(max = 500, message = "Delivery notes must not exceed 500 characters")
        String deliveryNotes
    ) {}
    
    /**
     * Request DTO for updating a customer.
     * All fields are optional - only provided fields will be updated.
     */
    public record UpdateCustomerRequest(
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name,
        
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,
        
        @Size(max = 1000, message = "Address must not exceed 1000 characters")
        String address,
        
        @Size(max = 500, message = "Delivery notes must not exceed 500 characters")
        String deliveryNotes
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for customer details.
     */
    public record CustomerResponse(
        UUID id,
        UUID tenantId,
        String name,
        String phone,
        String address,
        String deliveryNotes,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    /**
     * Response DTO for order history.
     * Simplified view showing key order information.
     */
    public record OrderHistoryResponse(
        UUID id,
        com.restaurantpos.orders.model.OrderType orderType,
        com.restaurantpos.orders.model.OrderStatus status,
        java.math.BigDecimal totalAmount,
        Instant createdAt
    ) {}
}
