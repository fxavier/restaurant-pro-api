package com.restaurantpos.customers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.restaurantpos.customers.controller.CustomerController;
import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.service.CustomerService;
import com.restaurantpos.identityaccess.tenant.TenantContext;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.model.OrderType;

/**
 * Unit tests for CustomerController.
 * Tests all REST endpoints for customer management operations.
 */
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {
    
    @Mock
    private CustomerService customerService;
    
    @InjectMocks
    private CustomerController customerController;
    
    private UUID tenantId;
    private UUID customerId;
    private Customer testCustomer;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        
        testCustomer = new Customer(
            tenantId,
            "John Doe",
            "1234567890",
            "123 Main St",
            "Ring doorbell"
        );
        
        // Set tenant context for tests
        TenantContext.setTenantId(tenantId);
    }
    
    @Test
    void searchCustomers_byPhone_returnsCustomers() {
        // Arrange
        String phone = "1234567890";
        when(customerService.searchByPhone(tenantId, phone))
            .thenReturn(List.of(testCustomer));
        
        // Act
        ResponseEntity<List<CustomerController.CustomerResponse>> response = 
            customerController.searchCustomers(phone, null);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("John Doe", response.getBody().get(0).name());
        assertEquals(phone, response.getBody().get(0).phone());
        
        verify(customerService).searchByPhone(tenantId, phone);
    }
    
    @Test
    void searchCustomers_bySuffix_returnsCustomers() {
        // Arrange
        String suffix = "7890";
        when(customerService.searchByPhoneSuffix(tenantId, suffix))
            .thenReturn(List.of(testCustomer));
        
        // Act
        ResponseEntity<List<CustomerController.CustomerResponse>> response = 
            customerController.searchCustomers(null, suffix);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("John Doe", response.getBody().get(0).name());
        
        verify(customerService).searchByPhoneSuffix(tenantId, suffix);
    }
    
    @Test
    void searchCustomers_withBothParameters_returnsBadRequest() {
        // Act
        ResponseEntity<List<CustomerController.CustomerResponse>> response = 
            customerController.searchCustomers("1234567890", "7890");
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void searchCustomers_withNoParameters_returnsBadRequest() {
        // Act
        ResponseEntity<List<CustomerController.CustomerResponse>> response = 
            customerController.searchCustomers(null, null);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void searchCustomers_noTenantContext_returnsUnauthorized() {
        // Arrange
        TenantContext.clear();
        
        // Act
        ResponseEntity<List<CustomerController.CustomerResponse>> response = 
            customerController.searchCustomers("1234567890", null);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void createCustomer_validRequest_returnsCreated() {
        // Arrange
        CustomerController.CreateCustomerRequest request = 
            new CustomerController.CreateCustomerRequest(
                "John Doe",
                "1234567890",
                "123 Main St",
                "Ring doorbell"
            );
        
        when(customerService.createCustomer(
            eq(tenantId),
            eq("John Doe"),
            eq("1234567890"),
            eq("123 Main St"),
            eq("Ring doorbell")
        )).thenReturn(testCustomer);
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.createCustomer(request);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Doe", response.getBody().name());
        assertEquals("1234567890", response.getBody().phone());
        
        verify(customerService).createCustomer(
            tenantId, "John Doe", "1234567890", "123 Main St", "Ring doorbell"
        );
    }
    
    @Test
    void createCustomer_noTenantContext_returnsUnauthorized() {
        // Arrange
        TenantContext.clear();
        CustomerController.CreateCustomerRequest request = 
            new CustomerController.CreateCustomerRequest(
                "John Doe",
                "1234567890",
                null,
                null
            );
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.createCustomer(request);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void createCustomer_serviceThrowsException_returnsBadRequest() {
        // Arrange
        CustomerController.CreateCustomerRequest request = 
            new CustomerController.CreateCustomerRequest(
                "John Doe",
                "1234567890",
                null,
                null
            );
        
        when(customerService.createCustomer(any(), any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Invalid data"));
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.createCustomer(request);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    void updateCustomer_validRequest_returnsOk() {
        // Arrange
        CustomerController.UpdateCustomerRequest request = 
            new CustomerController.UpdateCustomerRequest(
                "Jane Doe",
                "0987654321",
                "456 Oak Ave",
                "Leave at door"
            );
        
        Customer updatedCustomer = new Customer(
            tenantId,
            "Jane Doe",
            "0987654321",
            "456 Oak Ave",
            "Leave at door"
        );
        
        when(customerService.updateCustomer(
            eq(tenantId),
            eq(customerId),
            eq("Jane Doe"),
            eq("0987654321"),
            eq("456 Oak Ave"),
            eq("Leave at door")
        )).thenReturn(updatedCustomer);
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.updateCustomer(customerId, request);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Jane Doe", response.getBody().name());
        assertEquals("0987654321", response.getBody().phone());
        
        verify(customerService).updateCustomer(
            tenantId, customerId, "Jane Doe", "0987654321", "456 Oak Ave", "Leave at door"
        );
    }
    
    @Test
    void updateCustomer_customerNotFound_returnsNotFound() {
        // Arrange
        CustomerController.UpdateCustomerRequest request = 
            new CustomerController.UpdateCustomerRequest(
                "Jane Doe",
                null,
                null,
                null
            );
        
        when(customerService.updateCustomer(any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Customer not found"));
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.updateCustomer(customerId, request);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    void updateCustomer_noTenantContext_returnsUnauthorized() {
        // Arrange
        TenantContext.clear();
        CustomerController.UpdateCustomerRequest request = 
            new CustomerController.UpdateCustomerRequest(
                "Jane Doe",
                null,
                null,
                null
            );
        
        // Act
        ResponseEntity<CustomerController.CustomerResponse> response = 
            customerController.updateCustomer(customerId, request);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void getOrderHistory_validCustomer_returnsOrders() {
        // Arrange
        UUID siteId = UUID.randomUUID();
        
        Order order = new Order(
            tenantId,
            siteId,
            null,
            customerId,
            OrderType.DELIVERY
        );
        
        when(customerService.getOrderHistory(tenantId, customerId))
            .thenReturn(List.of(order));
        
        // Act
        ResponseEntity<List<CustomerController.OrderHistoryResponse>> response = 
            customerController.getOrderHistory(customerId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(OrderType.DELIVERY, response.getBody().get(0).orderType());
        
        verify(customerService).getOrderHistory(tenantId, customerId);
    }
    
    @Test
    void getOrderHistory_customerNotFound_returnsNotFound() {
        // Arrange
        when(customerService.getOrderHistory(any(), any()))
            .thenThrow(new IllegalArgumentException("Customer not found"));
        
        // Act
        ResponseEntity<List<CustomerController.OrderHistoryResponse>> response = 
            customerController.getOrderHistory(customerId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    void getOrderHistory_noTenantContext_returnsUnauthorized() {
        // Arrange
        TenantContext.clear();
        
        // Act
        ResponseEntity<List<CustomerController.OrderHistoryResponse>> response = 
            customerController.getOrderHistory(customerId);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(customerService);
    }
    
    @Test
    void getOrderHistory_emptyHistory_returnsEmptyList() {
        // Arrange
        when(customerService.getOrderHistory(tenantId, customerId))
            .thenReturn(List.of());
        
        // Act
        ResponseEntity<List<CustomerController.OrderHistoryResponse>> response = 
            customerController.getOrderHistory(customerId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
