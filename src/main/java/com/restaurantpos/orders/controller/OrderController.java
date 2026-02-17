package com.restaurantpos.orders.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurantpos.orders.dto.DiscountDetails;
import com.restaurantpos.orders.entity.Discount;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.exception.ItemUnavailableException;
import com.restaurantpos.orders.exception.OrderException;
import com.restaurantpos.orders.model.OrderLineStatus;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * REST controller for order management operations.
 * Provides endpoints for creating orders, managing order lines, confirming orders,
 * voiding lines, applying discounts, and retrieving orders.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.6, 5.7
 */
@RestController
@RequestMapping("/api")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Creates a new order.
     * 
     * POST /api/orders
     * 
     * Requirements: 5.1
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Creating order for table: {}, type: {}", request.tableId(), request.orderType());
            
            Order order = orderService.createOrder(
                request.tableId(),
                request.orderType(),
                request.siteId(),
                request.customerId(),
                userId
            );
            
            OrderResponse response = toOrderResponse(order);
            logger.info("Order created successfully: {}", order.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets order details by ID.
     * 
     * GET /api/orders/{id}
     * 
     * Requirements: 5.1
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        try {
            logger.debug("Fetching order: {}", id);
            
            // The service will handle tenant isolation
            Order order = orderService.getOrdersByTable(null).stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new OrderException("Order not found: " + id));
            
            OrderResponse response = toOrderResponse(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            logger.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching order: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Adds an order line to an existing order.
     * 
     * POST /api/orders/{id}/lines
     * 
     * Requirements: 5.1, 5.2
     */
    @PostMapping("/orders/{id}/lines")
    public ResponseEntity<OrderLineResponse> addOrderLine(
            @PathVariable UUID id,
            @Valid @RequestBody AddOrderLineRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Adding order line to order {}: item {}, quantity {}", 
                id, request.itemId(), request.quantity());
            
            OrderLine orderLine = orderService.addOrderLine(
                id,
                request.itemId(),
                request.quantity(),
                request.modifiers(),
                request.notes(),
                userId
            );
            
            OrderLineResponse response = toOrderLineResponse(orderLine);
            logger.info("Order line added successfully: {}", orderLine.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ItemUnavailableException e) {
            logger.warn("Item unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (OrderException e) {
            logger.warn("Failed to add order line: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error adding order line: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Updates an existing order line.
     * 
     * PUT /api/orders/{id}/lines/{lineId}
     * 
     * Requirements: 5.2
     */
    @PutMapping("/orders/{id}/lines/{lineId}")
    public ResponseEntity<OrderLineResponse> updateOrderLine(
            @PathVariable UUID id,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateOrderLineRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Updating order line {}: quantity {}", lineId, request.quantity());
            
            OrderLine orderLine = orderService.updateOrderLine(
                lineId,
                request.quantity(),
                request.modifiers(),
                request.notes(),
                userId
            );
            
            OrderLineResponse response = toOrderLineResponse(orderLine);
            logger.info("Order line updated successfully: {}", orderLine.getId());
            
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            logger.warn("Failed to update order line: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error updating order line: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Confirms an order, transitioning order lines to CONFIRMED status.
     * 
     * POST /api/orders/{id}/confirm
     * 
     * Requirements: 5.3
     */
    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Confirming order: {}", id);
            
            Order order = orderService.confirmOrder(id, userId);
            
            OrderResponse response = toOrderResponse(order);
            logger.info("Order confirmed successfully: {}", order.getId());
            
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            logger.warn("Failed to confirm order: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error confirming order: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Voids an order line, canceling it with optional waste tracking.
     * 
     * POST /api/orders/{id}/lines/{lineId}/void
     * 
     * Requirements: 5.6
     */
    @PostMapping("/orders/{id}/lines/{lineId}/void")
    public ResponseEntity<Void> voidOrderLine(
            @PathVariable UUID id,
            @PathVariable UUID lineId,
            @Valid @RequestBody VoidOrderLineRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Voiding order line {}: reason {}", lineId, request.reason());
            
            orderService.voidOrderLine(
                lineId,
                request.reason(),
                Boolean.TRUE.equals(request.recordWaste()),
                userId
            );
            
            logger.info("Order line voided successfully: {}", lineId);
            return ResponseEntity.ok().build();
        } catch (OrderException e) {
            logger.warn("Failed to void order line: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error voiding order line: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Applies a discount to an order or order line.
     * 
     * POST /api/orders/{id}/discounts
     * 
     * Requirements: 5.7
     */
    @PostMapping("/orders/{id}/discounts")
    public ResponseEntity<DiscountResponse> applyDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyDiscountRequest request) {
        try {
            UUID userId = extractUserIdFromAuthentication();
            
            logger.info("Applying discount to order {}: type {}, amount {}", 
                id, request.type(), request.amount());
            
            DiscountDetails discountDetails = new DiscountDetails(
                request.orderLineId(),
                request.type(),
                request.amount(),
                request.reason()
            );
            
            Discount discount = orderService.applyDiscount(id, discountDetails, userId);
            
            DiscountResponse response = toDiscountResponse(discount);
            logger.info("Discount applied successfully: {}", discount.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (OrderException e) {
            logger.warn("Failed to apply discount: {}", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("not found") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Error applying discount: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets all orders for a specific table.
     * 
     * GET /api/tables/{tableId}/orders
     * 
     * Requirements: 5.1
     */
    @GetMapping("/tables/{tableId}/orders")
    public ResponseEntity<List<OrderResponse>> getOrdersByTable(@PathVariable UUID tableId) {
        try {
            logger.debug("Fetching orders for table: {}", tableId);
            
            List<Order> orders = orderService.getOrdersByTable(tableId);
            List<OrderResponse> response = orders.stream()
                .map(this::toOrderResponse)
                .toList();
            
            logger.debug("Retrieved {} orders for table {}", response.size(), tableId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching orders for table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper methods
    
    /**
     * Extracts the user ID from the current authentication context.
     */
    private UUID extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Invalid user ID in JWT: " + subject, e);
                }
            }
        }
        
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
    
    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getTenantId(),
            order.getSiteId(),
            order.getTableId(),
            order.getCustomerId(),
            order.getOrderType(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getVersion()
        );
    }
    
    private OrderLineResponse toOrderLineResponse(OrderLine orderLine) {
        return new OrderLineResponse(
            orderLine.getId(),
            orderLine.getOrderId(),
            orderLine.getItemId(),
            orderLine.getQuantity(),
            orderLine.getUnitPrice(),
            orderLine.getModifiers(),
            orderLine.getNotes(),
            orderLine.getStatus(),
            orderLine.getCreatedAt(),
            orderLine.getUpdatedAt(),
            orderLine.getVersion()
        );
    }
    
    private DiscountResponse toDiscountResponse(Discount discount) {
        return new DiscountResponse(
            discount.getId(),
            discount.getOrderId(),
            discount.getOrderLineId(),
            discount.getType(),
            discount.getAmount(),
            discount.getReason(),
            discount.getAppliedBy(),
            discount.getCreatedAt()
        );
    }
    
    // Request DTOs
    
    /**
     * Request DTO for creating an order.
     */
    public record CreateOrderRequest(
        UUID tableId,  // null for delivery/takeout
        
        @NotNull(message = "Order type is required")
        OrderType orderType,
        
        @NotNull(message = "Site ID is required")
        UUID siteId,
        
        UUID customerId  // null for dine-in
    ) {}
    
    /**
     * Request DTO for adding an order line.
     */
    public record AddOrderLineRequest(
        @NotNull(message = "Item ID is required")
        UUID itemId,
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        
        Map<String, Object> modifiers,
        
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
    ) {}
    
    /**
     * Request DTO for updating an order line.
     */
    public record UpdateOrderLineRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        
        Map<String, Object> modifiers,
        
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
    ) {}
    
    /**
     * Request DTO for voiding an order line.
     */
    public record VoidOrderLineRequest(
        @NotNull(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,
        
        Boolean recordWaste
    ) {}
    
    /**
     * Request DTO for applying a discount.
     */
    public record ApplyDiscountRequest(
        UUID orderLineId,  // null for order-level discount
        
        @NotNull(message = "Discount type is required")
        com.restaurantpos.orders.model.DiscountType type,
        
        @NotNull(message = "Discount amount is required")
        @Positive(message = "Discount amount must be positive")
        BigDecimal amount,
        
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
    ) {}
    
    // Response DTOs
    
    /**
     * Response DTO for order details.
     */
    public record OrderResponse(
        UUID id,
        UUID tenantId,
        UUID siteId,
        UUID tableId,
        UUID customerId,
        OrderType orderType,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        Integer version
    ) {}
    
    /**
     * Response DTO for order line details.
     */
    public record OrderLineResponse(
        UUID id,
        UUID orderId,
        UUID itemId,
        Integer quantity,
        BigDecimal unitPrice,
        Map<String, Object> modifiers,
        String notes,
        OrderLineStatus status,
        Instant createdAt,
        Instant updatedAt,
        Integer version
    ) {}
    
    /**
     * Response DTO for discount details.
     */
    public record DiscountResponse(
        UUID id,
        UUID orderId,
        UUID orderLineId,
        com.restaurantpos.orders.model.DiscountType type,
        BigDecimal amount,
        String reason,
        UUID appliedBy,
        Instant createdAt
    ) {}
}
