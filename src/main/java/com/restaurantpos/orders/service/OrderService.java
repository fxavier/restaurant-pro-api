package com.restaurantpos.orders.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.catalog.api.CatalogService;
import com.restaurantpos.identityaccess.api.AuthorizationApi;
import com.restaurantpos.identityaccess.api.AuthorizationApi.PermissionType;
import com.restaurantpos.orders.dto.DiscountDetails;
import com.restaurantpos.orders.entity.Consumption;
import com.restaurantpos.orders.entity.Discount;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.event.OrderConfirmed;
import com.restaurantpos.orders.exception.ItemUnavailableException;
import com.restaurantpos.orders.exception.OrderException;
import com.restaurantpos.orders.model.OrderLineStatus;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.repository.ConsumptionRepository;
import com.restaurantpos.orders.repository.DiscountRepository;
import com.restaurantpos.orders.repository.OrderLineRepository;
import com.restaurantpos.orders.repository.OrderRepository;

/**
 * Service for managing orders and order lines.
 * Handles order creation, modification, confirmation, void operations, and discounts.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.6, 5.7
 */
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final ConsumptionRepository consumptionRepository;
    private final DiscountRepository discountRepository;
    private final CatalogService catalogService;
    private final AuthorizationApi authorizationApi;
    private final ApplicationEventPublisher eventPublisher;
    
    public OrderService(
            OrderRepository orderRepository,
            OrderLineRepository orderLineRepository,
            ConsumptionRepository consumptionRepository,
            DiscountRepository discountRepository,
            CatalogService catalogService,
            AuthorizationApi authorizationApi,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.consumptionRepository = consumptionRepository;
        this.discountRepository = discountRepository;
        this.catalogService = catalogService;
        this.authorizationApi = authorizationApi;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Creates a new order for a table or customer.
     * 
     * @param tableId the table ID (null for delivery/takeout)
     * @param orderType the type of order (DINE_IN, DELIVERY, TAKEOUT)
     * @param siteId the site ID
     * @param customerId the customer ID (null for dine-in)
     * @param userId the user creating the order
     * @return the created order
     * Requirements: 5.1
     */
    public Order createOrder(UUID tableId, OrderType orderType, UUID siteId, UUID customerId, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        Order order = new Order(tenantId, siteId, tableId, customerId, orderType);
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        
        return orderRepository.save(order);
    }
    
    /**
     * Adds an order line to an existing order.
     * Validates that the item is available before adding.
     * 
     * @param orderId the order ID
     * @param itemId the item ID
     * @param quantity the quantity to order
     * @param modifiers optional modifiers (size, extras, etc.)
     * @param notes optional preparation notes
     * @param userId the user adding the line
     * @return the created order line
     * @throws OrderException if order not found or not in OPEN status
     * @throws ItemUnavailableException if item is not available
     * Requirements: 5.1, 5.2, 4.3
     */
    public OrderLine addOrderLine(UUID orderId, UUID itemId, Integer quantity, 
                                   Map<String, Object> modifiers, String notes, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Validate order exists and is open
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        
        if (!order.isOpen()) {
            throw new OrderException("Cannot add items to order with status: " + order.getStatus());
        }
        
        // Validate item exists and is available using catalog API
        CatalogService.ItemInfo item = catalogService.getItem(itemId, tenantId)
            .orElseThrow(() -> new OrderException("Item not found: " + itemId));
        
        if (!item.available()) {
            throw new ItemUnavailableException(itemId, item.name());
        }
        
        // Create order line with PENDING status
        OrderLine orderLine = new OrderLine(orderId, itemId, quantity, item.basePrice());
        orderLine.setModifiers(modifiers);
        orderLine.setNotes(notes);
        
        OrderLine savedLine = orderLineRepository.save(orderLine);
        
        // Update order total
        updateOrderTotal(order);
        order.setUpdatedBy(userId);
        orderRepository.save(order);
        
        return savedLine;
    }
    
    /**
     * Updates an existing order line.
     * Only PENDING order lines can be modified.
     * 
     * @param orderLineId the order line ID
     * @param quantity the new quantity
     * @param modifiers the new modifiers
     * @param notes the new notes
     * @param userId the user updating the line
     * @return the updated order line
     * @throws OrderException if order line not found or already confirmed
     * Requirements: 5.2
     */
    public OrderLine updateOrderLine(UUID orderLineId, Integer quantity, 
                                      Map<String, Object> modifiers, String notes, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        OrderLine orderLine = orderLineRepository.findById(orderLineId)
            .orElseThrow(() -> new OrderException("Order line not found: " + orderLineId));
        
        // Verify tenant isolation through order
        Order order = orderRepository.findByIdAndTenantId(orderLine.getOrderId(), tenantId)
            .orElseThrow(() -> new OrderException("Order not found or access denied"));
        
        if (!orderLine.isPending()) {
            throw new OrderException("Cannot modify order line with status: " + orderLine.getStatus());
        }
        
        orderLine.setQuantity(quantity);
        orderLine.setModifiers(modifiers);
        orderLine.setNotes(notes);
        
        OrderLine savedLine = orderLineRepository.save(orderLine);
        
        // Update order total
        updateOrderTotal(order);
        order.setUpdatedBy(userId);
        orderRepository.save(order);
        
        return savedLine;
    }
    
    /**
     * Confirms an order, transitioning all PENDING order lines to CONFIRMED
     * and creating consumption records.
     * Emits OrderConfirmed event for print job creation.
     * 
     * @param orderId the order ID
     * @param userId the user confirming the order
     * @return the confirmed order
     * @throws OrderException if order not found or not in OPEN status
     * Requirements: 5.3, 5.4, 6.1
     */
    public Order confirmOrder(UUID orderId, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        
        if (!order.isOpen()) {
            throw new OrderException("Cannot confirm order with status: " + order.getStatus());
        }
        
        // Get all pending order lines
        List<OrderLine> pendingLines = orderLineRepository.findByOrderIdAndStatus(
            orderId, OrderLineStatus.PENDING);
        
        if (pendingLines.isEmpty()) {
            throw new OrderException("No pending order lines to confirm");
        }
        
        Instant confirmedAt = Instant.now();
        
        // Transition order lines to CONFIRMED and create consumptions
        for (OrderLine line : pendingLines) {
            line.setStatus(OrderLineStatus.CONFIRMED);
            orderLineRepository.save(line);
            
            // Create consumption record
            Consumption consumption = new Consumption(
                tenantId, 
                line.getId(), 
                line.getQuantity(), 
                confirmedAt
            );
            consumption.setCreatedBy(userId);
            consumptionRepository.save(consumption);
        }
        
        // Update order status
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedBy(userId);
        Order savedOrder = orderRepository.save(order);
        
        // Emit event for print job creation
        eventPublisher.publishEvent(new OrderConfirmed(
            orderId, 
            tenantId, 
            order.getSiteId(), 
            confirmedAt
        ));
        
        return savedOrder;
    }
    
    /**
     * Voids an order line, canceling it with optional waste tracking.
     * Requires permission check for confirmed order lines.
     * 
     * @param orderLineId the order line ID
     * @param reason the reason for voiding
     * @param recordWaste whether to record waste for inventory
     * @param userId the user voiding the line
     * @throws OrderException if order line not found or already voided
     * Requirements: 5.5, 5.6
     */
    public void voidOrderLine(UUID orderLineId, String reason, boolean recordWaste, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        OrderLine orderLine = orderLineRepository.findById(orderLineId)
            .orElseThrow(() -> new OrderException("Order line not found: " + orderLineId));
        
        // Verify tenant isolation through order
        Order order = orderRepository.findByIdAndTenantId(orderLine.getOrderId(), tenantId)
            .orElseThrow(() -> new OrderException("Order not found or access denied"));
        
        if (orderLine.isVoided()) {
            throw new OrderException("Order line is already voided");
        }
        
        // Check permission if order line is confirmed
        if (orderLine.isConfirmed()) {
            authorizationApi.requirePermission(userId, PermissionType.VOID_AFTER_SUBTOTAL);
            
            // Void the consumption record
            List<Consumption> consumptions = consumptionRepository.findByTenantIdAndOrderLineId(
                tenantId, orderLineId);
            for (Consumption consumption : consumptions) {
                if (!consumption.isVoided()) {
                    consumption.setVoidedAt(Instant.now());
                    consumptionRepository.save(consumption);
                }
            }
        }
        
        // Void the order line
        orderLine.setStatus(OrderLineStatus.VOIDED);
        orderLineRepository.save(orderLine);
        
        // Update order total
        updateOrderTotal(order);
        order.setUpdatedBy(userId);
        orderRepository.save(order);
        
        // TODO: If recordWaste is true, create waste tracking record (future requirement)
    }
    
    /**
     * Applies a discount to an order or order line.
     * Requires APPLY_DISCOUNT permission.
     * 
     * @param orderId the order ID
     * @param discountDetails the discount details
     * @param userId the user applying the discount
     * @return the created discount
     * @throws OrderException if order not found
     * Requirements: 5.7
     */
    public Discount applyDiscount(UUID orderId, DiscountDetails discountDetails, UUID userId) {
        UUID tenantId = authorizationApi.getTenantContext();
        
        // Check permission
        authorizationApi.requirePermission(userId, PermissionType.APPLY_DISCOUNT);
        
        // Validate order exists
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        
        // If line discount, validate order line exists
        if (discountDetails.isLineDiscount()) {
            OrderLine orderLine = orderLineRepository.findById(discountDetails.orderLineId())
                .orElseThrow(() -> new OrderException("Order line not found: " + discountDetails.orderLineId()));
            
            if (!orderLine.getOrderId().equals(orderId)) {
                throw new OrderException("Order line does not belong to this order");
            }
        }
        
        // Create discount
        Discount discount = new Discount(
            orderId,
            discountDetails.orderLineId(),
            discountDetails.type(),
            discountDetails.amount(),
            discountDetails.reason(),
            userId
        );
        
        Discount savedDiscount = discountRepository.save(discount);
        
        // Update order total
        updateOrderTotal(order);
        order.setUpdatedBy(userId);
        orderRepository.save(order);
        
        return savedDiscount;
    }
    
    /**
     * Gets all orders for a specific table.
     * 
     * @param tableId the table ID
     * @return list of orders for the table
     * Requirements: 5.1
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByTable(UUID tableId) {
        UUID tenantId = authorizationApi.getTenantContext();
        return orderRepository.findByTenantIdAndTableId(tenantId, tableId);
    }
    
    /**
     * Updates the total amount for an order based on order lines and discounts.
     * 
     * @param order the order to update
     */
    private void updateOrderTotal(Order order) {
        // Calculate total from non-voided order lines
        List<OrderLine> orderLines = orderLineRepository.findByOrderId(order.getId());
        BigDecimal lineTotal = orderLines.stream()
            .filter(line -> !line.isVoided())
            .map(OrderLine::calculateTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Apply discounts
        List<Discount> discounts = discountRepository.findByOrderId(order.getId());
        BigDecimal discountTotal = BigDecimal.ZERO;
        
        for (Discount discount : discounts) {
            if (discount.isOrderDiscount()) {
                // Order-level discount
                if (discount.isPercentage()) {
                    discountTotal = discountTotal.add(
                        lineTotal.multiply(discount.getAmount()).divide(BigDecimal.valueOf(100))
                    );
                } else {
                    discountTotal = discountTotal.add(discount.getAmount());
                }
            } else {
                // Line-level discount
                OrderLine line = orderLineRepository.findById(discount.getOrderLineId()).orElse(null);
                if (line != null && !line.isVoided()) {
                    BigDecimal lineAmount = line.calculateTotal();
                    if (discount.isPercentage()) {
                        discountTotal = discountTotal.add(
                            lineAmount.multiply(discount.getAmount()).divide(BigDecimal.valueOf(100))
                        );
                    } else {
                        discountTotal = discountTotal.add(discount.getAmount());
                    }
                }
            }
        }
        
        BigDecimal total = lineTotal.subtract(discountTotal);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        
        order.setTotalAmount(total);
    }
}
