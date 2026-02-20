# Implementation Plan: Restaurant POS SaaS

## Overview

This implementation plan breaks down the Restaurant POS SaaS system into discrete, incremental coding tasks. The system is built as a Spring Modulith modular monolith with PostgreSQL, following a phased approach: MVP core functionality → robustness and testing → integrations and advanced features.

Each task builds on previous work, with checkpoints to ensure stability. Property-based tests are integrated close to implementation to catch errors early.

## Tasks

### Phase 1: Foundation and Core Infrastructure

- [x] 1. Project setup and Spring Modulith configuration
  - Create Maven project with Spring Boot 3.x parent
  - Add dependencies: Spring Boot Starter Web, Data JPA, Security, Validation, PostgreSQL driver, Flyway, Testcontainers, QuickTheories, Micrometer
  - Configure Spring Modulith with module boundaries
  - Set up package structure: `com.restaurantpos.{module}` for each of 9 modules
  - Configure application.yml with PostgreSQL connection, JPA settings, logging
  - Create docker-compose.yml for local PostgreSQL
  - _Requirements: 16.1, 16.2_

- [ ] 2. Database schema baseline migration
  - [x] 2.1 Create V1__baseline.sql with all core tables
    - Create tables: tenants, sites, users, refresh_tokens, dining_tables, blacklist_entries, families, subfamilies, items, orders, order_lines, consumptions, discounts, printers, print_jobs, payments, fiscal_documents, customers, cash_registers, cash_sessions, cash_movements, cash_closings
    - Add tenant_id, site_id columns to all domain tables
    - Add audit columns: created_at, created_by, updated_at, updated_by, version
    - Add unique constraints: tenant+username, tenant+site+table_number, tenant+idempotency_key, tenant+site+document_type+document_number
    - Define enums via CHECK constraints: table status, order status, payment method, printer status, etc.
    - _Requirements: 1.2, 1.3, 16.4_
  
  - [x] 2.2 Create V2__indexes.sql for hot path optimization
    - Add indexes: tables by tenant+site+status, orders by tenant+table+status, customers by tenant+phone (including varchar_pattern_ops for suffix search), cash_sessions by tenant+register+status, print_jobs by tenant+printer+status, payments by tenant+idempotency_key
    - _Requirements: 15.4, 15.5_
  
  - [x] 2.3 Create V3__rls_policies.sql (optional RLS)
    - Enable RLS on all domain tables
    - Create tenant_isolation_policy for each table using current_setting('app.tenant_id')
    - _Requirements: 1.5_

- [ ] 3. Tenant context and multi-tenancy infrastructure
  - [x] 3.1 Implement TenantContext thread-local holder
    - Create TenantContext class with setTenantId/getTenantId/clear methods
    - Create TenantContextFilter to extract tenant from JWT and set context
    - Create TenantAspect to enforce tenant_id on all repository queries
    - _Requirements: 1.4, 1.7_
  
  - [x] 3.2 Write property test for tenant data isolation
    - **Property 1: Tenant Data Isolation**
    - **Validates: Requirements 1.4**
  
  - [x] 3.3 Write property test for RLS enforcement (if RLS enabled)
    - **Property 2: PostgreSQL RLS Enforcement**
    - **Validates: Requirements 1.5**


- [x] 4. JWT authentication and security foundation
  - [x] 4.1 Implement JWT token generation and validation
    - Create JwtTokenProvider with generateAccessToken, generateRefreshToken, validateToken, extractClaims methods
    - Include tenant_id in JWT claims
    - Configure token expiry: access 15min, refresh 7 days
    - _Requirements: 2.1, 2.2, 1.6_
  
  - [x] 4.2 Implement Spring Security configuration
    - Create SecurityConfig with JWT filter chain
    - Configure authentication entry point and access denied handler
    - Set up password encoder (BCrypt with strength 12)
    - Configure CORS and CSRF protection
    - _Requirements: 2.7, 13.6_
  
  - [x] 4.3 Create User entity and repository
    - Define User JPA entity with tenant_id, username, password_hash, email, role, status, version
    - Create UserRepository extending JpaRepository with tenant filtering
    - _Requirements: 2.1_
  
  - [x] 4.4 Implement AuthenticationService
    - login(username, password): authenticate, generate tokens, return AuthResponse
    - refreshToken(refreshToken): validate, issue new access token
    - logout(refreshToken): revoke refresh token
    - _Requirements: 2.2, 2.3, 2.8_
  
  - [x] 4.5 Write property test for JWT token contains tenant
    - **Property 3: JWT Token Contains Tenant**
    - **Validates: Requirements 1.6**
  
  - [x] 4.6 Write property test for token issuance and expiry
    - **Property 5: Token Issuance and Expiry**
    - **Validates: Requirements 2.2**
  
  - [ ]* 4.7 Write property test for token refresh
    - **Property 6: Token Refresh**
    - **Validates: Requirements 2.3**
  
  - [ ]* 4.8 Write property test for password hashing
    - **Property 8: Password Hashing**
    - **Validates: Requirements 2.7**
  
  - [ ]* 4.9 Write property test for refresh token invalidation
    - **Property 9: Refresh Token Invalidation**
    - **Validates: Requirements 2.8**

