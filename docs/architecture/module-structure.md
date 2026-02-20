# Module Structure Documentation

## Overview

The Restaurant POS SaaS system is built as a **Spring Modulith modular monolith** with clear module boundaries and dependencies. This architecture provides the benefits of modularity (clear boundaries, independent development, testability) while maintaining the simplicity of a single deployable artifact.

## Module Inventory

The system consists of 11 modules organized by domain responsibility:

### Core Business Modules

1. **tenantprovisioning** - Tenant onboarding and multi-site configuration
2. **identityaccess** - Authentication, authorization, and user management  
3. **diningroom** - Table management and real-time status tracking
4. **catalog** - Menu structure (families, subfamilies, items)
5. **orders** - Order creation, modification, and confirmation
6. **kitchenprinting** - Print job generation and printer management
7. **paymentsbilling** - Payment processing and fiscal documents
8. **customers** - Customer data for delivery orders
9. **cashregister** - Cash session management and financial closings

### Supporting Modules

10. **common** - Cross-cutting concerns (exception handling, health checks, metrics, security)
11. **fiscalexport** - SAF-T PT fiscal export functionality

## Module Dependencies

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                     identityaccess                           │
│                  (authentication, RBAC)                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ (all modules depend on identity)
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                   tenantprovisioning                         │
│                  (tenant configuration)                      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────┐      ┌──────────────────┐
│   diningroom     │      │     catalog      │
│  (table status)  │      │  (menu items)    │
└────────┬─────────┘      └────────┬─────────┘
         │                         │
         │                         │
         └────────┬────────────────┘
                  │
         ┌────────▼─────────┐
         │      orders      │
         │ (order creation) │
         └────────┬─────────┘
                  │
         ┌────────┴────────┬──────────────────┐
         │                 │                  │
┌────────▼─────────┐ ┌────▼──────────┐ ┌────▼─────────┐
│ kitchenprinting  │ │   payments-   │ │  customers   │
│  (print jobs)    │ │    billing    │ │  (delivery)  │
└──────────────────┘ └────┬──────────┘ └──────────────┘
                          │
                   ┌──────▼──────────┐
                   │  cashregister   │
                   │ (cash tracking) │
                   └─────────────────┘

         ┌──────────────────────────────┐
         │        fiscalexport          │
         │  (depends on multiple modules)│
         └──────────────────────────────┘

         ┌──────────────────────────────┐
         │          common              │
         │   (cross-cutting concerns)   │
         └──────────────────────────────┘
