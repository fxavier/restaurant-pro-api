package com.restaurantpos.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.catalog.entity.Item;

/**
 * Repository for Item entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 4.1, 4.2
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
    
    /**
     * Finds all items for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of items
     */
    List<Item> findByTenantId(UUID tenantId);
    
    /**
     * Finds all items for a specific subfamily.
     * 
     * @param tenantId the tenant ID
     * @param subfamilyId the subfamily ID
     * @return list of items
     */
    List<Item> findByTenantIdAndSubfamilyId(UUID tenantId, UUID subfamilyId);
    
    /**
     * Finds all available items for a subfamily.
     * 
     * @param tenantId the tenant ID
     * @param subfamilyId the subfamily ID
     * @param available the availability status
     * @return list of available items
     */
    List<Item> findByTenantIdAndSubfamilyIdAndAvailable(UUID tenantId, UUID subfamilyId, Boolean available);
    
    /**
     * Finds an item by ID and tenant ID (for tenant isolation).
     * 
     * @param id the item ID
     * @param tenantId the tenant ID
     * @return the item if found
     */
    Optional<Item> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds an item by name within a subfamily.
     * 
     * @param tenantId the tenant ID
     * @param subfamilyId the subfamily ID
     * @param name the item name
     * @return the item if found
     */
    Optional<Item> findByTenantIdAndSubfamilyIdAndName(UUID tenantId, UUID subfamilyId, String name);
    
    /**
     * Finds all items by IDs for a tenant (for quick pages).
     * 
     * @param tenantId the tenant ID
     * @param ids the list of item IDs
     * @return list of items
     */
    List<Item> findByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);
    
    /**
     * Checks if an item name exists within a subfamily.
     * 
     * @param tenantId the tenant ID
     * @param subfamilyId the subfamily ID
     * @param name the item name
     * @return true if the item name exists
     */
    boolean existsByTenantIdAndSubfamilyIdAndName(UUID tenantId, UUID subfamilyId, String name);
    
    /**
     * Counts the number of items for a subfamily.
     * 
     * @param tenantId the tenant ID
     * @param subfamilyId the subfamily ID
     * @return the count of items
     */
    long countByTenantIdAndSubfamilyId(UUID tenantId, UUID subfamilyId);
    
    /**
     * Counts the number of available items for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param available the availability status
     * @return the count of available items
     */
    long countByTenantIdAndAvailable(UUID tenantId, Boolean available);
}
