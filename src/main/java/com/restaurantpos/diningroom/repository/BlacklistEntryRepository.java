package com.restaurantpos.diningroom.repository;

import com.restaurantpos.diningroom.entity.BlacklistEntry;
import com.restaurantpos.diningroom.model.EntityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for BlacklistEntry entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 3.6
 */
@Repository
public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, UUID> {
    
    /**
     * Finds all blacklist entries for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of blacklist entries
     */
    List<BlacklistEntry> findByTenantId(UUID tenantId);
    
    /**
     * Finds all blacklist entries for a tenant by entity type.
     * 
     * @param tenantId the tenant ID
     * @param entityType the entity type (TABLE or CARD)
     * @return list of blacklist entries
     */
    List<BlacklistEntry> findByTenantIdAndEntityType(UUID tenantId, EntityType entityType);
    
    /**
     * Finds a blacklist entry by tenant, entity type, and entity value.
     * 
     * @param tenantId the tenant ID
     * @param entityType the entity type
     * @param entityValue the entity value (table ID or card last four)
     * @return the blacklist entry if found
     */
    Optional<BlacklistEntry> findByTenantIdAndEntityTypeAndEntityValue(UUID tenantId, EntityType entityType, String entityValue);
    
    /**
     * Checks if an entity is blacklisted.
     * 
     * @param tenantId the tenant ID
     * @param entityType the entity type
     * @param entityValue the entity value
     * @return true if the entity is blacklisted
     */
    boolean existsByTenantIdAndEntityTypeAndEntityValue(UUID tenantId, EntityType entityType, String entityValue);
    
    /**
     * Deletes a blacklist entry by tenant, entity type, and entity value.
     * 
     * @param tenantId the tenant ID
     * @param entityType the entity type
     * @param entityValue the entity value
     */
    void deleteByTenantIdAndEntityTypeAndEntityValue(UUID tenantId, EntityType entityType, String entityValue);
    
    /**
     * Counts the number of blacklist entries for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return the count of blacklist entries
     */
    long countByTenantId(UUID tenantId);
}
