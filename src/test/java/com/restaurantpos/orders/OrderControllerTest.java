package com.restaurantpos.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurantpos.orders.controller.OrderController;
import com.restaurantpos.orders.dto.DiscountDetails;
import com.restaurantpos.orders.entity.Discount;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.exception.ItemUnavailableException;
import com.restaurantpos.orders.model.DiscountType;
import com.restaurantpos.orders.model.OrderStatus;
import com.restaurantpos.orders.model.OrderType;
import com.restaurantpos.orders.service.OrderService;

/**
 * Unit tests for OrderController.
 * Tests REST endpoints for order management operations.
 */
@WebMvcTest(controllers = OrderController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    private UUID tenantId;
    private UUID siteId;
    private UUID tableId;
    private UUID userId;
    private UUID orderId;
    private UUID itemId;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }
    
    @Test
    void createOrder_withValidRequest_returnsCreated() throws Exception {
        // Given
        Order order = new Order(tenantId, siteId, tableId, null, OrderType.DINE_IN);
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        
        when(orderService.createOrder(eq(tableId), eq(OrderType.DINE_IN), eq(siteId), eq(null), any(UUID.class)))
            .thenReturn(order);
        
        String requestBody = String.format("""
            {
                "tableId": "%s",
                "orderType": "DINE_IN",
                "siteId": "%s",
                "customerId": null
            }
            """, tableId, siteId);
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
            .andExpect(jsonPath("$.siteId").value(siteId.toString()))
            .andExpect(jsonPath("$.tableId").value(tableId.toString()))
            .andExpect(jsonPath("$.orderType").value("DINE_IN"))
            .andExpect(jsonPath("$.status").value("OPEN"));
        
        verify(orderService).createOrder(eq(tableId), eq(OrderType.DINE_IN), eq(siteId), eq(null), any(UUID.class));
    }
    
    @Test
    void addOrderLine_withValidRequest_returnsCreated() throws Exception {
        // Given
        OrderLine orderLine = new OrderLine(orderId, itemId, 2, new BigDecimal("10.00"));
        orderLine.setNotes("No ice");
        
        when(orderService.addOrderLine(eq(orderId), eq(itemId), eq(2), any(), eq("No ice"), any(UUID.class)))
            .thenReturn(orderLine);
        
        String requestBody = String.format("""
            {
                "itemId": "%s",
                "quantity": 2,
                "modifiers": {},
                "notes": "No ice"
            }
            """, itemId);
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/lines")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.itemId").value(itemId.toString()))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.notes").value("No ice"))
            .andExpect(jsonPath("$.status").value("PENDING"));
        
        verify(orderService).addOrderLine(eq(orderId), eq(itemId), eq(2), any(), eq("No ice"), any(UUID.class));
    }
    
    @Test
    void addOrderLine_withUnavailableItem_returnsUnprocessableEntity() throws Exception {
        // Given
        when(orderService.addOrderLine(eq(orderId), eq(itemId), eq(2), any(), any(), any(UUID.class)))
            .thenThrow(new ItemUnavailableException(itemId, "Coffee"));
        
        String requestBody = String.format("""
            {
                "itemId": "%s",
                "quantity": 2,
                "modifiers": {},
                "notes": ""
            }
            """, itemId);
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/lines")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnprocessableEntity());
        
        verify(orderService).addOrderLine(eq(orderId), eq(itemId), eq(2), any(), any(), any(UUID.class));
    }
    
    @Test
    void updateOrderLine_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID lineId = UUID.randomUUID();
        OrderLine orderLine = new OrderLine(orderId, itemId, 3, new BigDecimal("10.00"));
        orderLine.setNotes("Extra hot");
        
        when(orderService.updateOrderLine(eq(lineId), eq(3), any(), eq("Extra hot"), any(UUID.class)))
            .thenReturn(orderLine);
        
        String requestBody = """
            {
                "quantity": 3,
                "modifiers": {},
                "notes": "Extra hot"
            }
            """;
        
        // When & Then
        mockMvc.perform(put("/api/orders/" + orderId + "/lines/" + lineId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.notes").value("Extra hot"));
        
        verify(orderService).updateOrderLine(eq(lineId), eq(3), any(), eq("Extra hot"), any(UUID.class));
    }
    
    @Test
    void confirmOrder_withValidRequest_returnsOk() throws Exception {
        // Given
        Order order = new Order(tenantId, siteId, tableId, null, OrderType.DINE_IN);
        order.setStatus(OrderStatus.CONFIRMED);
        
        when(orderService.confirmOrder(eq(orderId), any(UUID.class)))
            .thenReturn(order);
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
        
        verify(orderService).confirmOrder(eq(orderId), any(UUID.class));
    }
    
    @Test
    void voidOrderLine_withValidRequest_returnsOk() throws Exception {
        // Given
        UUID lineId = UUID.randomUUID();
        
        String requestBody = """
            {
                "reason": "Customer changed mind",
                "recordWaste": false
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/lines/" + lineId + "/void")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        verify(orderService).voidOrderLine(eq(lineId), eq("Customer changed mind"), eq(false), any(UUID.class));
    }
    
    @Test
    void applyDiscount_withValidRequest_returnsCreated() throws Exception {
        // Given
        Discount discount = new Discount(
            orderId,
            null,
            DiscountType.PERCENTAGE,
            new BigDecimal("10.00"),
            "Happy hour",
            userId
        );
        
        when(orderService.applyDiscount(eq(orderId), any(DiscountDetails.class), any(UUID.class)))
            .thenReturn(discount);
        
        String requestBody = """
            {
                "orderLineId": null,
                "type": "PERCENTAGE",
                "amount": 10.00,
                "reason": "Happy hour"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/discounts")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.type").value("PERCENTAGE"))
            .andExpect(jsonPath("$.amount").value(10.00))
            .andExpect(jsonPath("$.reason").value("Happy hour"));
        
        verify(orderService).applyDiscount(eq(orderId), any(DiscountDetails.class), any(UUID.class));
    }
    
    @Test
    void getOrdersByTable_withValidTableId_returnsOk() throws Exception {
        // Given
        Order order1 = new Order(tenantId, siteId, tableId, null, OrderType.DINE_IN);
        Order order2 = new Order(tenantId, siteId, tableId, null, OrderType.DINE_IN);
        
        when(orderService.getOrdersByTable(tableId))
            .thenReturn(List.of(order1, order2));
        
        // When & Then
        mockMvc.perform(get("/api/tables/" + tableId + "/orders")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].tableId").value(tableId.toString()))
            .andExpect(jsonPath("$[1].tableId").value(tableId.toString()));
        
        verify(orderService).getOrdersByTable(tableId);
    }
    
    @Test
    void createOrder_withMissingRequiredFields_returnsBadRequest() throws Exception {
        // Given
        String requestBody = """
            {
                "tableId": null,
                "siteId": null
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void addOrderLine_withInvalidQuantity_returnsBadRequest() throws Exception {
        // Given
        String requestBody = String.format("""
            {
                "itemId": "%s",
                "quantity": -1,
                "modifiers": {},
                "notes": ""
            }
            """, itemId);
        
        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/lines")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}