- [ ] 5. Role-based access control (RBAC)
  - [x] 5.1 Define Permission enum and role mappings
    - Create Permission enum: VOID_AFTER_SUBTOTAL, APPLY_DISCOUNT, REPRINT_DOCUMENT, REDIRECT_PRINTER, CLOSE_CASH, VOID_INVOICE
    - Create Role enum: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN_STAFF with permission mappings
    - _Requirements: 2.4, 2.6_
  
  - [x] 5.2 Implement AuthorizationService
    - hasPermission(userId, permission): check user role and permission
    - requirePermission(permission): throw exception if not authorized
    - Create @RequirePermission annotation for method-level security
    - _Requirements: 2.5_
  
  - [ ]* 5.3 Write property test for permission enforcement
    - **Property 7: Permission Enforcement**
    - **Validates: Requirements 2.5**

- [x] 6. Checkpoint - Foundation complete
  - Ensure all tests pass
  - Verify database migrations apply successfully
  - Verify JWT authentication works end-to-end
  - Ask user if questions arise

### Phase 2: Core Domain Modules

- [ ] 7. Tenant provisioning module
  - [x] 7.1 Create Tenant and Site entities
    - Define Tenant JPA entity: id, name, subscription_plan, status, created_at, updated_at
    - Define Site JPA entity: id, tenant_id, name, address, timezone, settings (JSONB), created_at, updated_at
    - Create TenantRepository and SiteRepository
    - _Requirements: 1.1, 1.8_
  
  - [x] 7.2 Implement TenantProvisioningService
    - provisionTenant(name, plan): create tenant + default configuration in transaction
    - createSite(tenantId, siteDetails): add site to tenant
    - updateTenantSettings(tenantId, settings): modify configuration
    - _Requirements: 1.8_
  
  - [x] 7.3 Create REST controllers for tenant management
    - POST /api/tenants: create tenant (admin only)
    - GET /api/tenants/{id}: get tenant details
    - POST /api/tenants/{id}/sites: create site
    - PUT /api/tenants/{id}/settings: update settings
    - _Requirements: 1.8_
  
  - [ ]* 7.4 Write property test for tenant provisioning atomicity
    - **Property 4: Tenant Provisioning Atomicity**
    - **Validates: Requirements 1.8**

- [ ] 8. Dining room module
  - [x] 8.1 Create DiningTable and BlacklistEntry entities
    - Define DiningTable JPA entity: id, tenant_id, site_id, table_number, area, status, capacity, version
    - Define BlacklistEntry JPA entity: id, tenant_id, entity_type, entity_value, reason, blocked_at, created_by
    - Create repositories with tenant filtering
    - _Requirements: 3.2, 3.6_
  
  - [x] 8.2 Implement TableManagementService
    - getTableMap(siteId): return all tables with current status
    - openTable(tableId): transition to OCCUPIED
    - closeTable(tableId): transition to AVAILABLE
    - transferTable(fromTableId, toTableId): move orders between tables
    - blockTable(tableId, reason): add to blacklist
    - unblockTable(tableId): remove from blacklist
    - _Requirements: 3.1, 3.4, 3.5, 3.6, 3.8_
  
  - [x] 8.3 Create REST controllers for table operations
    - GET /api/tables: get table map
    - POST /api/tables/{id}/open: open table
    - POST /api/tables/{id}/close: close table
    - POST /api/tables/{id}/transfer: transfer orders
    - POST /api/tables/{id}/block: block table
    - DELETE /api/tables/{id}/block: unblock table
    - _Requirements: 3.1, 3.4, 3.5, 3.6_
  
  - [ ]* 8.4 Write property test for table state transition on open
    - **Property 10: Table State Transition on Open**
    - **Validates: Requirements 3.4**
  
  - [ ]* 8.5 Write property test for order transfer consistency
    - **Property 11: Order Transfer Consistency**
    - **Validates: Requirements 3.5**
  
  - [ ]* 8.6 Write property test for blacklist enforcement (tables)
    - **Property 12: Blacklist Enforcement**
    - **Validates: Requirements 3.6**
  
  - [ ]* 8.7 Write property test for table state transition on close
    - **Property 13: Table State Transition on Close**
    - **Validates: Requirements 3.8**


