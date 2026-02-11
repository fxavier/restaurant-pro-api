# Requirements Document: Restaurant POS SaaS

## Introduction

This document specifies the requirements for a production-ready SaaS Multi-tenant Restaurant Front Office Point of Sale (POS) system. The system enables restaurant operations including table management, order processing, kitchen coordination, payment handling, reservations, delivery orders, and cash register management across multiple tenants in a shared infrastructure.

The system is designed as a modular monolith using Spring Modulith architecture with PostgreSQL for data persistence, supporting real-time multi-terminal operations with proper tenant isolation and concurrency control.

## Glossary

- **System**: The Restaurant POS SaaS application
- **Tenant**: A restaurant organization using the system (multi-tenant SaaS model)
- **Site**: A physical restaurant location within a tenant (one tenant may have multiple sites)
- **Terminal**: A workstation/device running the POS application
- **Table**: A dining table in the restaurant that can be assigned orders
- **Order**: A collection of order lines representing items requested by customers
- **Order_Line**: An individual item within an order with quantity and modifiers
- **Consumption**: The confirmed state of an order after "Pedir" (order confirmation)
- **Print_Job**: A task to print order items to a specific printer/zone
- **Payment**: A transaction recording money received for an order
- **Cash_Session**: A work period for a specific employee on a specific register
- **Cash_Register**: A physical or logical register device for handling cash operations
- **Reservation**: A scheduled booking for a table at a specific date/time
- **Delivery_Order**: An order placed via phone for delivery or pickup
- **Customer**: A person or entity with contact information for delivery orders
- **Catalog**: The menu structure including families, subfamilies, and items
- **Fiscal_Document**: An invoice or receipt with legal/tax requirements
- **Blacklist_Entry**: A blocked table or payment card that cannot be used
- **Printer**: A physical printing device assigned to zones/categories
- **Zone**: A kitchen area or station that receives specific order items
- **Void**: Cancellation of an order or payment with optional waste tracking
- **Discount**: A price reduction applied to order lines or entire orders
- **Offer**: A promotional pricing rule applied automatically or manually
- **Subtotal**: An intermediate bill print before final payment
- **Split_Bill**: Division of an order's payment across multiple transactions
- **NIF**: Tax identification number for fiscal customer identification
- **RLS**: PostgreSQL Row Level Security for tenant data isolation
- **JWT**: JSON Web Token for authentication and authorization
- **Idempotency_Key**: A unique identifier to prevent duplicate operations

## Requirements

### Requirement 1: Multi-Tenant Infrastructure

**User Story:** As a SaaS operator, I want to support multiple restaurant tenants in a shared infrastructure, so that I can efficiently manage resources while ensuring complete data isolation.

#### Acceptance Criteria

1. THE System SHALL store all tenant data in a single PostgreSQL database with a shared schema
2. THE System SHALL include a tenant_id column on all domain tables to identify data ownership
3. THE System SHALL include an optional site_id column on relevant tables to support multi-location tenants
4. WHEN any data access operation occurs, THE System SHALL filter results by the authenticated user's tenant_id
5. WHERE PostgreSQL RLS is enabled, THE System SHALL enforce tenant isolation at the database level
6. WHEN a user authenticates, THE System SHALL include tenant_id in the JWT token claims
7. THE System SHALL maintain a server-side TenantContext for the current request's tenant
8. WHEN provisioning a new tenant, THE System SHALL create tenant configuration and initial data atomically

### Requirement 2: Identity and Access Management

**User Story:** As a system administrator, I want secure authentication and role-based access control, so that users can only perform actions appropriate to their role.

#### Acceptance Criteria

1. THE System SHALL authenticate users using JWT access tokens and refresh tokens
2. WHEN a user logs in with valid credentials, THE System SHALL issue an access token (15 min expiry) and refresh token (7 day expiry)
3. WHEN an access token expires, THE System SHALL allow token refresh using a valid refresh token
4. THE System SHALL support role-based permissions including: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN_STAFF
5. WHEN a user attempts a sensitive action, THE System SHALL verify the user has the required permission
6. THE System SHALL require specific permissions for: void after subtotal, apply discounts, reprint documents, redirect printers, close cash sessions, void invoices
7. THE System SHALL hash all passwords using bcrypt with appropriate work factor
8. THE System SHALL invalidate refresh tokens upon logout or security events

