package com.restaurantpos;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Tests to verify Spring Modulith module structure and boundaries.
 * 
 * These tests ensure:
 * - Module dependencies match the design document
 * - No circular dependencies exist
 * - Module boundaries are properly enforced
 * - All expected modules are present
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(RestaurantPosApplication.class);

    /**
     * Verifies that the module structure is valid according to Spring Modulith rules.
     * This includes:
     * - No circular dependencies
     * - Allowed dependencies are respected
     * - Named interfaces are properly defined
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * Verifies that all expected modules are present in the application.
     * According to the design document, we should have 9 core modules plus
     * 2 additional modules (common and fiscalexport):
     * 1. tenant-provisioning
     * 2. identity-access (identityaccess)
     * 3. dining-room (diningroom)
     * 4. catalog
     * 5. orders
     * 6. kitchen-printing (kitchenprinting)
     * 7. payments-billing (paymentsbilling)
     * 8. customers
     * 9. cash-register (cashregister)
     * 10. common (cross-cutting concerns)
     * 11. fiscalexport (SAF-T export functionality)
     */
    @Test
    void hasExpectedModules() {
        var moduleNames = modules.stream()
            .map(module -> module.getName())
            .toList();

        assertThat(moduleNames)
            .as("All expected modules should be present")
            .containsExactlyInAnyOrder(
                "tenantprovisioning",
                "identityaccess",
                "diningroom",
                "catalog",
                "orders",
                "kitchenprinting",
                "paymentsbilling",
                "customers",
                "cashregister",
                "common",
                "fiscalexport"
            );
    }

    /**
     * Verifies that there are no circular dependencies between modules.
     * Circular dependencies violate the modular architecture principles.
     * 
     * The verify() method already checks for circular dependencies,
     * so this test primarily documents the requirement.
     */
    @Test
    void hasNoCircularDependencies() {
        // Spring Modulith's verify() method checks for circular dependencies
        modules.verify();
    }

    /**
     * Verifies that the orders module properly exposes its event interface.
     * According to the design, orders module should expose an "event" named interface
     * for OrderConfirmed events that other modules can listen to.
     */
    @Test
    void ordersModuleExposesEventInterface() {
        var ordersModule = modules.getModuleByName("orders")
            .orElseThrow(() -> new AssertionError("Orders module not found"));

        assertThat(ordersModule.getNamedInterfaces())
            .as("Orders module should expose 'event' named interface")
            .anyMatch(namedInterface -> namedInterface.getName().equals("event"));
    }

    /**
     * Verifies that the catalog module has proper structure.
     * According to the design, catalog module should expose an API for other modules.
     */
    @Test
    void catalogModuleHasProperStructure() {
        var catalogModule = modules.getModuleByName("catalog")
            .orElseThrow(() -> new AssertionError("Catalog module not found"));

        // Catalog module exists and is properly configured
        assertThat(catalogModule).isNotNull();
        assertThat(catalogModule.getDisplayName()).isEqualTo("Catalog");
    }

    /**
     * Verifies that the identity-access module has proper structure.
     * According to the design, all modules depend on identity-access for authentication/authorization.
     */
    @Test
    void identityAccessModuleHasProperStructure() {
        var identityAccessModule = modules.getModuleByName("identityaccess")
            .orElseThrow(() -> new AssertionError("Identity Access module not found"));

        // Identity Access module exists and is properly configured
        assertThat(identityAccessModule).isNotNull();
        assertThat(identityAccessModule.getDisplayName()).isEqualTo("Identity and Access");
    }

    /**
     * Verifies that the kitchen-printing module exists and is properly configured.
     * According to the design, kitchen-printing listens to OrderConfirmed events.
     */
    @Test
    void kitchenPrintingModuleExists() {
        var kitchenPrintingModule = modules.getModuleByName("kitchenprinting")
            .orElseThrow(() -> new AssertionError("Kitchen Printing module not found"));

        assertThat(kitchenPrintingModule).isNotNull();
        assertThat(kitchenPrintingModule.getDisplayName()).isEqualTo("Kitchen and Printing");
    }

    /**
     * Verifies that the cash-register module exists and is properly configured.
     * According to the design, cash-register listens to PaymentCompleted events.
     */
    @Test
    void cashRegisterModuleExists() {
        var cashRegisterModule = modules.getModuleByName("cashregister")
            .orElseThrow(() -> new AssertionError("Cash Register module not found"));

        assertThat(cashRegisterModule).isNotNull();
        assertThat(cashRegisterModule.getDisplayName()).isEqualTo("Cash Register");
    }

    /**
     * Verifies that the orders module exists and is properly configured.
     * According to the design, orders depends on catalog and dining-room modules.
     */
    @Test
    void ordersModuleExists() {
        var ordersModule = modules.getModuleByName("orders")
            .orElseThrow(() -> new AssertionError("Orders module not found"));

        assertThat(ordersModule).isNotNull();
        assertThat(ordersModule.getDisplayName()).isEqualTo("Orders");
    }

    /**
     * Verifies that the dining-room module exists and is properly configured.
     */
    @Test
    void diningRoomModuleExists() {
        var diningRoomModule = modules.getModuleByName("diningroom")
            .orElseThrow(() -> new AssertionError("Dining Room module not found"));

        assertThat(diningRoomModule).isNotNull();
        assertThat(diningRoomModule.getDisplayName()).isEqualTo("Dining Room");
    }

    /**
     * Verifies that the payments-billing module exists and is properly configured.
     * According to the design, payments are associated with orders.
     */
    @Test
    void paymentsBillingModuleExists() {
        var paymentsBillingModule = modules.getModuleByName("paymentsbilling")
            .orElseThrow(() -> new AssertionError("Payments Billing module not found"));

        assertThat(paymentsBillingModule).isNotNull();
        assertThat(paymentsBillingModule.getDisplayName()).isEqualTo("Payments and Billing");
    }

    /**
     * Verifies that the customers module exists and is properly configured.
     * According to the design, customers module is independent and used by orders.
     */
    @Test
    void customersModuleExists() {
        var customersModule = modules.getModuleByName("customers")
            .orElseThrow(() -> new AssertionError("Customers module not found"));

        assertThat(customersModule).isNotNull();
        assertThat(customersModule.getDisplayName()).isEqualTo("Customers");
    }

    /**
     * Verifies that the tenant-provisioning module exists and is properly configured.
     */
    @Test
    void tenantProvisioningModuleExists() {
        var tenantProvisioningModule = modules.getModuleByName("tenantprovisioning")
            .orElseThrow(() -> new AssertionError("Tenant Provisioning module not found"));

        assertThat(tenantProvisioningModule).isNotNull();
        assertThat(tenantProvisioningModule.getDisplayName()).isEqualTo("Tenant Provisioning");
    }

    /**
     * Generates module documentation including:
     * - Module dependency diagrams
     * - PlantUML diagrams for each module
     * - Component documentation
     */
    @Test
    void createModuleDocumentation() {
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