- [ ] 9. Catalog module
  - [x] 9.1 Create catalog entities
    - Define Family JPA entity: id, tenant_id, name, display_order, active
    - Define Subfamily JPA entity: id, tenant_id, family_id, name, display_order, active
    - Define Item JPA entity: id, tenant_id, subfamily_id, name, description, base_price, available, image_url
    - Define QuickPage JPA entity: id, tenant_id, site_id, name, item_ids (array)
    - Create repositories with tenant filtering
    - _Requirements: 4.1, 4.2_
  
  - [x] 9.2 Implement CatalogManagementService
    - createFamily(tenantId, familyDetails): add family
    - createItem(tenantId, itemDetails): add item
    - updateItemAvailability(itemId, available): toggle availability
    - getMenuStructure(tenantId, siteId): return full hierarchy
    - _Requirements: 4.1, 4.3_
  
  - [x] 9.3 Create REST controllers for catalog
    - GET /api/catalog/menu: get menu structure
    - POST /api/catalog/families: create family
    - POST /api/catalog/items: create item
    - PUT /api/catalog/items/{id}: update item
    - PUT /api/catalog/items/{id}/availability: toggle availability
    - _Requirements: 4.1, 4.3_
  
  - [ ]* 9.4 Write property test for unavailable item rejection
    - **Property 14: Unavailable Item Rejection**
    - **Validates: Requirements 4.3**
  
  - [ ]* 9.5 Write property test for catalog change isolation
    - **Property 15: Catalog Change Isolation**
    - **Validates: Requirements 4.6**

- [ ] 10. Orders module
  - [x] 10.1 Create order entities
    - Define Order JPA entity: id, tenant_id, site_id, table_id, customer_id, order_type, status, total_amount, version
    - Define OrderLine JPA entity: id, order_id, item_id, quantity, unit_price, modifiers (JSONB), notes, status, version
    - Define Consumption JPA entity: id, tenant_id, order_line_id, quantity, confirmed_at, voided_at
    - Define Discount JPA entity: id, order_id, order_line_id, type, amount, reason, applied_by
    - Create repositories with tenant filtering and optimistic locking
    - _Requirements: 5.1, 5.2, 5.4, 5.7, 5.9_
  
  - [x] 10.2 Implement OrderService
    - createOrder(tableId, orderType): create new order
    - addOrderLine(orderId, itemId, quantity, modifiers): add item to order
    - updateOrderLine(orderLineId, quantity, modifiers): modify line
    - confirmOrder(orderId): transition lines to CONFIRMED, create consumptions, emit OrderConfirmed event
    - voidOrderLine(orderLineId, reason, recordWaste): cancel line with permission check
    - applyDiscount(orderId, discountDetails): apply discount with permission check
    - getOrdersByTable(tableId): get all orders for table
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6, 5.7_
  
  - [x] 10.3 Create REST controllers for orders
    - POST /api/orders: create order
    - GET /api/orders/{id}: get order details
    - POST /api/orders/{id}/lines: add order line
    - PUT /api/orders/{id}/lines/{lineId}: update order line
    - POST /api/orders/{id}/confirm: confirm order
    - POST /api/orders/{id}/lines/{lineId}/void: void order line
    - POST /api/orders/{id}/discounts: apply discount
    - GET /api/tables/{tableId}/orders: get orders for table
    - _Requirements: 5.1, 5.2, 5.3, 5.6, 5.7_
  
  - [ ]* 10.4 Write property test for order line creation state
    - **Property 16: Order Line Creation State**
    - **Validates: Requirements 5.1**
  
  - [ ]* 10.5 Write property test for order confirmation state transition
    - **Property 17: Order Confirmation State Transition**
    - **Validates: Requirements 5.3**
  
  - [ ]* 10.6 Write property test for consumption record creation
    - **Property 18: Consumption Record Creation**
    - **Validates: Requirements 5.4**
  
  - [ ]* 10.7 Write property test for optimistic locking conflict detection
    - **Property 19: Optimistic Locking Conflict Detection**
    - **Validates: Requirements 5.9**