### Requirement 3: Dining Room and Table Management

**User Story:** As a waiter, I want to view and manage table status in real-time, so that I can efficiently serve customers and coordinate with other terminals.

#### Acceptance Criteria

1. THE System SHALL display a real-time table map showing all tables with their current status
2. THE System SHALL support table states: AVAILABLE, OCCUPIED, RESERVED, BLOCKED
3. WHEN a table state changes on any terminal, THE System SHALL reflect the update on all connected terminals within 2 seconds
4. WHEN a user opens a table, THE System SHALL transition the table to OCCUPIED state
5. WHEN a user transfers an order from one table to another, THE System SHALL move all order lines and update both table states
6. WHEN a table is in the blacklist, THE System SHALL prevent any operations on that table and display a blocked indicator
7. THE System SHALL support grouping tables by area or zone for organizational purposes
8. WHEN all orders on a table are paid and closed, THE System SHALL transition the table to AVAILABLE state

### Requirement 4: Menu Catalog Management

**User Story:** As a manager, I want to organize menu items into families and subfamilies, so that staff can quickly find and order items.

#### Acceptance Criteria

1. THE System SHALL organize catalog items in a three-level hierarchy: Family → Subfamily → Item
2. THE System SHALL support quick pages for frequently ordered items accessible with minimal navigation
3. WHEN an item is marked as unavailable, THE System SHALL prevent ordering that item and display an indicator
4. THE System SHALL support item modifiers and variants (size, preparation notes, extras)
5. THE System SHALL maintain item pricing with support for multiple price levels (regular, happy hour, etc.)
6. WHEN catalog changes are made, THE System SHALL apply changes immediately to new orders without affecting existing orders
7. THE System SHALL support item images for touchscreen display

### Requirement 5: Order Creation and Management

**User Story:** As a waiter, I want to create and modify orders for tables, so that I can accurately capture customer requests.

#### Acceptance Criteria

1. WHEN a user adds items to a table, THE System SHALL create order lines in PENDING state
2. THE System SHALL support order line modifications: quantity changes, modifiers, preparation notes
3. WHEN a user confirms an order (Pedir), THE System SHALL transition order lines from PENDING to CONFIRMED state
4. WHEN order lines are confirmed, THE System SHALL create consumption records for sales tracking
5. THE System SHALL support voiding order lines before confirmation without requiring special permissions
6. WHEN a user voids a confirmed order line, THE System SHALL require appropriate permissions and optionally record waste
7. THE System SHALL support applying discounts to individual order lines or entire orders
8. THE System SHALL support applying promotional offers manually or automatically based on rules
9. THE System SHALL use optimistic locking on order entities to prevent concurrent modification conflicts

### Requirement 6: Kitchen Coordination and Printing

**User Story:** As kitchen staff, I want to receive printed orders for items in my zone, so that I can prepare food efficiently.

#### Acceptance Criteria

1. WHEN an order is confirmed, THE System SHALL create print jobs for each item based on printer/zone assignments
2. THE System SHALL route print jobs to the appropriate printer based on item category and zone configuration
3. THE System SHALL support printer states: NORMAL, WAIT, IGNORE, REDIRECT
4. WHEN a printer is in REDIRECT mode, THE System SHALL send print jobs to an alternate configured printer
5. WHEN a printer is in IGNORE mode, THE System SHALL skip printing for that printer's jobs
6. THE System SHALL support manual reprint of order items with appropriate permissions
7. THE System SHALL use a dedupe_key on print jobs to prevent duplicate printing on retry
8. WHEN print jobs are created, THE System SHALL include table number, item details, quantity, modifiers, and timestamp

### Requirement 7: Payment Processing and Billing

**User Story:** As a cashier, I want to process payments using multiple methods and generate fiscal documents, so that I can close orders and comply with regulations.

#### Acceptance Criteria

1. THE System SHALL support payment methods: CASH, CARD, MOBILE, VOUCHER, MIXED
2. WHEN processing a payment, THE System SHALL accept an Idempotency-Key header to prevent duplicate charges
3. THE System SHALL support partial payments where multiple transactions contribute to an order total
4. THE System SHALL support bill splitting where an order is divided into multiple separate payments
5. WHEN an order is fully paid, THE System SHALL transition the order to CLOSED state
6. THE System SHALL generate fiscal documents (invoices/receipts) with sequential numbering per tenant
7. WHEN a customer requests an invoice, THE System SHALL capture and store the customer's NIF (tax ID)
8. THE System SHALL support voiding payments and fiscal documents with appropriate permissions and audit trail
9. THE System SHALL calculate and display change amount for cash payments
10. WHEN a payment card is blacklisted, THE System SHALL reject any payment attempts using that card