```

### Dependency Rules

1. **All modules depend on identityaccess** for authentication and authorization
2. **No circular dependencies** are allowed between modules
3. **Event-driven communication** is used for loose coupling:
   - `orders` publishes `OrderConfirmed` event → `kitchenprinting` listens
   - `paymentsbilling` publishes `PaymentCompleted` event → `cashregister` listens
4. **Named interfaces** expose specific APIs for cross-module communication
5. **Repository and entity access** should be limited to module boundaries

## Module Details

### 1. Tenant Provisioning Module

**Package**: `com.restaurantpos.tenantprovisioning`

**Responsibility**: Manage tenant onboarding, configuration, and multi-site setup.

**Key Components**:
- `Tenant` entity - Tenant master data
- `Site` entity - Physical restaurant locations
- `TenantProvisioningService` - Tenant lifecycle management
- `TenantProvisioningController` - REST API for tenant operations

**Dependencies**: identityaccess

**Exposes**: Tenant and site configuration APIs

---

### 2. Identity and Access Module

**Package**: `com.restaurantpos.identityaccess`

**Responsibility**: Authentication, authorization, JWT token management, and RBAC.

**Key Components**:
- `User` entity - User accounts with roles
- `RefreshToken` entity - Token refresh mechanism
- `JwtTokenProvider` - JWT generation and validation
- `AuthenticationService` - Login/logout operations
- `AuthorizationService` - Permission checking
- `TenantContext` - Thread-local tenant context
- `TenantContextFilter` - Extract tenant from JWT
- `TenantAspect` - Enforce tenant filtering on repositories

**Dependencies**: None (foundational module)

**Exposes**: 
- `api` package - Authorization API for other modules
- Authentication and authorization services

---

### 3. Dining Room Module

**Package**: `com.restaurantpos.diningroom`

**Responsibility**: Table management, real-time status updates, table operations.

**Key Components**:
- `DiningTable` entity - Table master data with status
- `BlacklistEntry` entity - Blocked tables/cards
- `TableManagementService` - Table lifecycle and operations
- `TableController` - REST API for table operations

**Dependencies**: identityaccess

**Exposes**: Table management APIs

**Events Published**: `TableStatusChanged` (for real-time updates)

---

### 4. Catalog Module

**Package**: `com.restaurantpos.catalog`

**Responsibility**: Menu structure management (families, subfamilies, items, pricing).

**Key Components**:
- `Family` entity - Top-level menu categories
- `Subfamily` entity - Sub-categories
- `Item` entity - Menu items with pricing
- `CatalogManagementService` - Menu CRUD operations
- `CatalogController` - REST API for catalog

**Dependencies**: identityaccess

**Exposes**: 
- `api` package - Catalog query API
- Menu structure and item APIs

---

### 5. Orders Module

**Package**: `com.restaurantpos.orders`

**Responsibility**: Order creation, modification, confirmation, void operations.

**Key Components**:
- `Order` entity - Order header
- `OrderLine` entity - Order line items
- `Consumption` entity - Confirmed order tracking
- `Discount` entity - Discounts applied
- `OrderService` - Order lifecycle management
- `OrderController` - REST API for orders

**Dependencies**: identityaccess, diningroom, catalog

**Exposes**: 
- `event` package - Domain events (OrderConfirmed, OrderLineVoided)

**Events Published**: 
- `OrderConfirmed` - Triggers print job creation
- `OrderLineVoided` - Audit trail

---

### 6. Kitchen and Printing Module

**Package**: `com.restaurantpos.kitchenprinting`

**Responsibility**: Print job generation, printer management, routing logic.

**Key Components**:
- `Printer` entity - Printer configuration
- `PrintJob` entity - Print tasks
- `PrintingService` - Print job generation and processing
- `PrinterManagementService` - Printer state management
- `OrderConfirmedEventListener` - Listens to order confirmations
- `PrinterController` - REST API for printer management

**Dependencies**: identityaccess, orders::event

**Exposes**: Printer management APIs

**Events Consumed**: `OrderConfirmed` from orders module

---

### 7. Payments and Billing Module

**Package**: `com.restaurantpos.paymentsbilling`

**Responsibility**: Payment processing, bill splitting, fiscal document generation.

**Key Components**:
- `Payment` entity - Payment transactions
- `FiscalDocument` entity - Invoices and receipts
- `PaymentService` - Payment processing with idempotency
- `BillingService` - Fiscal document generation
- `PaymentController` - REST API for payments
- `BillingController` - REST API for billing

**Dependencies**: identityaccess, orders

**Exposes**: 
- `event` package - Domain events (PaymentCompleted)

**Events Published**: `PaymentCompleted` - Triggers cash movement

---

### 8. Customers Module

**Package**: `com.restaurantpos.customers`

**Responsibility**: Customer data management for delivery orders.

**Key Components**:
- `Customer` entity - Customer master data
- `CustomerService` - Customer CRUD and search
- `CustomerController` - REST API for customers

**Dependencies**: identityaccess, orders (for order history)

**Exposes**: Customer management APIs

---

### 9. Cash Register Module

**Package**: `com.restaurantpos.cashregister`

**Responsibility**: Cash session management, movements, closings, financial reporting.

**Key Components**:
- `CashRegister` entity - Register master data
- `CashSession` entity - Work sessions
- `CashMovement` entity - Cash transactions
- `CashClosing` entity - Financial closings
- `CashSessionService` - Session lifecycle
- `CashClosingService` - Closing operations
- `PaymentCompletedListener` - Listens to payment events
- `CashRegisterController` - REST API for cash operations

**Dependencies**: identityaccess, paymentsbilling::event

**Exposes**: Cash management APIs

**Events Consumed**: `PaymentCompleted` from paymentsbilling module

---

### 10. Common Module

**Package**: `com.restaurantpos.common`

**Responsibility**: Cross-cutting concerns shared across all modules.

**Key Components**:
- `GlobalExceptionHandler` - Centralized error handling
- `BusinessRuleViolationException` - Domain exceptions
- `DatabaseHealthIndicator` - Health checks
- `DiskSpaceHealthIndicator` - Health checks
- `MetricsConfiguration` - Micrometer setup
- `MetricsFilter` - Request metrics
- `SensitiveDataMasker` - Log masking
- `SqlInjectionPrevention` - Security utilities

**Dependencies**: identityaccess (for exception types)

**Exposes**: Common utilities and exception types

---

### 11. Fiscal Export Module

**Package**: `com.restaurantpos.fiscalexport`

**Responsibility**: SAF-T PT fiscal export for tax compliance.

**Key Components**:
- `SaftExportService` - XML generation and validation
- `SaftExportController` - REST API for exports

**Dependencies**: identityaccess, paymentsbilling, customers, catalog

**Exposes**: Export APIs

---

## Module Communication Patterns

### 1. Direct Service Calls

Used for synchronous operations within allowed dependencies:
- Controllers call services within the same module
- Services can call other module services if dependency is allowed

### 2. Domain Events (Asynchronous)

Used for loose coupling between modules:
- `orders` → `kitchenprinting`: OrderConfirmed event
- `paymentsbilling` → `cashregister`: PaymentCompleted event

### 3. Named Interfaces

Expose specific APIs for cross-module access:
- `identityaccess::api` - Authorization API
- `orders::event` - Order domain events

### 4. Repository Access

Repositories should only be accessed within their owning module. Cross-module data access should go through service APIs or events.

## Module Boundaries Enforcement

Spring Modulith enforces module boundaries at compile time and runtime:

1. **Package Structure**: Each module is a top-level package under `com.restaurantpos`
2. **package-info.java**: Defines module metadata and allowed dependencies
3. **Verification Tests**: `ModularityTests` validates module structure
4. **Named Interfaces**: Explicit API contracts between modules

## Testing Strategy

### Module-Level Tests

Each module has its own test suite:
- Unit tests for services and components
- Integration tests with Testcontainers
- Property-based tests for correctness properties

### Cross-Module Integration Tests

Located in `src/test/java/com/restaurantpos`:
- `CompleteOrderFlowIntegrationTest` - End-to-end order flow
- `CashSessionLifecycleIntegrationTest` - Cash operations
- `TenantIsolationIntegrationTest` - Multi-tenancy verification

### Modularity Tests

`ModularityTests` class verifies:
- No circular dependencies
- Allowed dependencies are respected
- All expected modules are present
- Named interfaces are properly exposed

## Benefits of This Architecture

1. **Clear Boundaries**: Each module has a well-defined responsibility
2. **Independent Development**: Teams can work on modules independently
3. **Testability**: Modules can be tested in isolation
4. **Maintainability**: Changes are localized to specific modules
5. **Evolvability**: Modules can be extracted to microservices if needed
6. **Single Deployment**: Simpler operations than microservices
7. **Consistency**: Shared database ensures data consistency

## Migration Path

If the system needs to scale beyond a monolith:

1. **Identify module for extraction** (e.g., kitchenprinting)
2. **Convert domain events to message queue** (e.g., RabbitMQ, Kafka)
3. **Extract module to separate service**
4. **Update API calls to REST/gRPC**
5. **Deploy independently**

The modular structure makes this migration path straightforward.

## References

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Design Document](.kiro/specs/restaurant-pos-saas/design.md)
- [Requirements Document](.kiro/specs/restaurant-pos-saas/requirements.md)
- [Architecture Decision Records](./adr/)