- [x] 11. Checkpoint - Core domain complete
  - Ensure all tests pass
  - Verify order flow: create → add lines → confirm → consumptions created
  - Verify table state transitions work correctly
  - Ask user if questions arise

### Phase 3: Kitchen, Payments, and Cash

- [ ] 12. Kitchen and printing module
  - [x] 12.1 Create printer and print job entities
    - Define Printer JPA entity: id, tenant_id, site_id, name, ip_address, zone, status, redirect_to_printer_id
    - Define PrintJob JPA entity: id, tenant_id, order_id, printer_id, content, status, dedupe_key
    - Create repositories with tenant filtering
    - _Requirements: 6.1, 6.3, 6.7_
  
  - [x] 12.2 Implement PrintingService
    - createPrintJobs(orderId): generate jobs for confirmed order based on item categories and zones
    - reprintOrder(orderId, printerId): manually reprint with permission check
    - processPrintJob(printJobId): send to printer (handle REDIRECT, IGNORE states)
    - _Requirements: 6.1, 6.2, 6.4, 6.5, 6.6_
  
  - [x] 12.3 Implement OrderConfirmed event listener
    - Listen to OrderConfirmed event from orders module
    - Call createPrintJobs(orderId) to generate print jobs
    - _Requirements: 6.1_
  
  - [x] 12.4 Implement PrinterManagementService
    - updatePrinterStatus(printerId, status): change printer state
    - redirectPrinter(printerId, targetPrinterId): set redirect
    - testPrinter(printerId): send test print
    - listPrinters(siteId): get all printers with status
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  
  - [x] 12.5 Create REST controllers for printing
    - GET /api/printers: list printers
    - PUT /api/printers/{id}/status: update printer status
    - POST /api/printers/{id}/redirect: set redirect
    - POST /api/printers/{id}/test: test printer
    - POST /api/print-jobs/{id}/reprint: reprint job
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  
  - [ ]* 12.6 Write property test for print job creation on order confirmation
    - **Property 20: Print Job Creation on Order Confirmation**
    - **Validates: Requirements 6.1**
  
  - [ ]* 12.7 Write property test for printer redirect routing
    - **Property 21: Printer Redirect Routing**
    - **Validates: Requirements 6.4**
  
  - [ ]* 12.8 Write property test for printer ignore behavior
    - **Property 22: Printer Ignore Behavior**
    - **Validates: Requirements 6.5**
  
  - [ ]* 12.9 Write property test for idempotency key enforcement (print jobs)
    - **Property 23: Idempotency Key Enforcement**
    - **Validates: Requirements 6.7, 7.2**
  
  - [ ]* 12.10 Write property test for print job content completeness
    - **Property 24: Print Job Content Completeness**
    - **Validates: Requirements 6.8**


