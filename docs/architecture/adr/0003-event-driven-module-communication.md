# ADR 0003: Event-Driven Communication Between Modules

## Status

Accepted

## Context

In our modular monolith architecture, modules need to communicate with each other. We need to decide how modules should interact while maintaining loose coupling and clear boundaries.

We considered three communication patterns:

1. **Direct Service Calls**: Modules call each other's services directly
2. **Event-Driven Communication**: Modules publish domain events that other modules consume
3. **Hybrid Approach**: Direct calls for synchronous operations, events for asynchronous

## Decision

We will use a **hybrid approach**:
- **Direct service calls** for synchronous operations within allowed dependencies
- **Domain events** for asynchronous cross-module communication
- **Named interfaces** to expose explicit API contracts

## Rationale

### Event-Driven Communication

**Use Cases:**
1. **orders → kitchenprinting**: When order is confirmed, generate print jobs
2. **paymentsbilling → cashregister**: When payment completes, record cash movement

**Benefits:**
- Loose coupling between modules
- Publisher doesn't know about consumers
- Easy to add new consumers
- Natural asynchrony for non-blocking operations
- Clear audit trail of domain events

**Implementation:**
```java
// Publisher (orders module)
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void confirmOrder(UUID orderId) {
        // ... business logic
        eventPublisher.publishEvent(
            new OrderConfirmed(orderId, tenantId, orderLines)
        );
    }
}

// Consumer (kitchenprinting module)
@Component
public class OrderConfirmedEventListener {
    @EventListener
    @Transactional
    public void handleOrderConfirmed(OrderConfirmed event) {
        printingService.createPrintJobs(event.orderId());
    }
}
```

### Direct Service Calls

**Use Cases:**
1. **orders → catalog**: Validate item availability
2. **paymentsbilling → orders**: Check order status before payment
3. **All modules → identityaccess**: Check permissions

**Benefits:**
- Synchronous response needed
- Simpler error handling
- Type-safe API contracts
- IDE support for refactoring

**Implementation:**
```java
// Consumer (orders module)
@Service
public class OrderService {
    private final CatalogService catalogService;
    
    public void addOrderLine(UUID orderId, UUID itemId) {
        Item item = catalogService.getItem(itemId);
        if (!item.isAvailable()) {
            throw new BusinessRuleViolationException("Item not available");
        }
        // ... add order line
    }
}
```

### Named Interfaces

**Use Cases:**
1. **identityaccess::api**: Authorization API for all modules
2. **orders::event**: Order domain events for consumers

**Benefits:**
- Explicit API contracts
- Clear module boundaries
- Compile-time verification
- Documentation generation

**Implementation:**
```java
// package-info.java in identityaccess module
@org.springframework.modulith.ApplicationModule(
    displayName = "Identity and Access",
    allowedDependencies = {}
)
@org.springframework.modulith.NamedInterface("api")
package com.restaurantpos.identityaccess.api;

// package-info.java in orders module
@org.springframework.modulith.ApplicationModule(
    displayName = "Orders",
    allowedDependencies = {"identityaccess", "catalog", "diningroom"}
)
@org.springframework.modulith.NamedInterface("event")
package com.restaurantpos.orders.event;
```

## Event Design Principles

### 1. Domain Events are Immutable

```java
public record OrderConfirmed(
    UUID orderId,
    UUID tenantId,
    UUID siteId,
    List<OrderLineDto> orderLines,
    Instant confirmedAt
) {}
```

### 2. Events Contain Sufficient Data

Events should include enough data for consumers to act without additional queries:
- Include IDs for reference
- Include key business data
- Include tenant context
- Include timestamp

### 3. Events are Published After Transaction

Use `@TransactionalEventListener` to ensure events are only published if transaction commits:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderConfirmed(OrderConfirmed event) {
    // This only runs if the order confirmation transaction committed
}
```

### 4. Event Handlers are Idempotent

Event handlers should be safe to retry:
- Use dedupe keys where appropriate
- Check if work already done
- Use database constraints

## Communication Matrix

| From Module | To Module | Pattern | Reason |
|-------------|-----------|---------|--------|
| orders | kitchenprinting | Event | Async print job creation |
| paymentsbilling | cashregister | Event | Async cash movement |
| orders | catalog | Direct | Sync item validation |
| orders | diningroom | Direct | Sync table status |
| paymentsbilling | orders | Direct | Sync order status check |
| All | identityaccess | Direct | Sync permission check |

## Consequences

### Positive

- Loose coupling between modules
- Clear separation of concerns
- Easy to add new event consumers
- Natural asynchrony for background work
- Explicit API contracts with named interfaces

### Negative

- Eventual consistency for event-driven flows
- Need to handle event failures
- More complex debugging across events
- Need to version events carefully

### Mitigation

- Use transactional event listeners
- Implement idempotent event handlers
- Add comprehensive logging
- Use correlation IDs for tracing
- Document event contracts

## Event Versioning Strategy

When events need to change:

1. **Add new fields**: Safe, old consumers ignore them
2. **Remove fields**: Create new event version (e.g., `OrderConfirmedV2`)
3. **Change semantics**: Create new event type
4. **Deprecate old events**: Support both versions during transition

## Testing Strategy

### Event Publishing Tests

```java
@Test
void confirmOrder_publishesOrderConfirmedEvent() {
    // Given
    UUID orderId = createOrder();
    
    // When
    orderService.confirmOrder(orderId);
    
    // Then
    verify(eventPublisher).publishEvent(
        argThat(event -> event instanceof OrderConfirmed &&
                        ((OrderConfirmed) event).orderId().equals(orderId))
    );
}
```

### Event Handling Tests

```java
@Test
void handleOrderConfirmed_createsPrintJobs() {
    // Given
    OrderConfirmed event = new OrderConfirmed(orderId, tenantId, ...);
    
    // When
    listener.handleOrderConfirmed(event);
    
    // Then
    List<PrintJob> jobs = printJobRepository.findByOrderId(orderId);
    assertThat(jobs).isNotEmpty();
}
```

## Alternatives Considered

### 1. Only Direct Service Calls

**Pros:**
- Simpler to understand
- Synchronous flow
- Easier debugging

**Cons:**
- Tight coupling
- Circular dependencies risk
- Harder to add new consumers
- Blocking operations

**Rejected because:** Creates tight coupling and makes it hard to add new functionality.

### 2. Only Event-Driven

**Pros:**
- Maximum loose coupling
- Easy to add consumers
- Natural asynchrony

**Cons:**
- Eventual consistency everywhere
- Complex error handling
- Harder to reason about flow
- Overkill for simple queries

**Rejected because:** Too much complexity for synchronous operations.

## Migration to Message Queue

If we need to scale beyond in-process events:

1. Replace `ApplicationEventPublisher` with message queue client
2. Events already designed as immutable DTOs
3. Consumers already idempotent
4. Minimal code changes needed

## References

- [Spring Modulith Events](https://docs.spring.io/spring-modulith/reference/events.html)
- [Domain Events Pattern](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Design Document](../../.kiro/specs/restaurant-pos-saas/design.md)

## Date

2024-01-15

## Authors

Development Team