### Requirement 8: Customer Management for Delivery

**User Story:** As a phone order taker, I want to search for customers by phone number and view their order history, so that I can quickly process delivery orders.

#### Acceptance Criteria

1. THE System SHALL support searching customers by full phone number or suffix (last N digits)
2. WHEN a phone search matches multiple customers, THE System SHALL display all matches with names and addresses
3. WHEN creating a delivery order, THE System SHALL allow creating a new customer record if not found
4. THE System SHALL display customer order history showing previous orders with dates and items
5. THE System SHALL store customer information: name, phone, address, delivery notes
6. THE System SHALL support marking delivery orders with estimated delivery time
7. WHEN a delivery order is created, THE System SHALL follow the same order confirmation and payment flow as dine-in orders

### Requirement 9: Reservation Management

**User Story:** As a host, I want to manage table reservations, so that I can plan seating and inform customers of availability.

#### Acceptance Criteria

1. THE System SHALL support creating reservations with: date, time, number of people, customer name, phone, notes
2. THE System SHALL display reservations in a calendar/schedule view
3. THE System SHALL allow associating one or more tables with a reservation
4. WHEN a reservation time arrives, THE System SHALL display a notification but NOT automatically block table operations
5. THE System SHALL support editing reservation details: time, party size, table assignment
6. THE System SHALL support canceling reservations with a reason note
7. THE System SHALL maintain reservation history for reporting purposes

### Requirement 10: Cash Register Management

**User Story:** As a manager, I want to track cash movements and perform register closings, so that I can maintain accurate financial records and detect discrepancies.

#### Acceptance Criteria

1. THE System SHALL organize cash operations in a hierarchy: Day → Shift → Register → Session
2. WHEN an employee starts work, THE System SHALL open a cash session with an initial cash amount
3. THE System SHALL record all cash movements: sales, refunds, deposits, withdrawals, with reason codes
4. WHEN a payment is completed, THE System SHALL automatically create a cash movement record
5. THE System SHALL support manual cash movements (deposits, withdrawals) with required reason and amount
6. WHEN closing a cash session, THE System SHALL calculate expected vs actual cash and record variance
7. THE System SHALL support closing at multiple levels: session close, register close, day close, financial period close
8. THE System SHALL generate cash reports showing: opening balance, movements, expected close, actual close, variance
9. THE System SHALL support reprinting previous cash closing reports with appropriate permissions
10. THE System SHALL maintain a complete audit trail of all cash operations with timestamps and user identification

### Requirement 11: System Administration and Printer Control

**User Story:** As a system administrator, I want to control printer behavior and test hardware, so that I can troubleshoot issues and maintain operations.

#### Acceptance Criteria

1. THE System SHALL support changing printer state in real-time: NORMAL, WAIT, IGNORE, REDIRECT, REPRINT
2. WHEN a printer state is changed, THE System SHALL apply the change to all subsequent print jobs immediately
3. THE System SHALL support redirecting a printer's jobs to another printer temporarily
4. THE System SHALL support test printing to verify printer connectivity and configuration
5. THE System SHALL display a list of all configured printers with their current status
6. THE System SHALL support locking a workstation to prevent unauthorized access
7. THE System SHALL log all printer state changes with user and timestamp for audit purposes

### Requirement 12: Concurrency and Data Integrity

**User Story:** As a system architect, I want to prevent data corruption from concurrent operations, so that the system maintains consistency across multiple terminals.

#### Acceptance Criteria

1. THE System SHALL use optimistic locking on critical entities: Order, Table, Cash_Session, Payment
2. WHEN a concurrent modification conflict occurs, THE System SHALL reject the stale update and return an error
3. THE System SHALL wrap each use case service method in a database transaction
4. WHEN a transaction fails, THE System SHALL roll back all changes and return a clear error message
5. THE System SHALL use database-level unique constraints to prevent duplicate fiscal document numbers
6. THE System SHALL use database-level unique constraints to prevent duplicate idempotency keys within a time window