- [ ] 13. Payments and billing module
  - [x] 13.1 Create payment and fiscal document entities
    - Define Payment JPA entity: id, tenant_id, order_id, amount, payment_method, status, idempotency_key, terminal_transaction_id, version
    - Define FiscalDocument JPA entity: id, tenant_id, site_id, document_type, document_number, order_id, amount, customer_nif, issued_at, voided_at
    - Define PaymentCardBlacklist JPA entity: id, tenant_id, card_last_four, reason, blocked_at
    - Create repositories with tenant filtering and unique constraints
    - _Requirements: 7.1, 7.2, 7.6, 7.10_
  
  - [x] 13.2 Implement PaymentService
    - processPayment(orderId, amount, method, idempotencyKey): process payment with idempotency
    - voidPayment(paymentId, reason): cancel payment with permission check and audit
    - getOrderPayments(orderId): get all payments for order
    - calculateChange(orderTotal, paymentAmount): calculate change for cash
    - _Requirements: 7.1, 7.2, 7.8, 7.9_
  
  - [x] 13.3 Implement BillingService
    - generateFiscalDocument(orderId, documentType, customerNif): create invoice/receipt with sequential numbering
    - voidFiscalDocument(documentId, reason): void document with permission check and audit
    - printSubtotal(orderId): print intermediate bill
    - splitBill(orderId, splitCount): divide order for split payment
    - _Requirements: 7.6, 7.7, 7.8_
  
  - [x] 13.4 Implement order closure logic
    - After each payment, check if sum of payments >= order total
    - If fully paid, transition order to CLOSED status
    - Emit PaymentCompleted event for cash tracking
    - _Requirements: 7.5_
  
  - [x] 13.5 Create REST controllers for payments and billing
    - POST /api/payments: process payment
    - POST /api/payments/{id}/void: void payment
    - GET /api/orders/{orderId}/payments: get order payments
    - POST /api/billing/documents: generate fiscal document
    - POST /api/billing/documents/{id}/void: void document
    - POST /api/billing/subtotal: print subtotal
    - POST /api/billing/split: split bill
    - _Requirements: 7.1, 7.2, 7.5, 7.6, 7.7, 7.8_
  
  - [ ]* 13.6 Write property test for blacklist enforcement (cards)
    - **Property 12: Blacklist Enforcement** (extend to cards)
    - **Validates: Requirements 7.10**
  
  - [ ]* 13.7 Write property test for idempotency key enforcement (payments)
    - **Property 23: Idempotency Key Enforcement** (extend to payments)
    - **Validates: Requirements 7.2**
  
  - [ ]* 13.8 Write property test for partial payment invariant
    - **Property 25: Partial Payment Invariant**
    - **Validates: Requirements 7.3**
  
  - [ ]* 13.9 Write property test for order closure on full payment
    - **Property 26: Order Closure on Full Payment**
    - **Validates: Requirements 7.5**
  
  - [ ]* 13.10 Write property test for fiscal document sequential numbering
    - **Property 27: Fiscal Document Sequential Numbering**
    - **Validates: Requirements 7.6**
  
  - [ ]* 13.11 Write property test for invoice NIF requirement
    - **Property 28: Invoice NIF Requirement**
    - **Validates: Requirements 7.7**
  
  - [ ]* 13.12 Write property test for audit trail on sensitive operations
    - **Property 29: Audit Trail on Sensitive Operations**
    - **Validates: Requirements 7.8**
  
  - [ ]* 13.13 Write property test for cash payment change calculation
    - **Property 30: Cash Payment Change Calculation**
    - **Validates: Requirements 7.9**

- [ ] 14. Customers module
  - [x] 14.1 Create Customer entity
    - Define Customer JPA entity: id, tenant_id, name, phone, address, delivery_notes
    - Create CustomerRepository with phone search methods (full and suffix)
    - Add index for phone suffix search (varchar_pattern_ops)
    - _Requirements: 8.1, 8.5_
  
  - [x] 14.2 Implement CustomerService
    - searchByPhone(phone): search by full phone number
    - searchByPhoneSuffix(suffix): search by last N digits using LIKE '%suffix'
    - createCustomer(tenantId, customerDetails): create new customer
    - updateCustomer(customerId, updates): modify customer
    - getOrderHistory(customerId): get customer's previous orders
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [x] 14.3 Create REST controllers for customers
    - GET /api/customers/search?phone={phone}: search by phone
    - GET /api/customers/search?suffix={suffix}: search by suffix
    - POST /api/customers: create customer
    - PUT /api/customers/{id}: update customer
    - GET /api/customers/{id}/orders: get order history
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ]* 14.4 Write property test for customer phone suffix search
    - **Property 31: Customer Phone Suffix Search**
    - **Validates: Requirements 8.1**
  
  - [ ]* 14.5 Write property test for customer search completeness
    - **Property 32: Customer Search Completeness**
    - **Validates: Requirements 8.2**
  
  - [ ]* 14.6 Write property test for customer order history completeness
    - **Property 33: Customer Order History Completeness**
    - **Validates: Requirements 8.4**

