package com.restaurantpos.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.restaurantpos.catalog.dto.FamilyDetails;
import com.restaurantpos.catalog.dto.ItemDetails;
import com.restaurantpos.catalog.dto.MenuStructure;
import com.restaurantpos.catalog.entity.Family;
import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.entity.Subfamily;
import com.restaurantpos.catalog.repository.FamilyRepository;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.catalog.repository.SubfamilyRepository;
import com.restaurantpos.catalog.service.CatalogManagementService;
import com.restaurantpos.identityaccess.tenant.TenantContext;

/**
 * Unit tests for CatalogManagementService.
 * 
 * Requirements: 4.1, 4.3
 */
@SpringBootTest
@Testcontainers
@Transactional
class CatalogManagementServiceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("restaurant_pos_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private CatalogManagementService catalogService;
    
    @Autowired
    private FamilyRepository familyRepository;
    
    @Autowired
    private SubfamilyRepository subfamilyRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    private UUID tenantId;
    
    @BeforeEach
    void setUp() {
        // Create a tenant using direct SQL to bypass tenant context requirement
        tenantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, subscription_plan, status, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            tenantId, "Test Restaurant", "BASIC", "ACTIVE"
        );
        
        TenantContext.setTenantId(tenantId);
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    void createFamily_withValidDetails_createsFamily() {
        // Given
        FamilyDetails details = new FamilyDetails("Beverages", 1);
        
        // When
        Family family = catalogService.createFamily(tenantId, details);
        
        // Then
        assertThat(family).isNotNull();
        assertThat(family.getId()).isNotNull();
        assertThat(family.getTenantId()).isEqualTo(tenantId);
        assertThat(family.getName()).isEqualTo("Beverages");
        assertThat(family.getDisplayOrder()).isEqualTo(1);
        assertThat(family.getActive()).isTrue();
    }
    
    @Test
    void createFamily_withDuplicateName_throwsException() {
        // Given
        FamilyDetails details = new FamilyDetails("Beverages", 1);
        catalogService.createFamily(tenantId, details);
        
        // When/Then
        assertThatThrownBy(() -> catalogService.createFamily(tenantId, details))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    void createFamily_withNullTenantId_throwsException() {
        // Given
        FamilyDetails details = new FamilyDetails("Beverages", 1);
        
        // When/Then
        assertThatThrownBy(() -> catalogService.createFamily(null, details))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
    }
    
    @Test
    void createItem_withValidDetails_createsItem() {
        // Given
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        
        ItemDetails details = new ItemDetails(
            subfamily.getId(),
            "Espresso",
            "Strong coffee",
            new BigDecimal("2.50"),
            null
        );
        
        // When
        Item item = catalogService.createItem(tenantId, details);
        
        // Then
        assertThat(item).isNotNull();
        assertThat(item.getId()).isNotNull();
        assertThat(item.getTenantId()).isEqualTo(tenantId);
        assertThat(item.getSubfamilyId()).isEqualTo(subfamily.getId());
        assertThat(item.getName()).isEqualTo("Espresso");
        assertThat(item.getDescription()).isEqualTo("Strong coffee");
        assertThat(item.getBasePrice()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(item.getAvailable()).isTrue();
    }
    
    @Test
    void createItem_withImageUrl_createsItemWithImage() {
        // Given
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        
        ItemDetails details = new ItemDetails(
            subfamily.getId(),
            "Espresso",
            "Strong coffee",
            new BigDecimal("2.50"),
            "https://example.com/espresso.jpg"
        );
        
        // When
        Item item = catalogService.createItem(tenantId, details);
        
        // Then
        assertThat(item.getImageUrl()).isEqualTo("https://example.com/espresso.jpg");
    }
    
    @Test
    void createItem_withNonExistentSubfamily_throwsException() {
        // Given
        UUID nonExistentSubfamilyId = UUID.randomUUID();
        
        ItemDetails details = new ItemDetails(
            nonExistentSubfamilyId,
            "Espresso",
            "Strong coffee",
            new BigDecimal("2.50"),
            null
        );
        
        // When/Then
        assertThatThrownBy(() -> catalogService.createItem(tenantId, details))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Subfamily not found");
    }
    
    @Test
    void createItem_withDuplicateNameInSubfamily_throwsException() {
        // Given
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        
        ItemDetails details = new ItemDetails(
            subfamily.getId(),
            "Espresso",
            "Strong coffee",
            new BigDecimal("2.50"),
            null
        );
        catalogService.createItem(tenantId, details);
        
        // When/Then
        assertThatThrownBy(() -> catalogService.createItem(tenantId, details))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    void createItem_withNullTenantId_throwsException() {
        // Given
        ItemDetails details = new ItemDetails(
            UUID.randomUUID(),
            "Espresso",
            "Strong coffee",
            new BigDecimal("2.50"),
            null
        );
        
        // When/Then
        assertThatThrownBy(() -> catalogService.createItem(null, details))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
    }
    
    @Test
    void updateItemAvailability_withValidItem_updatesAvailability() {
        // Given
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        Item item = itemRepository.save(new Item(tenantId, subfamily.getId(), "Espresso", "Strong coffee", new BigDecimal("2.50")));
        
        assertThat(item.getAvailable()).isTrue();
        
        // When
        Item updatedItem = catalogService.updateItemAvailability(item.getId(), false);
        
        // Then
        assertThat(updatedItem.getAvailable()).isFalse();
    }
    
    @Test
    void updateItemAvailability_withNonExistentItem_throwsException() {
        // Given
        UUID nonExistentItemId = UUID.randomUUID();
        
        // When/Then
        assertThatThrownBy(() -> catalogService.updateItemAvailability(nonExistentItemId, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Item not found");
    }
    
    @Test
    void updateItemAvailability_withNullItemId_throwsException() {
        // When/Then
        assertThatThrownBy(() -> catalogService.updateItemAvailability(null, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Item ID cannot be null");
    }
    
    @Test
    void updateItemAvailability_withNullAvailability_throwsException() {
        // Given
        UUID itemId = UUID.randomUUID();
        
        // When/Then
        assertThatThrownBy(() -> catalogService.updateItemAvailability(itemId, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Availability status cannot be null");
    }
    
    @Test
    void getMenuStructure_withCompleteHierarchy_returnsFullMenu() {
        // Given
        UUID siteId = UUID.randomUUID();
        
        // Create family
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        
        // Create subfamily
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        
        // Create items
        itemRepository.save(new Item(tenantId, subfamily.getId(), "Espresso", "Strong coffee", new BigDecimal("2.50")));
        itemRepository.save(new Item(tenantId, subfamily.getId(), "Cappuccino", "Coffee with milk", new BigDecimal("3.50")));
        
        // When
        MenuStructure menu = catalogService.getMenuStructure(tenantId, siteId);
        
        // Then
        assertThat(menu.families()).hasSize(1);
        
        MenuStructure.FamilyNode familyNode = menu.families().get(0);
        assertThat(familyNode.id()).isEqualTo(family.getId());
        assertThat(familyNode.name()).isEqualTo("Beverages");
        assertThat(familyNode.subfamilies()).hasSize(1);
        
        MenuStructure.SubfamilyNode subfamilyNode = familyNode.subfamilies().get(0);
        assertThat(subfamilyNode.id()).isEqualTo(subfamily.getId());
        assertThat(subfamilyNode.name()).isEqualTo("Hot Drinks");
        assertThat(subfamilyNode.items()).hasSize(2);
        
        assertThat(subfamilyNode.items())
            .extracting(MenuStructure.ItemNode::name)
            .containsExactlyInAnyOrder("Espresso", "Cappuccino");
    }
    
    @Test
    void getMenuStructure_withInactiveFamilies_excludesInactiveFamilies() {
        // Given
        UUID siteId = UUID.randomUUID();
        
        // Create active family
        familyRepository.save(new Family(tenantId, "Beverages", 1));
        
        // Create inactive family
        Family inactiveFamily = familyRepository.save(new Family(tenantId, "Desserts", 2));
        inactiveFamily.setActive(false);
        familyRepository.save(inactiveFamily);
        
        // When
        MenuStructure menu = catalogService.getMenuStructure(tenantId, siteId);
        
        // Then
        assertThat(menu.families()).hasSize(1);
        assertThat(menu.families().get(0).name()).isEqualTo("Beverages");
    }
    
    @Test
    void getMenuStructure_withUnavailableItems_excludesUnavailableItems() {
        // Given
        UUID siteId = UUID.randomUUID();
        
        Family family = familyRepository.save(new Family(tenantId, "Beverages", 1));
        Subfamily subfamily = subfamilyRepository.save(new Subfamily(tenantId, family.getId(), "Hot Drinks", 1));
        
        // Create available item
        itemRepository.save(new Item(tenantId, subfamily.getId(), "Espresso", "Strong coffee", new BigDecimal("2.50")));
        
        // Create unavailable item
        Item unavailableItem = itemRepository.save(new Item(tenantId, subfamily.getId(), "Latte", "Coffee with milk", new BigDecimal("3.50")));
        unavailableItem.setAvailable(false);
        itemRepository.save(unavailableItem);
        
        // When
        MenuStructure menu = catalogService.getMenuStructure(tenantId, siteId);
        
        // Then
        MenuStructure.SubfamilyNode subfamilyNode = menu.families().get(0).subfamilies().get(0);
        assertThat(subfamilyNode.items()).hasSize(1);
        assertThat(subfamilyNode.items().get(0).name()).isEqualTo("Espresso");
    }
    
    @Test
    void getMenuStructure_withEmptyMenu_returnsEmptyList() {
        // Given
        UUID siteId = UUID.randomUUID();
        
        // When
        MenuStructure menu = catalogService.getMenuStructure(tenantId, siteId);
        
        // Then
        assertThat(menu.families()).isEmpty();
    }
    
    @Test
    void getMenuStructure_withNullTenantId_throwsException() {
        // When/Then
        assertThatThrownBy(() -> catalogService.getMenuStructure(null, UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
    }
    
    @Test
    void getMenuStructure_withMultipleFamilies_ordersCorrectly() {
        // Given
        UUID siteId = UUID.randomUUID();
        
        // Create families with different display orders
        familyRepository.save(new Family(tenantId, "Desserts", 3));
        familyRepository.save(new Family(tenantId, "Beverages", 1));
        familyRepository.save(new Family(tenantId, "Main Courses", 2));
        
        // When
        MenuStructure menu = catalogService.getMenuStructure(tenantId, siteId);
        
        // Then
        assertThat(menu.families()).hasSize(3);
        assertThat(menu.families())
            .extracting(MenuStructure.FamilyNode::name)
            .containsExactly("Beverages", "Main Courses", "Desserts");
    }
}
