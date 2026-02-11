# Design Document: Restaurant POS SaaS

## Overview

This design specifies a production-ready SaaS Multi-tenant Restaurant POS system built as a modular monolith using Spring Modulith architecture. The system handles real-time table management, order processing, kitchen coordination, payment handling, and cash register operations across multiple restaurant tenants with complete data isolation.

### Architecture Principles

- **Modular Monolith**: Single deployable application with clear module boundaries enforced by Spring Modulith
- **Multi-Tenancy**: Single database, shared schema, tenant_id filtering with optional PostgreSQL RLS
- **Concurrency**: Optimistic locking on critical entities to handle multi-terminal operations
- **Transactions**: Each use case service method wrapped in @Transactional
- **Security**: JWT-based authentication with role-based access control
- **Observability**: Structured logging, Micrometer metrics, health checks
- **Data Integrity**: Database constraints, unique indexes, audit trails

### Technology Stack

- Java 21
- Spring Boot 3.x
- Spring Modulith (module boundaries and events)
- PostgreSQL 15+ (with optional RLS)
- Spring Security + JWT
- Spring Data JPA + Hibernate
- Flyway (database migrations)
- Testcontainers (integration testing)
- Micrometer (metrics)

## Architecture

### Module Structure

The system is organized into 9 modules following domain-driven design principles:

1. **tenant-provisioning**: Tenant onboarding and configuration
2. **identity-access**: Authentication, authorization, user management
3. **dining-room**: Table management and real-time status
4. **catalog**: Menu structure (families, subfamilies, items)
5. **orders**: Order creation, modification, confirmation
6. **kitchen-printing**: Print job generation and printer management
7. **payments-billing**: Payment processing and fiscal documents
8. **customers**: Customer data for delivery orders
9. **cash-register**: Cash session management and financial closings


### Module Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                     identity-access                          │
│                  (authentication, RBAC)                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ (all modules depend on identity)
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                   tenant-provisioning                        │
│                  (tenant configuration)                      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────┐      ┌──────────────────┐
│   dining-room    │      │     catalog      │
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
│ kitchen-printing │ │   payments-   │ │  customers   │
│  (print jobs)    │ │    billing    │ │  (delivery)  │
└──────────────────┘ └────┬──────────┘ └──────────────┘
                          │
                   ┌──────▼──────────┐
                   │  cash-register  │
                   │ (cash tracking) │
                   └─────────────────┘
