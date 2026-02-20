# Architecture Documentation

This directory contains comprehensive architecture documentation for the Restaurant POS SaaS system.

## Documentation Index

### Core Architecture Documents

1. **[Module Structure](./module-structure.md)**
   - Complete module inventory and descriptions
   - Module dependency graph
   - Communication patterns
   - Module boundaries enforcement
   - Testing strategy
   - Benefits and migration path

2. **[Architecture Decision Records (ADRs)](./adr/)**
   - [ADR-0001: Use Spring Modulith for Modular Monolith](./adr/0001-use-spring-modulith-for-modular-monolith.md)
   - [ADR-0002: Multi-Tenancy with Shared Database](./adr/0002-multi-tenancy-with-shared-database.md)
   - [ADR-0003: Event-Driven Module Communication](./adr/0003-event-driven-module-communication.md)
   - [ADR-0004: Optimistic Locking for Concurrency](./adr/0004-optimistic-locking-for-concurrency.md)

### Specification Documents

Located in `.kiro/specs/restaurant-pos-saas/`:

1. **[Requirements Document](../../.kiro/specs/restaurant-pos-saas/requirements.md)**
   - Functional requirements organized by domain
   - Acceptance criteria for each requirement
   - Glossary of domain terms

2. **[Design Document](../../.kiro/specs/restaurant-pos-saas/design.md)**
   - Detailed component design
   - Data models and state machines
   - Correctness properties
   - API specifications

3. **[Implementation Tasks](../../.kiro/specs/restaurant-pos-saas/tasks.md)**
   - Phased implementation plan
   - Task breakdown with dependencies
   - Property-based testing tasks
   - Checkpoints and milestones

## Architecture Overview

### System Type

**Modular Monolith** using Spring Modulith

### Key Characteristics

- **Single Deployment**: One deployable artifact
- **Module Boundaries**: Enforced at compile time
- **Event-Driven**: Loose coupling via domain events
- **Multi-Tenant**: Shared database with tenant isolation
- **Concurrent**: Optimistic locking for multi-terminal operations

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Architecture**: Spring Modulith
- **Database**: PostgreSQL 15+ with optional RLS
- **Migrations**: Flyway
- **Testing**: JUnit 5, Testcontainers, QuickTheories
- **Observability**: Micrometer, Logback
- **Security**: Spring Security, JWT

## Module Organization

### Core Business Modules (9)

1. **tenantprovisioning** - Tenant onboarding
2. **identityaccess** - Authentication and authorization
3. **diningroom** - Table management
4. **catalog** - Menu structure
5. **orders** - Order processing
6. **kitchenprinting** - Print job management
7. **paymentsbilling** - Payment and fiscal documents
8. **customers** - Customer data
9. **cashregister** - Cash operations

### Supporting Modules (2)

10. **common** - Cross-cutting concerns
11. **fiscalexport** - Tax compliance

## Key Architectural Decisions

### 1. Modular Monolith (ADR-0001)

**Decision**: Use Spring Modulith for modular monolith architecture

**Rationale**:
- Enforced module boundaries prevent tight coupling
- Single deployment simplifies operations
- Provides migration path to microservices
- Better than traditional monolith, simpler than microservices

### 2. Shared Database Multi-Tenancy (ADR-0002)

**Decision**: Shared database with tenant_id column and optional RLS

**Rationale**:
- Operational simplicity with single database
- Cost-effective resource sharing
- Easy schema migrations
- Defense-in-depth with optional RLS

### 3. Hybrid Communication (ADR-0003)

**Decision**: Direct calls for sync, events for async

**Rationale**:
- Loose coupling via events where appropriate
- Synchronous responses where needed
- Clear API contracts with named interfaces
- Natural asynchrony for background work

### 4. Optimistic Locking (ADR-0004)

**Decision**: Version-based concurrency control

**Rationale**:
- No blocking, better performance
- Scalable to many concurrent users
- Simple implementation with JPA
- Suitable for POS workload

## Communication Patterns

### Direct Service Calls

Used for synchronous operations:
- orders → catalog (validate items)
- paymentsbilling → orders (check status)
- All → identityaccess (check permissions)

### Domain Events

