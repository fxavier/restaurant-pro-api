package com.restaurantpos.customers.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.repository.OrderRepository;

/**
 * Service for managing customer operations including search, creation, updates,
 * and order history retrieval.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@Service
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    
    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Searches for customers by full phone number.
     * Uses the standard index on (tenant_id, phone) for efficient lookup.
     * 
     * Requirements: 8.1 - Support phone number search
     * 
     * @param tenantId the tenant ID
     * @param phone the full phone number
     * @return list of customers with matching phone number
     * @throws IllegalArgumentException if tenantId or phone is null
     */
    @Transactional(readOnly = true)
    public List<Customer> searchByPhone(UUID tenantId, String phone) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        return customerRepository.findByTenantIdAndPhone(tenantId, phone.trim());
    }
    
    /**
     * Searches for customers by phone number suffix (last N digits).
     * Uses the varchar_pattern_ops index for efficient suffix search with LIKE '%suffix'.
     * 
     * The suffix is sanitized to prevent SQL injection by:
     * 1. Trimming whitespace
     * 2. Removing any SQL wildcard characters (%, _)
     * 3. Prepending '%' for the LIKE pattern
     * 
     * Requirements: 8.1 - Support phone suffix search for quick customer lookup
     * Requirements: 8.2 - Display all matches when multiple customers found
     * Requirements: 13.2 - Prevent SQL injection
     * 
     * @param tenantId the tenant ID
     * @param suffix the phone number suffix (last N digits)
     * @return list of customers whose phone ends with the suffix
     * @throws IllegalArgumentException if tenantId or suffix is null
     */
    @Transactional(readOnly = true)
    public List<Customer> searchByPhoneSuffix(UUID tenantId, String suffix) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (suffix == null || suffix.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone suffix cannot be null or empty");
        }
        
        // Sanitize suffix to prevent SQL injection
        // Remove SQL wildcard characters and trim
        String sanitizedSuffix = suffix.trim()
            .replace("%", "")
            .replace("_", "");
        
        if (sanitizedSuffix.isEmpty()) {
            throw new IllegalArgumentException("Phone suffix cannot contain only wildcard characters");
        }
        
        // Prepend '%' for LIKE pattern matching
        String likePattern = "%" + sanitizedSuffix;
        
        return customerRepository.findByTenantIdAndPhoneSuffix(tenantId, likePattern);
    }
    
    /**
     * Creates a new customer for a tenant.
     * 
     * Requirements: 8.3 - Allow creating new customer record if not found
     * 
     * @param tenantId the tenant ID
     * @param name the customer name
     * @param phone the customer phone number
     * @param address the customer address (optional)
     * @param deliveryNotes the delivery notes (optional)
     * @return the created customer
     * @throws IllegalArgumentException if required fields are null or empty
     */
    public Customer createCustomer(UUID tenantId, String name, String phone, String address, String deliveryNotes) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        Customer customer = new Customer(
            tenantId,
            name.trim(),
            phone.trim(),
            address != null ? address.trim() : null,
            deliveryNotes != null ? deliveryNotes.trim() : null
        );
        
        return customerRepository.save(customer);
    }
    
    /**
     * Updates an existing customer's information.
     * 
     * Requirements: 8.3 - Support modifying customer details
     * 
     * @param tenantId the tenant ID (for tenant isolation)
     * @param customerId the customer ID
     * @param name the updated customer name (optional, null to keep existing)
     * @param phone the updated phone number (optional, null to keep existing)
     * @param address the updated address (optional, null to keep existing)
     * @param deliveryNotes the updated delivery notes (optional, null to keep existing)
     * @return the updated customer
     * @throws IllegalArgumentException if customer not found or doesn't belong to tenant
     */
    public Customer updateCustomer(UUID tenantId, UUID customerId, String name, String phone, String address, String deliveryNotes) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found or does not belong to tenant"));
        
        // Update fields only if new values are provided
        if (name != null && !name.trim().isEmpty()) {
            customer.setName(name.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            customer.setPhone(phone.trim());
        }
        if (address != null) {
            customer.setAddress(address.trim());
        }
        if (deliveryNotes != null) {
            customer.setDeliveryNotes(deliveryNotes.trim());
        }
        
        return customerRepository.save(customer);
    }
    
    /**
     * Retrieves the order history for a customer.
     * Returns all orders associated with the customer, ordered by creation date descending.
     * 
     * Requirements: 8.4 - Display customer order history with dates and items
     * 
     * @param tenantId the tenant ID (for tenant isolation)
     * @param customerId the customer ID
     * @return list of orders for the customer, most recent first
     * @throws IllegalArgumentException if customer not found or doesn't belong to tenant
     */
    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(UUID tenantId, UUID customerId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        
        // Verify customer exists and belongs to tenant
        customerRepository.findByIdAndTenantId(customerId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found or does not belong to tenant"));
        
        return orderRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }
}