```

**Dependency Rules:**
- All modules depend on identity-access for authentication/authorization
- orders depends on dining-room, catalog
- kitchen-printing depends on orders (listens to OrderConfirmed event)
- payments-billing depends on orders
- cash-register depends on payments-billing (listens to PaymentCompleted event)
- customers is independent (used by orders for delivery)
- No circular dependencies allowed
- Cross-module communication via application services or domain events


## Components and Interfaces

### Module 1: Tenant Provisioning

**Responsibility**: Manage tenant onboarding, configuration, and site setup.

**Core Entities:**
- `Tenant`: id, name, subscription_plan, status, created_at
- `Site`: id, tenant_id, name, address, timezone, settings
- `TenantConfiguration`: id, tenant_id, feature_flags, limits

**Application Services:**
- `TenantProvisioningService`:
  - `provisionTenant(name, plan)`: Create tenant with initial configuration
  - `createSite(tenantId, siteDetails)`: Add new site to tenant
  - `updateTenantSettings(tenantId, settings)`: Modify tenant configuration

**REST Controllers:**
- `POST /api/tenants`: Create new tenant (admin only)
- `GET /api/tenants/{id}`: Get tenant details
- `POST /api/tenants/{id}/sites`: Create site
- `PUT /api/tenants/{id}/settings`: Update settings

**Domain Events:** None

---

### Module 2: Identity and Access

**Responsibility**: Authentication, authorization, user management, JWT token handling.

**Core Entities:**
- `User`: id, tenant_id, username, password_hash, email, role, status, version
- `RefreshToken`: id, user_id, token_hash, expires_at, revoked
- `Permission`: id, code, description
- `Role`: id, name, permissions (many-to-many)

**Application Services:**
- `AuthenticationService`:
  - `login(username, password)`: Authenticate and issue JWT tokens
  - `refreshToken(refreshToken)`: Issue new access token
  - `logout(refreshToken)`: Revoke refresh token
- `UserManagementService`:
  - `createUser(tenantId, userDetails)`: Create new user
  - `updateUser(userId, updates)`: Modify user details
  - `assignRole(userId, role)`: Change user role
- `AuthorizationService`:
  - `hasPermission(userId, permission)`: Check if user has permission
  - `getTenantContext()`: Get current request tenant

**REST Controllers:**
- `POST /api/auth/login`: Authenticate user
- `POST /api/auth/refresh`: Refresh access token
- `POST /api/auth/logout`: Logout user
- `GET /api/users`: List users (filtered by tenant)
- `POST /api/users`: Create user
- `PUT /api/users/{id}`: Update user

**Domain Events:** None


---

### Module 3: Dining Room

**Responsibility**: Table management, real-time status updates, table operations.

**Core Entities:**
- `DiningTable`: id, tenant_id, site_id, table_number, area, status, capacity, version
  - Status: AVAILABLE, OCCUPIED, RESERVED, BLOCKED
- `TableArea`: id, tenant_id, site_id, name, display_order
- `BlacklistEntry`: id, tenant_id, entity_type, entity_value, reason, blocked_at

**Application Services:**
- `TableManagementService`:
  - `getTableMap(siteId)`: Get all tables with current status
  - `openTable(tableId)`: Mark table as occupied
  - `closeTable(tableId)`: Mark table as available
  - `transferTable(fromTableId, toTableId)`: Move orders between tables
  - `blockTable(tableId, reason)`: Add table to blacklist
  - `unblockTable(tableId)`: Remove table from blacklist
- `TableStatusService`:
  - `updateTableStatus(tableId, status)`: Change table state
  - `isTableBlocked(tableId)`: Check blacklist status

**REST Controllers:**
- `GET /api/tables`: Get table map for site
- `POST /api/tables/{id}/open`: Open table
- `POST /api/tables/{id}/close`: Close table
- `POST /api/tables/{id}/transfer`: Transfer to another table
- `POST /api/tables/{id}/block`: Block table
- `DELETE /api/tables/{id}/block`: Unblock table

**Domain Events:**
- `TableStatusChanged`: Emitted when table status changes (for real-time updates)


---

### Module 4: Catalog

**Responsibility**: Menu structure management (families, subfamilies, items, pricing).

**Core Entities:**
- `Family`: id, tenant_id, name, display_order, active
- `Subfamily`: id, tenant_id, family_id, name, display_order, active
- `Item`: id, tenant_id, subfamily_id, name, description, base_price, available, image_url
- `ItemModifier`: id, item_id, name, price_adjustment, modifier_type
- `QuickPage`: id, tenant_id, site_id, name, item_ids (array)
- `PriceLevel`: id, tenant_id, name, active_hours

**Application Services:**
- `CatalogManagementService`:
  - `createFamily(tenantId, familyDetails)`: Add menu family
  - `createItem(tenantId, itemDetails)`: Add menu item
  - `updateItemAvailability(itemId, available)`: Mark item available/unavailable
  - `getMenuStructure(tenantId, siteId)`: Get full menu hierarchy
- `QuickPageService`:
  - `createQuickPage(siteId, name, itemIds)`: Create quick access page
  - `getQuickPages(siteId)`: Get all quick pages for site

**REST Controllers:**
- `GET /api/catalog/menu`: Get menu structure
- `POST /api/catalog/families`: Create family
- `POST /api/catalog/items`: Create item
- `PUT /api/catalog/items/{id}`: Update item
- `PUT /api/catalog/items/{id}/availability`: Toggle availability
- `GET /api/catalog/quick-pages`: Get quick pages
- `POST /api/catalog/quick-pages`: Create quick page

**Domain Events:** None


---

### Module 5: Orders

**Responsibility**: Order creation, modification, confirmation, void operations.

**Core Entities:**
- `Order`: id, tenant_id, site_id, table_id, order_type, status, total_amount, created_at, version
  - OrderType: DINE_IN, DELIVERY, TAKEOUT
  - Status: OPEN, CONFIRMED, PAID, CLOSED, VOIDED
- `OrderLine`: id, order_id, item_id, quantity, unit_price, modifiers, notes, status, version
  - Status: PENDING, CONFIRMED, VOIDED
- `Consumption`: id, tenant_id, order_line_id, quantity, confirmed_at, voided_at
- `Discount`: id, order_id, order_line_id, type, amount, reason, applied_by
- `Offer`: id, tenant_id, name, rule_type, discount_value, active

**Application Services:**
- `OrderService`:
  - `createOrder(tableId, orderType)`: Create new order
  - `addOrderLine(orderId, itemId, quantity, modifiers)`: Add item to order
  - `updateOrderLine(orderLineId, quantity, modifiers)`: Modify order line
  - `confirmOrder(orderId)`: Confirm order (Pedir) - creates consumptions
  - `voidOrderLine(orderLineId, reason, recordWaste)`: Cancel order line
  - `applyDiscount(orderId, discountDetails)`: Apply discount
  - `getOrdersByTable(tableId)`: Get all orders for table
- `OrderTransferService`:
  - `transferOrder(orderId, toTableId)`: Move order to different table

**REST Controllers:**
- `POST /api/orders`: Create order
- `GET /api/orders/{id}`: Get order details
- `POST /api/orders/{id}/lines`: Add order line
- `PUT /api/orders/{id}/lines/{lineId}`: Update order line
- `POST /api/orders/{id}/confirm`: Confirm order
- `POST /api/orders/{id}/lines/{lineId}/void`: Void order line
- `POST /api/orders/{id}/discounts`: Apply discount
- `GET /api/tables/{tableId}/orders`: Get orders for table

**Domain Events:**
- `OrderConfirmed`: Emitted when order is confirmed (triggers print jobs)
- `OrderLineVoided`: Emitted when order line is voided


---

### Module 6: Kitchen and Printing

**Responsibility**: Print job generation, printer management, routing logic.

**Core Entities:**
- `Printer`: id, tenant_id, site_id, name, ip_address, zone, status, redirect_to_printer_id
  - Status: NORMAL, WAIT, IGNORE, REDIRECT
- `Zone`: id, tenant_id, site_id, name, description
- `PrintJob`: id, tenant_id, order_id, printer_id, content, status, dedupe_key, created_at
  - Status: PENDING, PRINTED, FAILED, SKIPPED
- `PrinterZoneMapping`: printer_id, zone_id, item_category

**Application Services:**
- `PrintingService`:
  - `createPrintJobs(orderId)`: Generate print jobs for confirmed order
  - `reprintOrder(orderId, printerId)`: Manually reprint order
  - `processPrintJob(printJobId)`: Send job to printer
- `PrinterManagementService`:
  - `updatePrinterStatus(printerId, status)`: Change printer state
  - `redirectPrinter(printerId, targetPrinterId)`: Set redirect target
  - `testPrinter(printerId)`: Send test print
  - `listPrinters(siteId)`: Get all printers with status

**REST Controllers:**
- `GET /api/printers`: List printers for site
- `PUT /api/printers/{id}/status`: Update printer status
- `POST /api/printers/{id}/redirect`: Set redirect target
- `POST /api/printers/{id}/test`: Test printer
- `POST /api/print-jobs/{id}/reprint`: Reprint job

**Domain Events:**
- Listens to `OrderConfirmed`: Creates print jobs when order is confirmed


---

### Module 7: Payments and Billing

**Responsibility**: Payment processing, bill splitting, fiscal document generation.

**Core Entities:**
- `Payment`: id, tenant_id, order_id, amount, payment_method, status, idempotency_key, created_at, version
  - PaymentMethod: CASH, CARD, MOBILE, VOUCHER, MIXED
  - Status: PENDING, COMPLETED, FAILED, VOIDED
- `FiscalDocument`: id, tenant_id, site_id, document_type, document_number, order_id, amount, customer_nif, issued_at, voided_at
  - DocumentType: RECEIPT, INVOICE, CREDIT_NOTE
- `BillSplit`: id, order_id, split_count, created_at
- `PaymentCardBlacklist`: id, tenant_id, card_last_four, reason, blocked_at

**Application Services:**
- `PaymentService`:
  - `processPayment(orderId, amount, method, idempotencyKey)`: Process payment
  - `voidPayment(paymentId, reason)`: Cancel payment
  - `getOrderPayments(orderId)`: Get all payments for order
  - `calculateChange(orderTotal, paymentAmount)`: Calculate change
- `BillingService`:
  - `generateFiscalDocument(orderId, documentType, customerNif)`: Create invoice/receipt
  - `voidFiscalDocument(documentId, reason)`: Void document
  - `printSubtotal(orderId)`: Print intermediate bill
  - `splitBill(orderId, splitCount)`: Divide order for split payment
- `PaymentIntegrationService`:
  - `processCardPayment(amount, terminalId)`: Integrate with payment terminal
  - `refundCardPayment(transactionId, amount)`: Process refund

**REST Controllers:**
- `POST /api/payments`: Process payment
- `POST /api/payments/{id}/void`: Void payment
- `GET /api/orders/{orderId}/payments`: Get order payments
- `POST /api/billing/documents`: Generate fiscal document
- `POST /api/billing/documents/{id}/void`: Void document
- `POST /api/billing/subtotal`: Print subtotal
- `POST /api/billing/split`: Split bill

**Domain Events:**
- `PaymentCompleted`: Emitted when payment is successful (triggers cash movement)
- `FiscalDocumentGenerated`: Emitted when document is created


---

### Module 8: Customers

**Responsibility**: Customer data management for delivery orders.

**Core Entities:**
- `Customer`: id, tenant_id, name, phone, address, delivery_notes, created_at
- `CustomerOrderHistory`: customer_id, order_id, order_date

**Application Services:**
- `CustomerService`:
  - `searchByPhone(phone)`: Search customers by full or suffix phone match
  - `searchByPhoneSuffix(suffix)`: Search by last N digits
  - `createCustomer(tenantId, customerDetails)`: Create new customer
  - `updateCustomer(customerId, updates)`: Modify customer details
  - `getOrderHistory(customerId)`: Get customer's previous orders

**REST Controllers:**
- `GET /api/customers/search?phone={phone}`: Search by phone
- `GET /api/customers/search?suffix={suffix}`: Search by phone suffix
- `POST /api/customers`: Create customer
- `PUT /api/customers/{id}`: Update customer
- `GET /api/customers/{id}/orders`: Get order history

**Domain Events:** None

---

### Module 9: Cash Register

**Responsibility**: Cash session management, movements, closings, financial reporting.

**Core Entities:**
- `CashRegister`: id, tenant_id, site_id, register_number, status
- `CashSession`: id, tenant_id, register_id, employee_id, opening_amount, expected_close, actual_close, variance, opened_at, closed_at, version
  - Status: OPEN, CLOSED
- `CashMovement`: id, tenant_id, session_id, movement_type, amount, reason, payment_id, created_at, created_by
  - MovementType: SALE, REFUND, DEPOSIT, WITHDRAWAL, OPENING, CLOSING
- `CashClosing`: id, tenant_id, closing_type, period_start, period_end, total_sales, total_refunds, variance, closed_at, closed_by
  - ClosingType: SESSION, REGISTER, DAY, FINANCIAL_PERIOD

**Application Services:**
- `CashSessionService`:
  - `openSession(registerId, employeeId, openingAmount)`: Start cash session
  - `closeSession(sessionId, actualAmount)`: Close session and calculate variance
  - `recordMovement(sessionId, type, amount, reason)`: Record manual movement
  - `getSessionSummary(sessionId)`: Get session details with movements
- `CashClosingService`:
  - `closeRegister(registerId, periodStart, periodEnd)`: Close register
  - `closeDay(siteId, date)`: Close day
  - `closeFinancialPeriod(tenantId, periodStart, periodEnd)`: Close period
  - `generateClosingReport(closingId)`: Generate report
  - `reprintClosingReport(closingId)`: Reprint previous report

**REST Controllers:**
- `POST /api/cash/sessions`: Open session
- `POST /api/cash/sessions/{id}/close`: Close session
- `POST /api/cash/sessions/{id}/movements`: Record movement
- `GET /api/cash/sessions/{id}`: Get session summary
- `POST /api/cash/closings/register`: Close register
- `POST /api/cash/closings/day`: Close day
- `GET /api/cash/closings/{id}/report`: Get closing report
- `POST /api/cash/closings/{id}/reprint`: Reprint report

**Domain Events:**
- Listens to `PaymentCompleted`: Creates cash movement for sales


## Data Models

### Multi-Tenancy Strategy

All domain tables include:
- `tenant_id` (UUID, NOT NULL, indexed): Identifies data ownership
- `site_id` (UUID, NULL, indexed): Optional for multi-location tenants
- Composite indexes: `(tenant_id, <business_key>)` for efficient queries

### Auditing Fields

All transactional tables include:
- `created_at` (TIMESTAMP, NOT NULL, DEFAULT NOW())
- `created_by` (UUID, references users.id)
- `updated_at` (TIMESTAMP, NOT NULL, DEFAULT NOW())
- `updated_by` (UUID, references users.id)
- `version` (INTEGER, NOT NULL, DEFAULT 0): For optimistic locking

### Core Tables

**tenants**
- id (UUID, PK)
- name (VARCHAR(255), NOT NULL)
- subscription_plan (VARCHAR(50))
- status (VARCHAR(20)): ACTIVE, SUSPENDED, CANCELLED
- created_at, updated_at

**sites**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- address (TEXT)
- timezone (VARCHAR(50))
- settings (JSONB)
- created_at, updated_at
- UNIQUE(tenant_id, name)

**users**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- username (VARCHAR(100), NOT NULL)
- password_hash (VARCHAR(255), NOT NULL)
- email (VARCHAR(255))
- role (VARCHAR(50)): ADMIN, MANAGER, CASHIER, WAITER, KITCHEN_STAFF
- status (VARCHAR(20)): ACTIVE, INACTIVE
- created_at, updated_at, version
- UNIQUE(tenant_id, username)
- INDEX(tenant_id, status)

**refresh_tokens**
- id (UUID, PK)
- user_id (UUID, FK users.id, NOT NULL)
- token_hash (VARCHAR(255), NOT NULL)
- expires_at (TIMESTAMP, NOT NULL)
- revoked (BOOLEAN, DEFAULT FALSE)
- created_at
- INDEX(user_id, revoked, expires_at)

**dining_tables**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- site_id (UUID, FK sites.id, NOT NULL)
- table_number (VARCHAR(20), NOT NULL)
- area (VARCHAR(100))
- status (VARCHAR(20)): AVAILABLE, OCCUPIED, RESERVED, BLOCKED
- capacity (INTEGER)
- created_at, updated_at, version
- UNIQUE(tenant_id, site_id, table_number)
- INDEX(tenant_id, site_id, status)

**blacklist_entries**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- entity_type (VARCHAR(20)): TABLE, CARD
- entity_value (VARCHAR(255), NOT NULL)
- reason (TEXT)
- blocked_at (TIMESTAMP, NOT NULL)
- created_by (UUID, FK users.id)
- UNIQUE(tenant_id, entity_type, entity_value)


**families**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- display_order (INTEGER, DEFAULT 0)
- active (BOOLEAN, DEFAULT TRUE)
- created_at, updated_at
- UNIQUE(tenant_id, name)

**subfamilies**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- family_id (UUID, FK families.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- display_order (INTEGER, DEFAULT 0)
- active (BOOLEAN, DEFAULT TRUE)
- created_at, updated_at
- UNIQUE(tenant_id, family_id, name)

**items**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- subfamily_id (UUID, FK subfamilies.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- description (TEXT)
- base_price (DECIMAL(10,2), NOT NULL)
- available (BOOLEAN, DEFAULT TRUE)
- image_url (VARCHAR(500))
- created_at, updated_at
- INDEX(tenant_id, subfamily_id, available)

**orders**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- site_id (UUID, FK sites.id, NOT NULL)
- table_id (UUID, FK dining_tables.id, NULL)
- customer_id (UUID, FK customers.id, NULL)
- order_type (VARCHAR(20)): DINE_IN, DELIVERY, TAKEOUT
- status (VARCHAR(20)): OPEN, CONFIRMED, PAID, CLOSED, VOIDED
- total_amount (DECIMAL(10,2), DEFAULT 0)
- created_at, updated_at, created_by, updated_by, version
- INDEX(tenant_id, site_id, status)
- INDEX(tenant_id, table_id, status)

**order_lines**
- id (UUID, PK)
- order_id (UUID, FK orders.id, NOT NULL)
- item_id (UUID, FK items.id, NOT NULL)
- quantity (INTEGER, NOT NULL)
- unit_price (DECIMAL(10,2), NOT NULL)
- modifiers (JSONB)
- notes (TEXT)
- status (VARCHAR(20)): PENDING, CONFIRMED, VOIDED
- created_at, updated_at, version
- INDEX(order_id, status)

**consumptions**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- order_line_id (UUID, FK order_lines.id, NOT NULL)
- quantity (INTEGER, NOT NULL)
- confirmed_at (TIMESTAMP, NOT NULL)
- voided_at (TIMESTAMP, NULL)
- created_at, created_by

**discounts**
- id (UUID, PK)
- order_id (UUID, FK orders.id, NOT NULL)
- order_line_id (UUID, FK order_lines.id, NULL)
- type (VARCHAR(20)): PERCENTAGE, FIXED_AMOUNT
- amount (DECIMAL(10,2), NOT NULL)
- reason (TEXT)
- applied_by (UUID, FK users.id, NOT NULL)
- created_at


**printers**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- site_id (UUID, FK sites.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- ip_address (VARCHAR(50))
- zone (VARCHAR(100))
- status (VARCHAR(20)): NORMAL, WAIT, IGNORE, REDIRECT
- redirect_to_printer_id (UUID, FK printers.id, NULL)
- created_at, updated_at
- UNIQUE(tenant_id, site_id, name)

**print_jobs**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- order_id (UUID, FK orders.id, NOT NULL)
- printer_id (UUID, FK printers.id, NOT NULL)
- content (TEXT, NOT NULL)
- status (VARCHAR(20)): PENDING, PRINTED, FAILED, SKIPPED
- dedupe_key (VARCHAR(255), NOT NULL)
- created_at, updated_at
- UNIQUE(tenant_id, dedupe_key)
- INDEX(tenant_id, printer_id, status)

**payments**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- order_id (UUID, FK orders.id, NOT NULL)
- amount (DECIMAL(10,2), NOT NULL)
- payment_method (VARCHAR(20)): CASH, CARD, MOBILE, VOUCHER, MIXED
- status (VARCHAR(20)): PENDING, COMPLETED, FAILED, VOIDED
- idempotency_key (VARCHAR(255), NOT NULL)
- terminal_transaction_id (VARCHAR(255), NULL)
- created_at, updated_at, created_by, version
- UNIQUE(tenant_id, idempotency_key)
- INDEX(tenant_id, order_id, status)

**fiscal_documents**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- site_id (UUID, FK sites.id, NOT NULL)
- document_type (VARCHAR(20)): RECEIPT, INVOICE, CREDIT_NOTE
- document_number (VARCHAR(50), NOT NULL)
- order_id (UUID, FK orders.id, NOT NULL)
- amount (DECIMAL(10,2), NOT NULL)
- customer_nif (VARCHAR(20), NULL)
- issued_at (TIMESTAMP, NOT NULL)
- voided_at (TIMESTAMP, NULL)
- created_at, created_by
- UNIQUE(tenant_id, site_id, document_type, document_number)
- INDEX(tenant_id, site_id, issued_at)

**customers**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- name (VARCHAR(255), NOT NULL)
- phone (VARCHAR(20), NOT NULL)
- address (TEXT)
- delivery_notes (TEXT)
- created_at, updated_at
- INDEX(tenant_id, phone)
- INDEX(tenant_id, phone varchar_pattern_ops) -- for suffix search

**cash_registers**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- site_id (UUID, FK sites.id, NOT NULL)
- register_number (VARCHAR(20), NOT NULL)
- status (VARCHAR(20)): ACTIVE, INACTIVE
- created_at, updated_at
- UNIQUE(tenant_id, site_id, register_number)


**cash_sessions**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- register_id (UUID, FK cash_registers.id, NOT NULL)
- employee_id (UUID, FK users.id, NOT NULL)
- opening_amount (DECIMAL(10,2), NOT NULL)
- expected_close (DECIMAL(10,2), NULL)
- actual_close (DECIMAL(10,2), NULL)
- variance (DECIMAL(10,2), NULL)
- status (VARCHAR(20)): OPEN, CLOSED
- opened_at (TIMESTAMP, NOT NULL)
- closed_at (TIMESTAMP, NULL)
- created_at, updated_at, version
- INDEX(tenant_id, register_id, status)
- INDEX(tenant_id, employee_id, opened_at)

**cash_movements**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- session_id (UUID, FK cash_sessions.id, NOT NULL)
- movement_type (VARCHAR(20)): SALE, REFUND, DEPOSIT, WITHDRAWAL, OPENING, CLOSING
- amount (DECIMAL(10,2), NOT NULL)
- reason (TEXT)
- payment_id (UUID, FK payments.id, NULL)
- created_at, created_by
- INDEX(tenant_id, session_id, movement_type)

**cash_closings**
- id (UUID, PK)
- tenant_id (UUID, FK tenants.id, NOT NULL)
- closing_type (VARCHAR(20)): SESSION, REGISTER, DAY, FINANCIAL_PERIOD
- period_start (TIMESTAMP, NOT NULL)
- period_end (TIMESTAMP, NOT NULL)
- total_sales (DECIMAL(10,2), NOT NULL)
- total_refunds (DECIMAL(10,2), NOT NULL)
- variance (DECIMAL(10,2), NOT NULL)
- closed_at (TIMESTAMP, NOT NULL)
- closed_by (UUID, FK users.id, NOT NULL)
- INDEX(tenant_id, closing_type, period_start)

### State Transitions

**Table Status:**
```
AVAILABLE → OCCUPIED (when order created)
OCCUPIED → AVAILABLE (when all orders closed)
AVAILABLE/OCCUPIED → BLOCKED (manual block)
BLOCKED → AVAILABLE (manual unblock)
AVAILABLE → RESERVED (reservation created)
RESERVED → OCCUPIED (customer arrives)
```

**Order Status:**
```
OPEN → CONFIRMED (order confirmation "Pedir")
CONFIRMED → PAID (payment completed)
PAID → CLOSED (final close)
OPEN/CONFIRMED → VOIDED (order cancelled)
```

**Order Line Status:**
```
PENDING → CONFIRMED (order confirmation)
PENDING/CONFIRMED → VOIDED (line cancelled)
```

**Payment Status:**
```
PENDING → COMPLETED (payment successful)
PENDING → FAILED (payment error)
COMPLETED → VOIDED (payment cancelled)
```

**Print Job Status:**
```
PENDING → PRINTED (successfully printed)
PENDING → FAILED (print error)
PENDING → SKIPPED (printer in IGNORE mode)
```

**Cash Session Status:**
```
OPEN → CLOSED (session closed with variance calculation)
```


### Indexing Strategy

**Hot Path Indexes:**

1. **Table Map Queries** (frequent real-time updates):
   - `CREATE INDEX idx_tables_tenant_site_status ON dining_tables(tenant_id, site_id, status)`

2. **Open Orders per Table** (waiter operations):
   - `CREATE INDEX idx_orders_tenant_table_status ON orders(tenant_id, table_id, status)`

3. **Customer Phone Search** (delivery orders):
   - `CREATE INDEX idx_customers_tenant_phone ON customers(tenant_id, phone)`
   - `CREATE INDEX idx_customers_phone_suffix ON customers(tenant_id, phone varchar_pattern_ops)` -- for LIKE '%suffix'

4. **Cash Reports** (financial operations):
   - `CREATE INDEX idx_cash_sessions_tenant_register_status ON cash_sessions(tenant_id, register_id, status)`
   - `CREATE INDEX idx_cash_movements_tenant_session ON cash_movements(tenant_id, session_id, movement_type)`
   - `CREATE INDEX idx_cash_closings_tenant_type_period ON cash_closings(tenant_id, closing_type, period_start)`

5. **Print Job Processing**:
   - `CREATE INDEX idx_print_jobs_tenant_printer_status ON print_jobs(tenant_id, printer_id, status)`

6. **Payment Idempotency**:
   - `CREATE UNIQUE INDEX idx_payments_tenant_idempotency ON payments(tenant_id, idempotency_key)`

7. **Fiscal Document Lookup**:
   - `CREATE UNIQUE INDEX idx_fiscal_docs_tenant_site_type_number ON fiscal_documents(tenant_id, site_id, document_type, document_number)`

### PostgreSQL Row Level Security (Optional)

**RLS Policies** (applied when RLS is enabled):

```sql
-- Enable RLS on all domain tables
ALTER TABLE dining_tables ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
-- ... (repeat for all domain tables)

