package com.restaurantpos.catalog.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.catalog.api.CatalogService;
import com.restaurantpos.catalog.dto.FamilyDetails;
import com.restaurantpos.catalog.dto.ItemDetails;
import com.restaurantpos.catalog.dto.MenuStructure;
import com.restaurantpos.catalog.entity.Family;
import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.entity.Subfamily;
import com.restaurantpos.catalog.repository.FamilyRepository;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.catalog.repository.SubfamilyRepository;

/**
 * Service for managing catalog operations including families, subfamilies, and items.
 * 
 * Requirements: 4.1, 4.3
 */
@Service
@Transactional
public class CatalogManagementService implements CatalogService {
    
    private final FamilyRepository familyRepository;
    private final SubfamilyRepository subfamilyRepository;
    private final ItemRepository itemRepository;
    
    public CatalogManagementService(
            FamilyRepository familyRepository,
            SubfamilyRepository subfamilyRepository,
            ItemRepository itemRepository) {
        this.familyRepository = familyRepository;
        this.subfamilyRepository = subfamilyRepository;
        this.itemRepository = itemRepository;
    }
    
    /**
     * Creates a new family for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param familyDetails the family details
     * @return the created family
     * @throws IllegalArgumentException if family name already exists
     */
    public Family createFamily(UUID tenantId, FamilyDetails familyDetails) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        // Check if family name already exists for this tenant
        if (familyRepository.existsByTenantIdAndName(tenantId, familyDetails.name())) {
            throw new IllegalArgumentException("Family with name '" + familyDetails.name() + "' already exists");
        }
        
        Family family = new Family(tenantId, familyDetails.name(), familyDetails.displayOrder());
        return familyRepository.save(family);
    }
    
    /**
     * Creates a new item for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param itemDetails the item details
     * @return the created item
     * @throws IllegalArgumentException if subfamily doesn't exist or item name already exists
     */
    public Item createItem(UUID tenantId, ItemDetails itemDetails) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        // Verify subfamily exists and belongs to this tenant
        subfamilyRepository.findByIdAndTenantId(itemDetails.subfamilyId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Subfamily not found or does not belong to tenant"));
        
        // Check if item name already exists within this subfamily
        if (itemRepository.existsByTenantIdAndSubfamilyIdAndName(tenantId, itemDetails.subfamilyId(), itemDetails.name())) {
            throw new IllegalArgumentException("Item with name '" + itemDetails.name() + "' already exists in this subfamily");
        }
        
        Item item = new Item(
            tenantId,
            itemDetails.subfamilyId(),
            itemDetails.name(),
            itemDetails.description(),
            itemDetails.basePrice()
        );
        
        if (itemDetails.imageUrl() != null) {
            item.setImageUrl(itemDetails.imageUrl());
        }
        
        return itemRepository.save(item);
    }
    
    /**
     * Updates an existing item.
     * 
     * @param itemId the item ID
     * @param itemDetails the updated item details
     * @return the updated item
     * @throws IllegalArgumentException if item not found
     */
    public Item updateItem(UUID itemId, ItemDetails itemDetails) {
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }
        
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        
        // Update fields
        item.setName(itemDetails.name());
        if (itemDetails.description() != null) {
            item.setDescription(itemDetails.description());
        }
        item.setBasePrice(itemDetails.basePrice());
        if (itemDetails.imageUrl() != null) {
            item.setImageUrl(itemDetails.imageUrl());
        }
        
        return itemRepository.save(item);
    }
    
    /**
     * Updates the availability status of an item.
     * 
     * @param itemId the item ID
     * @param available the new availability status
     * @return the updated item
     * @throws IllegalArgumentException if item not found
     */
    public Item updateItemAvailability(UUID itemId, Boolean available) {
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }
        if (available == null) {
            throw new IllegalArgumentException("Availability status cannot be null");
        }
        
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        
        item.setAvailable(available);
        return itemRepository.save(item);
    }
    
    /**
     * Retrieves the complete menu structure for a tenant.
     * Note: siteId parameter is included for future site-specific menu support,
     * but currently returns the tenant-wide menu.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID (currently unused, for future expansion)
     * @return the complete menu hierarchy
     */
    @Transactional(readOnly = true)
    public MenuStructure getMenuStructure(UUID tenantId, UUID siteId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        // Get all active families ordered by display order
        List<Family> families = familyRepository.findByTenantIdAndActiveOrderByDisplayOrder(tenantId, true);
        
        List<MenuStructure.FamilyNode> familyNodes = families.stream()
            .map(family -> buildFamilyNode(tenantId, family))
            .collect(Collectors.toList());
        
        return new MenuStructure(familyNodes);
    }
    
    private MenuStructure.FamilyNode buildFamilyNode(UUID tenantId, Family family) {
        // Get all active subfamilies for this family
        List<Subfamily> subfamilies = subfamilyRepository
            .findByTenantIdAndFamilyIdAndActiveOrderByDisplayOrder(tenantId, family.getId(), true);
        
        List<MenuStructure.SubfamilyNode> subfamilyNodes = subfamilies.stream()
            .map(subfamily -> buildSubfamilyNode(tenantId, subfamily))
            .collect(Collectors.toList());
        
        return new MenuStructure.FamilyNode(
            family.getId(),
            family.getName(),
            family.getDisplayOrder(),
            family.getActive(),
            subfamilyNodes
        );
    }
    
    private MenuStructure.SubfamilyNode buildSubfamilyNode(UUID tenantId, Subfamily subfamily) {
        // Get all available items for this subfamily
        List<Item> items = itemRepository
            .findByTenantIdAndSubfamilyIdAndAvailable(tenantId, subfamily.getId(), true);
        
        List<MenuStructure.ItemNode> itemNodes = items.stream()
            .map(item -> new MenuStructure.ItemNode(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getBasePrice(),
                item.getAvailable(),
                item.getImageUrl()
            ))
            .collect(Collectors.toList());
        
        return new MenuStructure.SubfamilyNode(
            subfamily.getId(),
            subfamily.getName(),
            subfamily.getDisplayOrder(),
            subfamily.getActive(),
            itemNodes
        );
    }
    
    /**
     * Gets an item by ID with tenant isolation.
     * This method is part of the public API for cross-module communication.
     * 
     * @param itemId the item ID
     * @param tenantId the tenant ID
     * @return the item info if found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogService.ItemInfo> getItem(UUID itemId, UUID tenantId) {
        return itemRepository.findByIdAndTenantId(itemId, tenantId)
            .map(item -> new CatalogService.ItemInfo(
                item.getId(),
                item.getTenantId(),
                item.getName(),
                item.getBasePrice(),
                item.isAvailable()
            ));
    }
}