### Requirement 13: Security and Compliance

**User Story:** As a security officer, I want the system to protect sensitive data and comply with security best practices, so that we minimize risk and meet regulatory requirements.

#### Acceptance Criteria

1. THE System SHALL validate all input data against expected formats and ranges
2. THE System SHALL sanitize all user input to prevent SQL injection and XSS attacks
3. THE System SHALL mask sensitive data in logs: passwords, payment card numbers, full NIF
4. THE System SHALL implement rate limiting on authentication endpoints to prevent brute force attacks
5. THE System SHALL use HTTPS for all API communications in production
6. THE System SHALL implement CSRF protection for state-changing operations
7. THE System SHALL maintain audit logs for sensitive operations: voids, discounts, cash movements, document reprints
8. WHEN storing fiscal customer data (NIF), THE System SHALL comply with data protection regulations

### Requirement 14: Observability and Monitoring

**User Story:** As a DevOps engineer, I want structured logging and metrics, so that I can monitor system health and troubleshoot issues.

#### Acceptance Criteria

1. THE System SHALL emit structured logs in JSON format with: timestamp, level, tenant_id, user_id, trace_id, message
2. THE System SHALL log all API requests with: method, path, status code, duration, tenant_id
3. THE System SHALL expose metrics via Micrometer for: request rates, error rates, response times, database connection pool
4. THE System SHALL expose health check endpoints for: application status, database connectivity
5. THE System SHALL log all business events: order confirmed, payment completed, cash session closed
6. THE System SHALL include correlation IDs in logs to trace requests across components

### Requirement 15: Performance and Scalability

**User Story:** As a system operator, I want the system to handle peak loads efficiently, so that restaurants can serve customers without delays.

#### Acceptance Criteria

1. WHEN querying the table map, THE System SHALL return results within 500ms for up to 100 tables
2. WHEN confirming an order, THE System SHALL complete the operation within 1 second including print job creation
3. THE System SHALL support at least 10 concurrent terminals per site without performance degradation
4. THE System SHALL use database indexes on: tenant_id, table state, order status, customer phone, cash session date
5. WHEN searching customers by phone suffix, THE System SHALL use an appropriate index to avoid full table scans
6. THE System SHALL use connection pooling with appropriate sizing for expected load

### Requirement 16: Data Migration and Versioning

**User Story:** As a database administrator, I want automated schema migrations, so that I can deploy updates safely and track schema changes.

#### Acceptance Criteria

1. THE System SHALL use Flyway for all database schema migrations
2. THE System SHALL version migrations sequentially: V1__baseline.sql, V2__add_indexes.sql, etc.
3. WHEN the application starts, THE System SHALL automatically apply pending migrations
4. THE System SHALL include auditing columns on all transactional tables: created_at, created_by, updated_at, updated_by, version
5. THE System SHALL maintain migration history in the Flyway schema history table
6. WHERE PostgreSQL RLS is used, THE System SHALL provide RLS policies as a separate optional migration

### Requirement 17: Fiscal Document Export

**User Story:** As an accountant, I want to export fiscal data in SAF-T PT format, so that I can comply with tax reporting requirements.

#### Acceptance Criteria

1. WHERE SAF-T PT export is enabled, THE System SHALL generate XML files containing all fiscal documents for a date range
2. THE System SHALL include in the export: invoices, receipts, payments, customer data, product data
3. THE System SHALL validate the generated XML against the SAF-T PT schema before export
4. THE System SHALL support filtering exports by tenant and date range
5. THE System SHALL log all export operations with user, timestamp, and date range for audit purposes

### Requirement 18: Payment Integration

**User Story:** As a cashier, I want to integrate with payment terminals, so that I can process card payments seamlessly.

#### Acceptance Criteria

1. WHERE payment integration is enabled, THE System SHALL support a plugin architecture for payment providers
2. WHEN processing a card payment, THE System SHALL communicate with the payment terminal via the configured plugin
3. THE System SHALL handle payment terminal responses: approved, declined, timeout, error
4. WHEN a payment terminal transaction fails, THE System SHALL allow retry or alternative payment method
5. THE System SHALL store payment terminal transaction IDs for reconciliation
6. THE System SHALL support payment terminal operations: sale, refund, void