- [ ] 15. Cash register module
  - [x] 15.1 Create cash register entities
    - Define CashRegister JPA entity: id, tenant_id, site_id, register_number, status
    - Define CashSession JPA entity: id, tenant_id, register_id, employee_id, opening_amount, expected_close, actual_close, variance, status, opened_at, closed_at, version
    - Define CashMovement JPA entity: id, tenant_id, session_id, movement_type, amount, reason, payment_id, created_at, created_by
    - Define CashClosing JPA entity: id, tenant_id, closing_type, period_start, period_end, total_sales, total_refunds, variance, closed_at, closed_by
    - Create repositories with tenant filtering
    - _Requirements: 10.1, 10.2, 10.3, 10.6_
  
  - [x] 15.2 Implement CashSessionService
    - openSession(registerId, employeeId, openingAmount): start session
    - closeSession(sessionId, actualAmount): close session, calculate variance
    - recordMovement(sessionId, type, amount, reason): record manual movement
    - getSessionSummary(sessionId): get session with all movements
    - _Requirements: 10.2, 10.3, 10.5, 10.6_
  
  - [x] 15.3 Implement PaymentCompleted event listener
    - Listen to PaymentCompleted event from payments module
    - Create cash movement record for CASH payments
    - _Requirements: 10.4_
  
  - [x] 15.4 Implement CashClosingService
    - closeRegister(registerId, periodStart, periodEnd): close register
    - closeDay(siteId, date): close day
    - closeFinancialPeriod(tenantId, periodStart, periodEnd): close period
    - generateClosingReport(closingId): generate report
    - reprintClosingReport(closingId): reprint with permission check
    - _Requirements: 10.7, 10.8, 10.9_
  
  - [x] 15.5 Create REST controllers for cash register
    - POST /api/cash/sessions: open session
    - POST /api/cash/sessions/{id}/close: close session
    - POST /api/cash/sessions/{id}/movements: record movement
    - GET /api/cash/sessions/{id}: get session summary
    - POST /api/cash/closings/register: close register
    - POST /api/cash/closings/day: close day
    - GET /api/cash/closings/{id}/report: get closing report
    - POST /api/cash/closings/{id}/reprint: reprint report
    - _Requirements: 10.2, 10.3, 10.5, 10.6, 10.7, 10.8, 10.9_
  
  - [ ]* 15.6 Write property test for cash session opening
    - **Property 35: Cash Session Opening**
    - **Validates: Requirements 10.2**
  
  - [ ]* 15.7 Write property test for cash movement on payment
    - **Property 36: Cash Movement on Payment**
    - **Validates: Requirements 10.4**
  
  - [ ]* 15.8 Write property test for cash session variance calculation
    - **Property 37: Cash Session Variance Calculation**
    - **Validates: Requirements 10.6**
  
  - [ ]* 15.9 Write property test for cash report completeness
    - **Property 38: Cash Report Completeness**
    - **Validates: Requirements 10.8**

- [x] 16. Checkpoint - Core functionality complete
  - Ensure all tests pass
  - Verify complete order flow: open table → add lines → confirm → print jobs → payment → cash movement → close order
  - Verify cash session lifecycle works correctly
  - Ask user if questions arise

### Phase 4: Cross-Cutting Concerns and Robustness

- [ ] 17. Error handling and validation
  - [x] 17.1 Implement global exception handler
    - Create @ControllerAdvice with exception handlers
    - Return RFC 7807 Problem Details format for all errors
    - Handle: validation errors (400), authorization errors (403), not found (404), conflicts (409), business rule violations (422), server errors (500)
    - Include traceId in all error responses
    - _Requirements: 13.1_
  
  - [x] 17.2 Add input validation annotations
    - Add @Valid and Bean Validation annotations to all DTOs
    - Validate: required fields, formats, ranges, patterns
    - _Requirements: 13.1_
  
  - [ ]* 17.3 Write property test for input validation
    - **Property 42: Input Validation**
    - **Validates: Requirements 13.1**
  
  - [ ]* 17.4 Write property test for transaction rollback on failure
    - **Property 40: Transaction Rollback on Failure**
    - **Validates: Requirements 12.4**
  
  - [ ]* 17.5 Write property test for fiscal document number uniqueness
    - **Property 41: Fiscal Document Number Uniqueness**
    - **Validates: Requirements 12.5**


