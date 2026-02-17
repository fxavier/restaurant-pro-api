package com.restaurantpos.customers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.customers.service.CustomerService;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.repository.OrderRepository;

/**
 * Unit tests for CustomerService.
 * Tests customer search, creation, updates, and order history retrieval.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    private UUID tenantId;
    private UUID customerId;
    private Customer customer;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        customer = new Customer(tenantId, "John Doe", "1234567890", "123 Main St", "Ring doorbell");
    }
    
    @Test
    void searchByPhone_shouldReturnCustomers() {
        // Arrange
        String phone = "1234567890";
        when(customerRepository.findByTenantIdAndPhone(tenantId, phone))
            .thenReturn(Arrays.asList(customer));
        
        // Act
        List<Customer> result = customerService.searchByPhone(tenantId, phone);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(customer, result.get(0));
        verify(customerRepository).findByTenantIdAndPhone(tenantId, phone);
    }
    
    @Test
    void searchByPhone_shouldTrimPhone() {
        // Arrange
        String phone = "  1234567890  ";
        when(customerRepository.findByTenantIdAndPhone(tenantId, "1234567890"))
            .thenReturn(Arrays.asList(customer));
        
        // Act
        List<Customer> result = customerService.searchByPhone(tenantId, phone);
        
        // Assert
        assertNotNull(result);
        verify(customerRepository).findByTenantIdAndPhone(tenantId, "1234567890");
    }
    
    @Test
    void searchByPhone_shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhone(null, "1234567890"));
    }
    
    @Test
    void searchByPhone_shouldThrowExceptionWhenPhoneIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhone(tenantId, null));
    }
    
    @Test
    void searchByPhone_shouldThrowExceptionWhenPhoneIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhone(tenantId, "   "));
    }
    
    @Test
    void searchByPhoneSuffix_shouldReturnCustomers() {
        // Arrange
        String suffix = "7890";
        when(customerRepository.findByTenantIdAndPhoneSuffix(tenantId, suffix))
            .thenReturn(Arrays.asList(customer));
        
        // Act
        List<Customer> result = customerService.searchByPhoneSuffix(tenantId, suffix);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(customer, result.get(0));
        verify(customerRepository).findByTenantIdAndPhoneSuffix(tenantId, suffix);
    }
    
    @Test
    void searchByPhoneSuffix_shouldTrimSuffix() {
        // Arrange
        String suffix = "  7890  ";
        when(customerRepository.findByTenantIdAndPhoneSuffix(tenantId, "7890"))
            .thenReturn(Arrays.asList(customer));
        
        // Act
        List<Customer> result = customerService.searchByPhoneSuffix(tenantId, suffix);
        
        // Assert
        assertNotNull(result);
        verify(customerRepository).findByTenantIdAndPhoneSuffix(tenantId, "7890");
    }
    
    @Test
    void searchByPhoneSuffix_shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhoneSuffix(null, "7890"));
    }
    
    @Test
    void searchByPhoneSuffix_shouldThrowExceptionWhenSuffixIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhoneSuffix(tenantId, null));
    }
    
    @Test
    void searchByPhoneSuffix_shouldThrowExceptionWhenSuffixIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.searchByPhoneSuffix(tenantId, "   "));
    }
    
    @Test
    void createCustomer_shouldCreateAndReturnCustomer() {
        // Arrange
        String name = "Jane Smith";
        String phone = "9876543210";
        String address = "456 Oak Ave";
        String deliveryNotes = "Leave at door";
        
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Customer result = customerService.createCustomer(tenantId, name, phone, address, deliveryNotes);
        
        // Assert
        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId());
        assertEquals(name, result.getName());
        assertEquals(phone, result.getPhone());
        assertEquals(address, result.getAddress());
        assertEquals(deliveryNotes, result.getDeliveryNotes());
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    void createCustomer_shouldTrimInputs() {
        // Arrange
        String name = "  Jane Smith  ";
        String phone = "  9876543210  ";
        String address = "  456 Oak Ave  ";
        String deliveryNotes = "  Leave at door  ";
        
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Customer result = customerService.createCustomer(tenantId, name, phone, address, deliveryNotes);
        
        // Assert
        assertEquals("Jane Smith", result.getName());
        assertEquals("9876543210", result.getPhone());
        assertEquals("456 Oak Ave", result.getAddress());
        assertEquals("Leave at door", result.getDeliveryNotes());
    }
    
    @Test
    void createCustomer_shouldHandleNullOptionalFields() {
        // Arrange
        String name = "Jane Smith";
        String phone = "9876543210";
        
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Customer result = customerService.createCustomer(tenantId, name, phone, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(phone, result.getPhone());
        assertNull(result.getAddress());
        assertNull(result.getDeliveryNotes());
    }
    
    @Test
    void createCustomer_shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.createCustomer(null, "Jane", "123", "addr", "notes"));
    }
    
    @Test
    void createCustomer_shouldThrowExceptionWhenNameIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.createCustomer(tenantId, null, "123", "addr", "notes"));
    }
    
    @Test
    void createCustomer_shouldThrowExceptionWhenNameIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.createCustomer(tenantId, "   ", "123", "addr", "notes"));
    }
    
    @Test
    void createCustomer_shouldThrowExceptionWhenPhoneIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.createCustomer(tenantId, "Jane", null, "addr", "notes"));
    }
    
    @Test
    void createCustomer_shouldThrowExceptionWhenPhoneIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.createCustomer(tenantId, "Jane", "   ", "addr", "notes"));
    }
    
    @Test
    void updateCustomer_shouldUpdateAllFields() {
        // Arrange
        Customer existingCustomer = new Customer(tenantId, "Old Name", "1111111111", "Old Address", "Old Notes");
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Customer result = customerService.updateCustomer(
            tenantId, customerId, "New Name", "2222222222", "New Address", "New Notes"
        );
        
        // Assert
        assertEquals("New Name", result.getName());
        assertEquals("2222222222", result.getPhone());
        assertEquals("New Address", result.getAddress());
        assertEquals("New Notes", result.getDeliveryNotes());
        verify(customerRepository).save(existingCustomer);
    }
    
    @Test
    void updateCustomer_shouldOnlyUpdateProvidedFields() {
        // Arrange
        Customer existingCustomer = new Customer(tenantId, "Old Name", "1111111111", "Old Address", "Old Notes");
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act - only update name
        Customer result = customerService.updateCustomer(
            tenantId, customerId, "New Name", null, null, null
        );
        
        // Assert
        assertEquals("New Name", result.getName());
        assertEquals("1111111111", result.getPhone()); // unchanged
        assertEquals("Old Address", result.getAddress()); // unchanged
        assertEquals("Old Notes", result.getDeliveryNotes()); // unchanged
    }
    
    @Test
    void updateCustomer_shouldTrimInputs() {
        // Arrange
        Customer existingCustomer = new Customer(tenantId, "Old Name", "1111111111", "Old Address", "Old Notes");
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Customer result = customerService.updateCustomer(
            tenantId, customerId, "  New Name  ", "  2222222222  ", "  New Address  ", "  New Notes  "
        );
        
        // Assert
        assertEquals("New Name", result.getName());
        assertEquals("2222222222", result.getPhone());
        assertEquals("New Address", result.getAddress());
        assertEquals("New Notes", result.getDeliveryNotes());
    }
    
    @Test
    void updateCustomer_shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.updateCustomer(tenantId, customerId, "New Name", null, null, null));
    }
    
    @Test
    void updateCustomer_shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.updateCustomer(null, customerId, "New Name", null, null, null));
    }
    
    @Test
    void updateCustomer_shouldThrowExceptionWhenCustomerIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.updateCustomer(tenantId, null, "New Name", null, null, null));
    }
    
    @Test
    void getOrderHistory_shouldReturnOrders() {
        // Arrange
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);
        List<Order> orders = Arrays.asList(order1, order2);
        
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.of(customer));
        when(orderRepository.findByTenantIdAndCustomerId(tenantId, customerId))
            .thenReturn(orders);
        
        // Act
        List<Order> result = customerService.getOrderHistory(tenantId, customerId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(orders, result);
        verify(customerRepository).findByIdAndTenantId(customerId, tenantId);
        verify(orderRepository).findByTenantIdAndCustomerId(tenantId, customerId);
    }
    
    @Test
    void getOrderHistory_shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        when(customerRepository.findByIdAndTenantId(customerId, tenantId))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.getOrderHistory(tenantId, customerId));
    }
    
    @Test
    void getOrderHistory_shouldThrowExceptionWhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.getOrderHistory(null, customerId));
    }
    
    @Test
    void getOrderHistory_shouldThrowExceptionWhenCustomerIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> customerService.getOrderHistory(tenantId, null));
    }
}
