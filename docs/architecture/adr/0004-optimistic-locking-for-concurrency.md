# ADR 0004: Optimistic Locking for Concurrency Control

## Status

Accepted

## Context

The Restaurant POS system supports multiple terminals accessing the same data concurrently. We need to handle concurrent modifications to prevent data corruption and ensure consistency.

Common scenarios:
- Multiple waiters modifying the same order
- Multiple cashiers processing payments for the same order
- Multiple terminals updating table status
- Cash session updates from multiple sources

We considered three concurrency control strategies:

1. **No Locking**: Last write wins, potential data loss
2. **Pessimistic Locking**: Lock rows during read, blocks other transactions
3. **Optimistic Locking**: Version-based conflict detection, retry on conflict

## Decision

We will use **optimistic locking with version columns** on critical entities.

## Rationale

### Why Optimistic Locking?

**Characteristics:**
- Each entity has a `version` column (integer)
- Version increments on every update
- Update fails if version changed since read
- Application handles conflict by retrying or returning error

**Benefits:**
1. **No Blocking**: Reads don't block writes, writes don't block reads
2. **Better Performance**: No lock contention in database
3. **Scalability**: Supports high concurrency
4. **Deadlock-Free**: No lock acquisition ordering issues
5. **Simple Implementation**: JPA/Hibernate built-in support

**Trade-offs:**
- Conflicts must be handled by application
- May need retry logic
- Not suitable for high-contention scenarios

### Critical Entities with Optimistic Locking

1. **Order**: Multiple terminals may modify same order
2. **OrderLine**: Concurrent line modifications
3. **DiningTable**: Table status changes from multiple terminals
4. **Payment**: Prevent duplicate payment processing
5. **CashSession**: Multiple cash movements to same session

### Implementation

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    
    @Version
    private Integer version;  // Managed by JPA
    
    private UUID tenantId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    // ... other fields
}
```

**JPA automatically:**
- Includes version in WHERE clause: `WHERE id = ? AND version = ?`
- Increments version on update: `SET version = version + 1`
- Throws `OptimisticLockException` if version mismatch

### Conflict Handling

**Strategy 1: Return Error to User**
```java
@Service
public class OrderService {
    public void updateOrderLine(UUID orderLineId, int quantity) {
        try {
            OrderLine line = orderLineRepository.findById(orderLineId)
                .orElseThrow(() -> new ResourceNotFoundException("Order line not found"));
            
            line.setQuantity(quantity);
            orderLineRepository.save(line);
            
        } catch (OptimisticLockException e) {
            throw new ConflictException(
                "Order was modified by another user. Please refresh and try again."
            );
        }
    }
}
```

**Strategy 2: Automatic Retry**
```java
@Service
public class PaymentService {
    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public void processPayment(UUID orderId, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Process payment logic
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }
}
```

## Concurrency Scenarios

### Scenario 1: Concurrent Order Modifications

**Timeline:**
1. Terminal A reads Order (version=1)
2. Terminal B reads Order (version=1)
3. Terminal A updates Order → version=2 ✓
4. Terminal B updates Order → OptimisticLockException ✗

**Handling:**
- Terminal B receives 409 Conflict error
- User sees "Order was modified, please refresh"
- User refreshes and sees latest state
- User retries operation if still needed

### Scenario 2: Concurrent Payment Processing

**Timeline:**
1. Cashier A processes payment (version=1)
2. Cashier B processes payment (version=1)
3. Cashier A commits → version=2 ✓
4. Cashier B commits → OptimisticLockException ✗

**Handling:**
- Automatic retry with fresh data
- Second payment sees order already paid
- Returns appropriate error

### Scenario 3: Table Status Updates

**Timeline:**
1. Waiter A opens table (version=1)
2. Waiter B opens table (version=1)
3. Waiter A commits → version=2 ✓
4. Waiter B commits → OptimisticLockException ✗

**Handling:**
- Waiter B receives error
- UI refreshes table status
- Shows table already occupied

## Entities Without Optimistic Locking

Some entities don't need versioning:

1. **Read-Only Entities**: Catalog items (rarely updated)
2. **Append-Only Entities**: Audit logs, cash movements
3. **Low-Contention Entities**: Tenant configuration

## Testing Strategy

### Unit Tests

```java
@Test
void updateOrder_withStaleVersion_throwsOptimisticLockException() {
    // Given
    Order order = createOrder();
    UUID orderId = order.getId();
    
    // Simulate concurrent modification
    Order order1 = orderRepository.findById(orderId).get();
    Order order2 = orderRepository.findById(orderId).get();
    
    // When
    order1.setStatus(OrderStatus.CONFIRMED);
    orderRepository.save(order1);  // version=2
    
    order2.setStatus(OrderStatus.PAID);
    
    // Then
    assertThrows(OptimisticLockException.class, () -> {
        orderRepository.save(order2);  // Fails, still version=1
    });
}
```

### Integration Tests

```java
@Test
void concurrentOrderUpdates_oneSucceedsOneFailsWithConflict() {
    // Given
    UUID orderId = createOrder();
    
    // When - Simulate concurrent updates
    CompletableFuture<Void> future1 = CompletableFuture.runAsync(() ->
        orderService.updateOrderLine(orderId, lineId1, 5)
    );
    
    CompletableFuture<Void> future2 = CompletableFuture.runAsync(() ->
        orderService.updateOrderLine(orderId, lineId2, 3)
    );
    
    // Then - One should succeed, one should get conflict
    assertThatThrownBy(() -> CompletableFuture.allOf(future1, future2).join())
        .hasCauseInstanceOf(ConflictException.class);
}
```

## Consequences

### Positive

- No database lock contention
- Better performance under load
- Scalable to many concurrent users
- Simple implementation with JPA
- Clear error messages to users

### Negative

- Users may see conflict errors
- Need retry logic for some operations
- Not suitable for very high contention
- Requires user education

### Mitigation

- Clear error messages explaining conflict
- Automatic retry for idempotent operations
- UI refresh on conflict
- Monitor conflict rates
- Consider pessimistic locking for high-contention cases

## Monitoring

Track optimistic lock conflicts:

```java
@Aspect
@Component
public class OptimisticLockMonitoringAspect {
    private final MeterRegistry meterRegistry;
    
