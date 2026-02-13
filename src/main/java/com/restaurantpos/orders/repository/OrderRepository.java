package com.restaurantpos.orders.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.orders.entity.Order;
import com.restaurantpos.orders.model.OrderStatus;

/**
 * Repository for Order entity with tenant filtering and optimistic locking.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 5.1, 5.9
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    /**
     * Finds all orders for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of orders
     */
    List<Order> findByTenantId(UUID tenantId);
    
    /**
     * Finds all orders for a specific site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return list of orders
     */
    List<Order> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);
    
    /**
     * Finds all orders for a specific table.
     * 
     * @param tenantId the tenant ID
     * @param tableId the table ID
     * @return list of orders
     */
    List<Order> findByTenantIdAndTableId(UUID tenantId, UUID tableId);
    
    /**
     * Finds all orders for a specific table with a given status.
     * 
     * @param tenantId the tenant ID
     * @param tableId the table ID
     * @param status the order status
     * @return list of orders
     */
    List<Order> findByTenantIdAndTableIdAndStatus(UUID tenantId, UUID tableId, OrderStatus status);
    
    /**
     * Finds all orders for a specific customer.
     * 
     * @param tenantId the tenant ID
     * @param customerId the customer ID
     * @return list of orders
     */
    List<Order> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    
    /**
     * Finds an order by ID and tenant ID (for tenant isolation).
     * 
     * @param id the order ID
     * @param tenantId the tenant ID
     * @return the order if found
     */
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds all orders for a site with a given status.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param status the order status
     * @return list of orders
     */
    List<Order> findByTenantIdAndSiteIdAndStatus(UUID tenantId, UUID siteId, OrderStatus status);
    
    /**
     * Counts the number of orders for a table with a given status.
     * 
     * @param tenantId the tenant ID
     * @param tableId the table ID
     * @param status the order status
     * @return the count of orders
     */
    long countByTenantIdAndTableIdAndStatus(UUID tenantId, UUID tableId, OrderStatus status);
}
