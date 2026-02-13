package com.restaurantpos.orders.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.orders.entity.Discount;

/**
 * Repository for Discount entity.
 * 
 * Requirements: 5.7
 */
@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    
    /**
     * Finds all discounts for a specific order.
     * 
     * @param orderId the order ID
     * @return list of discounts
     */
    List<Discount> findByOrderId(UUID orderId);
    
    /**
     * Finds all discounts for a specific order line.
     * 
     * @param orderLineId the order line ID
     * @return list of discounts
     */
    List<Discount> findByOrderLineId(UUID orderLineId);
    
    /**
     * Finds all order-level discounts (not line-specific).
     * 
     * @param orderId the order ID
     * @return list of order-level discounts
     */
    List<Discount> findByOrderIdAndOrderLineIdIsNull(UUID orderId);
    
    /**
     * Finds all discounts applied by a specific user.
     * 
     * @param appliedBy the user ID who applied the discount
     * @return list of discounts
     */
    List<Discount> findByAppliedBy(UUID appliedBy);
}