Used for asynchronous operations:
- orders → kitchenprinting (OrderConfirmed)
- paymentsbilling → cashregister (PaymentCompleted)

### Named Interfaces

Explicit API contracts:
- identityaccess::api (authorization)
- orders::event (order events)

## Data Architecture

### Multi-Tenancy

- All domain tables include `tenant_id` column
- Composite indexes: `(tenant_id, <business_key>)`
- TenantContext thread-local for current tenant
- TenantAspect enforces filtering on repositories
- Optional PostgreSQL RLS for defense-in-depth

### Concurrency Control

- Optimistic locking on critical entities
- Version column managed by JPA
- Conflict handling with retry or user notification
- No blocking, better performance

### Audit Trail

All transactional tables include:
- `created_at`, `created_by`
- `updated_at`, `updated_by`
- `version` (for optimistic locking)

## Security Architecture

### Authentication

- JWT-based authentication
- Access tokens (15 min expiry)
- Refresh tokens (7 day expiry)
- Token includes tenant_id claim

### Authorization

- Role-based access control (RBAC)
- Permission checks via AuthorizationService
- @RequirePermission annotation
- Roles: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN_STAFF

### Data Protection

- Tenant isolation at application and database level
- SQL injection prevention
- Sensitive data masking in logs
- Rate limiting on authentication
- CSRF protection

## Observability

### Logging

- Structured JSON logs
- MDC fields: tenant_id, user_id, trace_id
- Request/response logging
- Sensitive data masking

### Metrics

- Request rates and response times
- Error rates by endpoint
- Database connection pool metrics
- Custom business metrics

### Health Checks

- Application health
- Database connectivity
- Disk space

## Testing Strategy

### Unit Tests

- Service layer logic
- Business rule validation
- Isolated component testing

### Integration Tests

- Full stack with Testcontainers
- Database interactions
- Multi-module flows

### Property-Based Tests

- Correctness properties from design
- QuickTheories for property testing
- Tenant isolation properties
- Concurrency properties

### Module Structure Tests

- No circular dependencies
- Allowed dependencies respected
- Named interfaces properly exposed

## Development Workflow

### Adding a New Feature

1. Update requirements document
2. Update design document
3. Add tasks to implementation plan
4. Implement in appropriate module
5. Add tests (unit, integration, property-based)
6. Verify module boundaries
7. Update documentation

### Creating a New Module

1. Create package structure
2. Add package-info.java with module metadata
3. Define named interfaces if needed
4. Implement module components
5. Add module tests
6. Update module structure documentation
7. Create ADR if significant decision

## Deployment

### Single Artifact

- One JAR file contains all modules
- Simple deployment process
- No distributed system complexity

### Database Migrations

- Flyway manages schema versions
- Migrations run on startup
- Versioned migration files

### Configuration

- application.yml for configuration
- Environment-specific profiles
- Externalized secrets

## Future Considerations

### Scaling Options

1. **Vertical Scaling**: Increase server resources
2. **Read Replicas**: For read-heavy workloads
3. **Database Sharding**: By tenant_id if needed
4. **Module Extraction**: Convert modules to microservices

### Migration to Microservices

If needed, the modular structure enables:
1. Extract module to separate service
2. Convert events to message queue
3. Convert API calls to REST/gRPC
4. Deploy independently

## References

### Internal Documents

- [Module Structure](./module-structure.md)
- [ADR Index](./adr/README.md)
- [Requirements](../../.kiro/specs/restaurant-pos-saas/requirements.md)
- [Design](../../.kiro/specs/restaurant-pos-saas/design.md)
- [Tasks](../../.kiro/specs/restaurant-pos-saas/tasks.md)

### External Resources

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Domain-Driven Design](https://martinfowler.com/tags/domain%20driven%20design.html)
- [Modular Monolith Primer](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer)
- [PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)

## Questions and Support

For questions about the architecture:

1. Review relevant documentation
2. Check ADRs for decision context
3. Discuss with the team
4. Update documentation if needed

## Contributing to Documentation

When updating architecture documentation:

1. Keep documents synchronized
2. Update ADRs for significant decisions
3. Link related documents
4. Use clear, concise language
5. Include diagrams where helpful
6. Review with team before committing
