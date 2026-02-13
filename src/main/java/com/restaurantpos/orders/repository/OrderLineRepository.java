package com.restaurantpos.orders.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.orders.entity.OrderLine;
import com.restaurantpos.orders.model.OrderLineStatus;

/**
 * Repository for OrderLine entity with optimistic locking.
 * 
 * Requirements: 5.1, 5.2, 5.9
 */
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
    
    /**
     * Finds all order lines for a specific order.
     * 
     * @param orderId the order ID
     * @return list of order lines
     */
    List<OrderLine> findByOrderId(UUID orderId);
    
    /**
     * Finds all order lines for a specific order with a given status.
     * 
     * @param orderId the order ID
     * @param status the order line status
     * @return list of order lines
     */
    List<OrderLine> findByOrderIdAndStatus(UUID orderId, OrderLineStatus status);
    
    /**
     * Finds an order line by ID.
     * 
     * @param id the order line ID
     * @return the order line if found
     */
    Optional<OrderLine> findById(UUID id);
    
    /**
     * Finds all order lines for a specific item across all orders.
     * 
     * @param itemId the item ID
     * @return list of order lines
     */
    List<OrderLine> findByItemId(UUID itemId);
    
    /**
     * Counts the number of order lines for an order with a given status.
     * 
     * @param orderId the order ID
     * @param status the order line status
     * @return the count of order lines
     */
    long countByOrderIdAndStatus(UUID orderId, OrderLineStatus status);
}
