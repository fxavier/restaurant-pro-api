package com.restaurantpos.customers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.restaurantpos.customers.entity.Customer;

/**
 * Unit tests for Customer entity.
 */
class CustomerEntityTest {
    
    @Test
    void shouldCreateCustomerWithAllFields() {
        UUID tenantId = UUID.randomUUID();
        String name = "John Doe";
        String phone = "1234567890";
        String address = "123 Main St";
        String deliveryNotes = "Ring doorbell";
        
        Customer customer = new Customer(tenantId, name, phone, address, deliveryNotes);
        
        assertThat(customer.getTenantId()).isEqualTo(tenantId);
        assertThat(customer.getName()).isEqualTo(name);
        assertThat(customer.getPhone()).isEqualTo(phone);
        assertThat(customer.getAddress()).isEqualTo(address);
        assertThat(customer.getDeliveryNotes()).isEqualTo(deliveryNotes);
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldUpdateCustomerFields() {
        UUID tenantId = UUID.randomUUID();
        Customer customer = new Customer(tenantId, "John Doe", "1234567890", "123 Main St", "Ring doorbell");
        
        customer.setName("Jane Doe");
        customer.setPhone("0987654321");
        customer.setAddress("456 Oak Ave");
        customer.setDeliveryNotes("Leave at door");
        
        assertThat(customer.getName()).isEqualTo("Jane Doe");
        assertThat(customer.getPhone()).isEqualTo("0987654321");
        assertThat(customer.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(customer.getDeliveryNotes()).isEqualTo("Leave at door");
    }
    
    @Test
    void shouldCreateCustomerWithNullOptionalFields() {
        UUID tenantId = UUID.randomUUID();
        Customer customer = new Customer(tenantId, "John Doe", "1234567890", null, null);
        
        assertThat(customer.getAddress()).isNull();
        assertThat(customer.getDeliveryNotes()).isNull();
    }
}