-- Policy: Users can only access their tenant's data
CREATE POLICY tenant_isolation_policy ON dining_tables
  USING (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE POLICY tenant_isolation_policy ON orders
  USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- ... (repeat for all domain tables)
```

**Application Setup:**
```java
// Before each request, set tenant context in PostgreSQL session
jdbcTemplate.execute("SET app.tenant_id = '" + tenantId + "'");
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified the following redundancies to eliminate:

- **Audit properties** (10.3, 10.10, 7.8, 11.7, 17.5): These all test that audit logs are created for operations. Combined into a single comprehensive audit property.
- **Idempotency properties** (6.7, 7.2): Both test idempotency with dedupe/idempotency keys. Combined into one property covering both print jobs and payments.
- **Blacklist properties** (3.6, 7.10): Both test that blacklisted entities are rejected. Combined into one property covering tables and cards.
- **Permission properties** (2.5): Covers all permission checks, no need for separate properties per action type.
- **Data completeness properties** (6.8, 7.7, 9.6, 18.5): Similar pattern of ensuring required data is present. Kept separate as they test different domains.
- **State transition properties** (3.4, 3.8, 5.1, 5.3, 7.5): Each tests different state machines, kept separate for clarity.

### Core Properties


**Property 1: Tenant Data Isolation**
*For any* data access operation and any authenticated user, the returned results SHALL only contain data where tenant_id matches the user's tenant, ensuring complete tenant isolation.
**Validates: Requirements 1.4**

**Property 2: PostgreSQL RLS Enforcement**
*For any* database query when RLS is enabled and tenant context is set, only rows matching the current_setting('app.tenant_id') SHALL be accessible, providing database-level tenant isolation.
**Validates: Requirements 1.5**

**Property 3: JWT Token Contains Tenant**
*For any* successful authentication, the issued JWT access token SHALL contain a tenant_id claim matching the authenticated user's tenant.
**Validates: Requirements 1.6**

**Property 4: Tenant Provisioning Atomicity**
*For any* tenant provisioning operation, either all tenant data (tenant record, configuration, initial data) SHALL be created successfully, or no data SHALL be created (transaction rollback on failure).
**Validates: Requirements 1.8**

**Property 5: Token Issuance and Expiry**
*For any* valid login with correct credentials, the system SHALL issue an access token with 15-minute expiry and a refresh token with 7-day expiry.
**Validates: Requirements 2.2**

**Property 6: Token Refresh**
*For any* expired access token paired with a valid (non-revoked, non-expired) refresh token, the token refresh operation SHALL succeed and issue a new access token.
**Validates: Requirements 2.3**

**Property 7: Permission Enforcement**
*For any* sensitive operation (void after subtotal, apply discount, reprint document, redirect printer, close cash, void invoice) and any user without the required permission, the operation SHALL be rejected with an authorization error.
**Validates: Requirements 2.5**

**Property 8: Password Hashing**
*For any* user account created or password updated, the stored password SHALL be bcrypt-hashed (not plaintext), verifiable by bcrypt format pattern.
**Validates: Requirements 2.7**

**Property 9: Refresh Token Invalidation**
*For any* logout operation or security event, the associated refresh token SHALL be marked as revoked and SHALL NOT be usable for subsequent token refresh attempts.
**Validates: Requirements 2.8**

**Property 10: Table State Transition on Open**
*For any* table in AVAILABLE state, when a user opens the table, the table SHALL transition to OCCUPIED state.
**Validates: Requirements 3.4**

**Property 11: Order Transfer Consistency**
*For any* order transfer from table A to table B, all order lines SHALL be reassigned to table B, table A's state SHALL update appropriately, and table B SHALL reflect the new order.
**Validates: Requirements 3.5**

**Property 12: Blacklist Enforcement**
*For any* entity (table or payment card) in the blacklist, all operations on that entity SHALL be rejected with a blocked error.
**Validates: Requirements 3.6, 7.10**

**Property 13: Table State Transition on Close**
*For any* table with all associated orders in CLOSED or VOIDED status, the table SHALL transition to AVAILABLE state.
**Validates: Requirements 3.8**

**Property 14: Unavailable Item Rejection**
*For any* catalog item marked as unavailable, attempts to add that item to an order SHALL be rejected with an unavailable error.
**Validates: Requirements 4.3**

**Property 15: Catalog Change Isolation**
*For any* catalog modification (price change, item update), existing order lines SHALL retain their original item data (price, name), while new order lines SHALL use the updated catalog data.
**Validates: Requirements 4.6**

**Property 16: Order Line Creation State**
*For any* item added to an order, the created order line SHALL have status PENDING until order confirmation.
**Validates: Requirements 5.1**

**Property 17: Order Confirmation State Transition**
*For any* order confirmation operation, all order lines with status PENDING SHALL transition to CONFIRMED status.
**Validates: Requirements 5.3**

**Property 18: Consumption Record Creation**
*For any* order line transitioned to CONFIRMED status, a corresponding consumption record SHALL be created with matching quantity and order_line_id.
**Validates: Requirements 5.4**

**Property 19: Optimistic Locking Conflict Detection**
*For any* concurrent modification attempt on the same entity (Order, Table, Cash_Session, Payment) by two transactions, the second transaction SHALL fail with an optimistic locking exception.
**Validates: Requirements 5.9**

**Property 20: Print Job Creation on Order Confirmation**
*For any* confirmed order, print jobs SHALL be created for each order line, routed to the appropriate printer based on item category and zone configuration.
**Validates: Requirements 6.1**

**Property 21: Printer Redirect Routing**
*For any* printer in REDIRECT status with a configured redirect_to_printer_id, all print jobs assigned to that printer SHALL be routed to the redirect target printer.
**Validates: Requirements 6.4**

**Property 22: Printer Ignore Behavior**
*For any* printer in IGNORE status, all print jobs assigned to that printer SHALL be marked with status SKIPPED and SHALL NOT be sent to the printer.
**Validates: Requirements 6.5**

**Property 23: Idempotency Key Enforcement**
*For any* operation with an idempotency key (payment or print job), multiple requests with the same idempotency key SHALL result in only one entity being created, with subsequent requests returning the existing entity.
**Validates: Requirements 6.7, 7.2**

**Property 24: Print Job Content Completeness**
*For any* created print job, the content SHALL include table number, item name, quantity, modifiers, and timestamp.
**Validates: Requirements 6.8**

**Property 25: Partial Payment Invariant**
*For any* order with multiple payments, the sum of all completed payment amounts SHALL be less than or equal to the order total amount (until fully paid).
**Validates: Requirements 7.3**

**Property 26: Order Closure on Full Payment**
*For any* order where the sum of completed payment amounts equals or exceeds the order total, the order status SHALL transition to CLOSED.
**Validates: Requirements 7.5**

**Property 27: Fiscal Document Sequential Numbering**
*For any* two fiscal documents of the same type (RECEIPT, INVOICE, CREDIT_NOTE) within the same tenant and site, their document numbers SHALL be sequential and unique without gaps.
**Validates: Requirements 7.6**

**Property 28: Invoice NIF Requirement**
*For any* fiscal document of type INVOICE, the customer_nif field SHALL be populated with a valid tax identification number.
**Validates: Requirements 7.7**

**Property 29: Audit Trail on Sensitive Operations**
*For any* sensitive operation (void payment, void order line, apply discount, close cash session, reprint document, change printer state, export fiscal data), an audit log entry SHALL be created with timestamp, user_id, operation type, and relevant entity IDs.
**Validates: Requirements 7.8, 10.3, 10.10, 11.7, 17.5**

**Property 30: Cash Payment Change Calculation**
*For any* cash payment where payment amount exceeds order total, the calculated change SHALL equal payment amount minus order total.
**Validates: Requirements 7.9**

**Property 31: Customer Phone Suffix Search**
*For any* customer with phone number ending in suffix S, searching by suffix S SHALL return that customer in the results.
**Validates: Requirements 8.1**

**Property 32: Customer Search Completeness**
*For any* phone search query matching multiple customers, all matching customers SHALL be returned in the search results.
**Validates: Requirements 8.2**

**Property 33: Customer Order History Completeness**
*For any* customer, their order history SHALL include all orders where customer_id matches, ordered by date descending.
**Validates: Requirements 8.4**

**Property 34: Reservation Cancellation Reason**
*For any* reservation cancellation operation, a reason note SHALL be stored with the cancellation record.
**Validates: Requirements 9.6**

**Property 35: Cash Session Opening**
*For any* cash session open operation, a new cash_session record SHALL be created with status OPEN and the specified opening_amount.
**Validates: Requirements 10.2**

**Property 36: Cash Movement on Payment**
*For any* completed payment of type CASH, a corresponding cash_movement record SHALL be created with movement_type SALE and amount matching the payment amount.
**Validates: Requirements 10.4**

**Property 37: Cash Session Variance Calculation**
*For any* cash session close operation, the variance SHALL equal actual_close minus expected_close, where expected_close is calculated from opening_amount plus all movements.
**Validates: Requirements 10.6**

**Property 38: Cash Report Completeness**
*For any* cash closing report, the report SHALL include opening_balance, all movements, expected_close, actual_close, and calculated variance.
**Validates: Requirements 10.8**

**Property 39: Printer State Change Propagation**
*For any* printer status change, all print jobs created after the change SHALL use the new printer status for routing decisions.
**Validates: Requirements 11.2**

**Property 40: Transaction Rollback on Failure**
*For any* transactional operation that encounters an error, no partial changes SHALL be visible in the database (all changes rolled back).
**Validates: Requirements 12.4**

**Property 41: Fiscal Document Number Uniqueness**
*For any* attempt to create a fiscal document with a document_number that already exists for the same tenant, site, and document_type, the operation SHALL be rejected with a uniqueness constraint violation.
**Validates: Requirements 12.5**

**Property 42: Input Validation**
*For any* API request with invalid input data (wrong format, out of range, missing required fields), the system SHALL reject the request with a validation error before processing.
**Validates: Requirements 13.1**

**Property 43: SQL Injection Prevention**
*For any* user input containing SQL injection patterns (e.g., '; DROP TABLE), the system SHALL sanitize or reject the input, preventing SQL execution.
**Validates: Requirements 13.2**

**Property 44: Sensitive Data Masking in Logs**
*For any* log entry containing sensitive data (password, card number, NIF), the sensitive portions SHALL be masked (e.g., card ending in ****1234).
**Validates: Requirements 13.3**

**Property 45: Authentication Rate Limiting**
*For any* source IP or username with excessive failed authentication attempts (e.g., >5 in 1 minute), subsequent authentication attempts SHALL be rejected with a rate limit error.
**Validates: Requirements 13.4**

**Property 46: CSRF Token Validation**
*For any* state-changing API operation (POST, PUT, DELETE) without a valid CSRF token, the request SHALL be rejected with a CSRF validation error.
**Validates: Requirements 13.6**

**Property 47: Structured Log Format**
*For any* log entry emitted by the system, the log SHALL be in JSON format and include fields: timestamp, level, tenant_id, user_id, trace_id, message.
**Validates: Requirements 14.1**

**Property 48: API Request Logging**
*For any* API request processed, a log entry SHALL be created including method, path, status_code, duration_ms, and tenant_id.
**Validates: Requirements 14.2**

**Property 49: Correlation ID Propagation**
*For any* request with a correlation ID, all log entries generated during that request's processing SHALL include the same correlation ID for traceability.
**Validates: Requirements 14.6**

**Property 50: SAF-T Export Completeness**
*For any* SAF-T PT export request for date range [start, end], the generated XML SHALL include all fiscal documents with issued_at between start and end for the specified tenant.
**Validates: Requirements 17.1**

**Property 51: SAF-T XML Schema Validation**
*For any* generated SAF-T PT XML export, the XML SHALL be valid according to the SAF-T PT schema (round-trip: generate → validate → pass).
**Validates: Requirements 17.3**

**Property 52: Payment Terminal Response Handling**
*For any* payment terminal response (approved, declined, timeout, error), the system SHALL handle the response appropriately: APPROVED → complete payment, DECLINED → fail payment, TIMEOUT/ERROR → allow retry.
**Validates: Requirements 18.3**

**Property 53: Payment Terminal Transaction ID Storage**
*For any* card payment processed through a payment terminal, the terminal_transaction_id SHALL be stored with the payment record for reconciliation.
**Validates: Requirements 18.5**


## Error Handling

### Error Response Format

All API errors follow RFC 7807 Problem Details format:

```json
{
  "type": "https://api.restaurant-pos.com/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Order line quantity must be positive",
  "instance": "/api/orders/123/lines",
  "traceId": "abc-123-def",
  "errors": [
    {
      "field": "quantity",
      "message": "must be greater than 0"
    }
  ]
}
```

### Error Categories

**Client Errors (4xx):**
- `400 Bad Request`: Invalid input, validation failures
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource does not exist or not accessible to tenant
- `409 Conflict`: Optimistic locking conflict, duplicate idempotency key
- `422 Unprocessable Entity`: Business rule violation (e.g., unavailable item, blacklisted table)
- `429 Too Many Requests`: Rate limit exceeded

**Server Errors (5xx):**
- `500 Internal Server Error`: Unexpected application error
- `503 Service Unavailable`: Database connection failure, external service timeout

### Error Handling Strategies

**Optimistic Locking Conflicts:**
- Return 409 Conflict with current entity version
- Client should retry with updated version

**Idempotency Key Conflicts:**
- Return 200 OK with existing entity (not an error)
- Ensures safe retries

**Tenant Isolation Violations:**
- Return 404 Not Found (don't reveal existence of other tenant's data)
- Log security event for monitoring

**Payment Terminal Failures:**
- Return 422 with terminal error details
- Allow client to retry or choose alternative payment method

**Database Transaction Failures:**
- Automatic rollback
- Return 500 with generic error (log details server-side)

**Validation Errors:**
- Return 400 with detailed field-level errors
- Use Bean Validation annotations for automatic validation


## Testing Strategy

### Dual Testing Approach

The system requires both unit tests and property-based tests for comprehensive coverage:

**Unit Tests:**
- Specific examples demonstrating correct behavior
- Edge cases (empty inputs, boundary values, null handling)
- Error conditions and exception handling
- Integration points between modules
- Mock external dependencies (payment terminals, printers)

**Property-Based Tests:**
- Universal properties that hold for all inputs
- Comprehensive input coverage through randomization
- Minimum 100 iterations per property test
- Each property test references its design document property

**Balance:** Avoid excessive unit tests. Property-based tests handle broad input coverage. Focus unit tests on specific examples, edge cases, and integration scenarios.

### Property-Based Testing Configuration

**Library:** Use [QuickTheories](https://github.com/quicktheories/QuickTheories) for Java property-based testing

**Test Structure:**
```java
@Test
void property1_tenantDataIsolation() {
    // Feature: restaurant-pos-saas, Property 1: Tenant Data Isolation
    qt()
        .forAll(tenants(), users(), dataQueries())
        .checkAssert((tenant, user, query) -> {
            // Setup: Create data for multiple tenants
            // Execute: Run query as user
            // Assert: Results only contain user's tenant data
        });
}
```

**Configuration:**
- Minimum 100 iterations per test: `qt().withExamples(100)`
- Shrinking enabled for failure diagnosis
- Custom generators for domain objects (Order, Table, Payment, etc.)

**Tag Format:**
```java
// Feature: restaurant-pos-saas, Property {number}: {property_text}
```

### Test Coverage by Module

**Module 1: Tenant Provisioning**
- Unit: Tenant creation with valid/invalid data
- Unit: Site creation and association
- Property 4: Tenant provisioning atomicity

**Module 2: Identity and Access**
- Unit: Login with valid/invalid credentials
- Unit: Token expiry edge cases
- Property 3: JWT contains tenant
- Property 5: Token issuance and expiry
- Property 6: Token refresh
- Property 7: Permission enforcement
- Property 8: Password hashing
- Property 9: Refresh token invalidation

**Module 3: Dining Room**
- Unit: Table map retrieval
- Unit: Table transfer with no orders
- Property 1: Tenant data isolation (test with tables)
- Property 10: Table state transition on open
- Property 11: Order transfer consistency
- Property 12: Blacklist enforcement (tables)
- Property 13: Table state transition on close

**Module 4: Catalog**
- Unit: Menu hierarchy retrieval
- Unit: Quick page creation
- Property 14: Unavailable item rejection
- Property 15: Catalog change isolation

**Module 5: Orders**
- Unit: Order creation for dine-in vs delivery
- Unit: Discount application
- Property 16: Order line creation state
- Property 17: Order confirmation state transition
- Property 18: Consumption record creation
- Property 19: Optimistic locking conflict detection

**Module 6: Kitchen and Printing**
- Unit: Print job content formatting
- Unit: Zone-based routing logic
- Property 20: Print job creation on order confirmation
- Property 21: Printer redirect routing
- Property 22: Printer ignore behavior
- Property 23: Idempotency key enforcement (print jobs)
- Property 24: Print job content completeness

**Module 7: Payments and Billing**
- Unit: Change calculation edge cases
- Unit: Bill splitting logic
- Property 12: Blacklist enforcement (cards)
- Property 23: Idempotency key enforcement (payments)
- Property 25: Partial payment invariant
- Property 26: Order closure on full payment
- Property 27: Fiscal document sequential numbering
- Property 28: Invoice NIF requirement
- Property 29: Audit trail on sensitive operations (payments)
- Property 30: Cash payment change calculation

**Module 8: Customers**
- Unit: Customer creation with duplicate phone
- Unit: Phone suffix search with various patterns
- Property 31: Customer phone suffix search
- Property 32: Customer search completeness
- Property 33: Customer order history completeness

**Module 9: Cash Register**
- Unit: Session open/close flow
- Unit: Manual movement recording
- Property 29: Audit trail on sensitive operations (cash)
- Property 35: Cash session opening
- Property 36: Cash movement on payment
- Property 37: Cash session variance calculation
- Property 38: Cash report completeness

**Module 10: Cross-Cutting Concerns**
- Property 2: PostgreSQL RLS enforcement
- Property 40: Transaction rollback on failure
- Property 41: Fiscal document number uniqueness
- Property 42: Input validation
- Property 43: SQL injection prevention
- Property 44: Sensitive data masking in logs
- Property 45: Authentication rate limiting
- Property 46: CSRF token validation
- Property 47: Structured log format
- Property 48: API request logging
- Property 49: Correlation ID propagation

**Module 11: Integrations**
- Unit: SAF-T XML generation structure
- Unit: Payment terminal mock responses
- Property 50: SAF-T export completeness
- Property 51: SAF-T XML schema validation
- Property 52: Payment terminal response handling
- Property 53: Payment terminal transaction ID storage

### Integration Tests with Testcontainers

**Critical Flows to Test:**

1. **Complete Order Flow:**
   - Open table → Add order lines → Confirm order → Create print jobs → Process payment → Close order → Verify table available

2. **Partial Payment and Split Bill:**
   - Create order → Add multiple payments (partial) → Verify order remains open → Complete final payment → Verify order closed

3. **Cash Session Lifecycle:**
   - Open session → Record sales (via payments) → Record manual movements → Close session → Verify variance calculation → Generate report

4. **Tenant Isolation:**
   - Create data for Tenant A and Tenant B → Query as Tenant A user → Verify no Tenant B data returned → Query as Tenant B user → Verify no Tenant A data returned

**Testcontainers Setup:**
```java
@Testcontainers
class IntegrationTest {
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
}
```

### Test Data Generators

**Custom Generators for Property Tests:**
- `tenants()`: Generate random tenant IDs
- `users()`: Generate users with various roles
- `tables()`: Generate tables with different states
- `orders()`: Generate orders with random line items
- `payments()`: Generate payments with various methods
- `phoneNumbers()`: Generate valid phone numbers
- `fiscalDocuments()`: Generate documents with sequential numbers

**Generator Constraints:**
- Respect database constraints (foreign keys, unique indexes)
- Generate valid state transitions
- Include edge cases (empty strings, max values, special characters)