    @AfterThrowing(
        pointcut = "execution(* com.restaurantpos..service.*.*(..))",
        throwing = "ex"
    )
    public void monitorOptimisticLockExceptions(OptimisticLockException ex) {
        meterRegistry.counter("optimistic_lock_conflicts",
            "entity", extractEntityName(ex)
        ).increment();
    }
}
```

## Alternatives Considered

### 1. No Locking (Last Write Wins)

**Pros:**
- Simplest implementation
- No conflicts
- Maximum performance

**Cons:**
- Data loss on concurrent updates
- Silent corruption
- Unacceptable for financial data

**Rejected because:** Risk of data loss is unacceptable for POS system.

### 2. Pessimistic Locking

**Pros:**
- Guaranteed consistency
- No conflicts
- Simpler error handling

**Cons:**
- Lock contention
- Potential deadlocks
- Reduced concurrency
- Worse performance

**Rejected because:** Performance impact and deadlock risk outweigh benefits.

### 3. Distributed Locking (Redis)

**Pros:**
- Works across multiple instances
- Fine-grained control
- Can implement timeouts

**Cons:**
- Additional infrastructure
- Network dependency
- More complex
- Overkill for monolith

**Rejected because:** Unnecessary complexity for modular monolith.

## Future Considerations

If conflict rates become too high:

1. **Identify hot spots**: Monitor which entities have most conflicts
2. **Consider pessimistic locking**: For specific high-contention operations
3. **Redesign workflow**: Reduce concurrent access patterns
4. **Partition data**: Separate by site or register

## References

- [JPA Optimistic Locking](https://docs.oracle.com/javaee/7/tutorial/persistence-locking.htm)
- [Hibernate Locking](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#locking)
- [Requirements Document](../../.kiro/specs/restaurant-pos-saas/requirements.md) - Requirement 12
- [Design Document](../../.kiro/specs/restaurant-pos-saas/design.md)

## Date

2024-01-15

## Authors

Development Team
