package com.restaurantpos.customers.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.restaurantpos.customers.entity.Customer;

/**
 * Repository for Customer entity with tenant filtering and phone search capabilities.
 * Supports both full phone number search and suffix-based search for efficient
 * customer lookup during phone orders.
 * 
 * Requirements: 8.1, 8.5
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    /**
     * Finds all customers for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of customers
     */
    List<Customer> findByTenantId(UUID tenantId);
    
    /**
     * Finds a customer by ID and tenant ID (for tenant isolation).
     * 
     * @param id the customer ID
     * @param tenantId the tenant ID
     * @return the customer if found
     */
    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds customers by exact phone number match.
     * This method uses the standard index on (tenant_id, phone).
     * 
     * @param tenantId the tenant ID
     * @param phone the full phone number
     * @return list of customers with matching phone number
     */
    List<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);
    
    /**
     * Finds customers by phone number suffix (last N digits).
     * This method uses the varchar_pattern_ops index for efficient suffix search.
     * The LIKE pattern '%suffix' is optimized by the specialized index.
     * 
     * Note: The suffix parameter should be passed with the '%' prefix already included
     * (e.g., "%1234") to prevent SQL injection. The service layer is responsible for
     * sanitizing and formatting the input.
     * 
     * Requirements: 8.1 - Support phone suffix search for quick customer lookup
     * 
     * @param tenantId the tenant ID
     * @param suffix the phone number suffix with '%' prefix (e.g., "%1234")
     * @return list of customers whose phone ends with the suffix
     */
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.phone LIKE :suffix")
    List<Customer> findByTenantIdAndPhoneSuffix(@Param("tenantId") UUID tenantId, @Param("suffix") String suffix);
    
    /**
     * Checks if a customer with the given phone number exists for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param phone the phone number
     * @return true if a customer with the phone exists
     */
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);
    
    /**
     * Counts the number of customers for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return the count of customers
     */
    long countByTenantId(UUID tenantId);
}