- [ ] 18. Security hardening
  - [x] 18.1 Implement SQL injection prevention
    - Verify all queries use parameterized statements (JPA/JPQL)
    - Add input sanitization for any raw SQL
    - _Requirements: 13.2_
  
  - [x] 18.2 Implement sensitive data masking in logs
    - Create LogMaskingFilter to mask passwords, card numbers, NIF
    - Configure logback with masking patterns
    - _Requirements: 13.3_
  
  - [x] 18.3 Implement rate limiting for authentication
    - Create RateLimitingFilter for /api/auth/** endpoints
    - Use in-memory cache (Caffeine) to track attempts per IP/username
    - Limit: 5 attempts per minute
    - _Requirements: 13.4_
  
  - [x] 18.4 Implement CSRF protection
    - Configure Spring Security CSRF for state-changing operations
    - Exclude /api/auth/** from CSRF (stateless JWT)
    - _Requirements: 13.6_
  
  - [ ]* 18.5 Write property test for SQL injection prevention
    - **Property 43: SQL Injection Prevention**
    - **Validates: Requirements 13.2**
  
  - [ ]* 18.6 Write property test for sensitive data masking in logs
    - **Property 44: Sensitive Data Masking in Logs**
    - **Validates: Requirements 13.3**
  
  - [ ]* 18.7 Write property test for authentication rate limiting
    - **Property 45: Authentication Rate Limiting**
    - **Validates: Requirements 13.4**
  
  - [ ]* 18.8 Write property test for CSRF token validation
    - **Property 46: CSRF Token Validation**
    - **Validates: Requirements 13.6**

- [ ] 19. Observability and monitoring
  - [x] 19.1 Configure structured logging
    - Configure Logback with JSON encoder
    - Add MDC fields: tenant_id, user_id, trace_id
    - Create LoggingFilter to populate MDC from request context
    - _Requirements: 14.1, 14.2, 14.6_
  
  - [x] 19.2 Implement API request logging
    - Create RequestLoggingFilter to log all API requests
    - Log: method, path, status_code, duration_ms, tenant_id
    - _Requirements: 14.2_
  
  - [x] 19.3 Configure Micrometer metrics
    - Add Micrometer dependencies
    - Expose metrics endpoint: /actuator/metrics
    - Configure metrics: request rates, error rates, response times, DB connection pool
    - _Requirements: 14.3_
  
  - [x] 19.4 Implement health checks
    - Configure Spring Boot Actuator
    - Add custom health indicators: database connectivity, disk space
    - Expose /actuator/health endpoint
    - _Requirements: 14.4_
  
  - [ ]* 19.5 Write property test for structured log format
    - **Property 47: Structured Log Format**
    - **Validates: Requirements 14.1**
  
  - [ ]* 19.6 Write property test for API request logging
    - **Property 48: API Request Logging**
    - **Validates: Requirements 14.2**
  
  - [ ]* 19.7 Write property test for correlation ID propagation
    - **Property 49: Correlation ID Propagation**
    - **Validates: Requirements 14.6**

- [x] 20. Checkpoint - Robustness complete
  - Ensure all tests pass
  - Verify error handling returns proper Problem Details format
  - Verify logs are in JSON format with all required fields
  - Verify metrics are exposed and health checks work
  - Ask user if questions arise

### Phase 5: Integrations and Advanced Features

- [ ] 21. SAF-T PT fiscal export (optional)
  - [x] 21.1 Implement SAF-T export service
    - Create SaftExportService with generateExport(tenantId, startDate, endDate) method
    - Query all fiscal documents, payments, customers, items in date range
    - Generate XML according to SAF-T PT schema
    - _Requirements: 17.1, 17.2_
  
  - [x] 21.2 Implement XML schema validation
    - Load SAF-T PT XSD schema
    - Validate generated XML before returning
    - _Requirements: 17.3_
  
  - [x] 21.3 Create REST controller for export
    - POST /api/exports/saft-pt: generate export with date range
    - Return XML file as download
    - Log export operation with audit trail
    - _Requirements: 17.1, 17.4, 17.5_
  
  - [ ]* 21.4 Write property test for SAF-T export completeness
    - **Property 50: SAF-T Export Completeness**
    - **Validates: Requirements 17.1**
  
  - [ ]* 21.5 Write property test for SAF-T XML schema validation
    - **Property 51: SAF-T XML Schema Validation**
    - **Validates: Requirements 17.3**

- [ ] 22. Payment terminal integration (optional)
  - [ ] 22.1 Define payment provider plugin interface
    - Create PaymentProviderPlugin interface with methods: processSale, processRefund, processVoid
    - Create PaymentTerminalResponse DTO with status, transactionId, errorMessage
    - _Requirements: 18.1, 18.2_
  
  - [ ] 22.2 Implement mock payment provider for testing
    - Create MockPaymentProvider implementing PaymentProviderPlugin
    - Simulate approved, declined, timeout, error responses
    - _Requirements: 18.2, 18.3_
  
  - [ ] 22.3 Integrate payment provider into PaymentService
    - When payment_method is CARD, call payment provider plugin
    - Handle all response types: approved, declined, timeout, error
    - Store terminal_transaction_id with payment
    - _Requirements: 18.2, 18.3, 18.5_
  
  - [ ]* 22.4 Write property test for payment terminal response handling
    - **Property 52: Payment Terminal Response Handling**
    - **Validates: Requirements 18.3**
  
  - [ ]* 22.5 Write property test for payment terminal transaction ID storage
    - **Property 53: Payment Terminal Transaction ID Storage**
    - **Validates: Requirements 18.5**

- [ ] 23. Reservations module (optional)
  - [ ] 23.1 Create Reservation entity
    - Define Reservation JPA entity: id, tenant_id, site_id, reservation_date, reservation_time, party_size, customer_name, customer_phone, notes, status, table_ids (array)
    - Create ReservationRepository with tenant filtering
    - _Requirements: 9.1, 9.3_
  
  - [ ] 23.2 Implement ReservationService
    - createReservation(siteId, reservationDetails): create reservation
    - updateReservation(reservationId, updates): modify reservation
    - cancelReservation(reservationId, reason): cancel with reason
    - getReservations(siteId, date): get reservations for date
    - _Requirements: 9.1, 9.5, 9.6_
  
  - [ ] 23.3 Create REST controllers for reservations
    - POST /api/reservations: create reservation
    - PUT /api/reservations/{id}: update reservation
    - POST /api/reservations/{id}/cancel: cancel reservation
    - GET /api/reservations: get reservations (filter by date)
    - _Requirements: 9.1, 9.2, 9.5, 9.6_
  
  - [ ]* 23.4 Write property test for reservation cancellation reason
    - **Property 34: Reservation Cancellation Reason**
    - **Validates: Requirements 9.6**

- [ ] 24. Integration tests with Testcontainers
  - [x] 24.1 Set up Testcontainers infrastructure
    - Create base integration test class with PostgreSQL container
    - Configure test application properties
    - Create test data builders for all entities
    - _Requirements: Testing Strategy_
  
  - [x] 24.2 Write integration test for complete order flow
    - Test: open table → add order lines → confirm order → create print jobs → process payment → close order → verify table available
    - _Requirements: Testing Strategy_
  
  - [x] 24.3 Write integration test for partial payment and split bill
    - Test: create order → add multiple partial payments → verify order remains open → complete final payment → verify order closed
    - _Requirements: Testing Strategy_
  
  - [x] 24.4 Write integration test for cash session lifecycle
    - Test: open session → record sales (via payments) → record manual movements → close session → verify variance calculation → generate report
    - _Requirements: Testing Strategy_
  
  - [x] 24.5 Write integration test for tenant isolation
    - Test: create data for Tenant A and Tenant B → query as Tenant A user → verify no Tenant B data returned → query as Tenant B user → verify no Tenant A data returned
    - _Requirements: Testing Strategy_

- [ ] 25. Spring Modulith verification
  - [ ] 25.1 Write module boundary tests
    - Create test to verify module dependencies match design
    - Use Spring Modulith's ApplicationModules.verify()
    - Ensure no circular dependencies
    - _Requirements: Architecture_
  
  - [ ] 25.2 Document module structure
    - Generate module documentation using Spring Modulith
    - Create architecture decision records (ADRs) for key decisions
    - _Requirements: Architecture_

- [ ] 26. Final checkpoint and documentation
  - Ensure all tests pass (unit, property, integration)
  - Verify all 53 correctness properties are implemented and passing
  - Run full test suite with coverage report
  - Create README.md with "How to run locally" instructions
  - Create API documentation (OpenAPI/Swagger)
  - Ask user if questions arise

## Notes

- Tasks marked with `*` are optional property-based tests that can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at major milestones
- Property tests validate universal correctness properties with minimum 100 iterations
- Integration tests verify critical end-to-end flows with real database
- All code uses Java 21, Spring Boot 3.x, and follows Spring Modulith architecture
