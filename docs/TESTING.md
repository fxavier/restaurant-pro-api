# Testing Guide

## Overview

The Restaurant POS SaaS system includes comprehensive testing at multiple levels to ensure correctness, reliability, and maintainability.

## Test Categories

### 1. Unit Tests

Unit tests verify individual components in isolation using mocks and stubs.

**Location**: `src/test/java/com/restaurantpos/{module}/`

**Naming Convention**: `*Test.java`

**Examples**:
- `OrderServiceTest.java`
- `PaymentServiceTest.java`
- `CatalogManagementServiceTest.java`

**Run Unit Tests**:
```bash
mvn test -Dtest="*Test"
```

### 2. Integration Tests

Integration tests verify complete flows with real database using Testcontainers.

**Location**: `src/test/java/com/restaurantpos/`

**Naming Convention**: `*IntegrationTest.java`

**Examples**:
- `CompleteOrderFlowIntegrationTest.java`
- `PartialPaymentAndSplitBillIntegrationTest.java`
- `CashSessionLifecycleIntegrationTest.java`
- `TenantIsolationIntegrationTest.java`

**Run Integration Tests**:
```bash
mvn test -Dtest="*IntegrationTest"
```

**Requirements**:
- Docker must be running (for Testcontainers)
- Sufficient memory for PostgreSQL container

### 3. Property-Based Tests

Property-based tests verify universal properties across many randomly generated inputs using QuickTheories.

**Location**: `src/test/java/com/restaurantpos/{module}/`

**Naming Convention**: `*PropertyTest.java`

**Examples**:
- `TenantDataIsolationPropertyTest.java`
- `JwtTokenContainsTenantPropertyTest.java`
- `TokenIssuanceAndExpiryPropertyTest.java`

**Run Property-Based Tests**:
```bash
mvn test -Dtest="*PropertyTest"
```

**Characteristics**:
- Minimum 100 iterations per property
- Automatic shrinking of failing examples
- Tests universal invariants

### 4. Module Structure Tests

Verify Spring Modulith module boundaries and dependencies.

**Location**: `src/test/java/com/restaurantpos/ModularityTests.java`

**Run Module Tests**:
```bash
mvn test -Dtest=ModularityTests
```

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=OrderServiceTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=OrderServiceTest#createOrder_withValidData_createsOrder
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

### Skip Tests During Build

```bash
mvn clean install -DskipTests
```

### Run Tests in Parallel

```bash
mvn test -T 4
```

## Test Structure

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderLineRepository orderLineRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void createOrder_withValidData_createsOrder() {
        // Arrange
        UUID tableId = UUID.randomUUID();
        Order order = new Order();
        when(orderRepository.save(any())).thenReturn(order);
        
        // Act
        Order result = orderService.createOrder(tableId, OrderType.DINE_IN);
        
        // Assert
        assertNotNull(result);
        verify(orderRepository).save(any());
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@Testcontainers
class CompleteOrderFlowIntegrationTest extends BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    @Transactional
    void completeOrderFlow_fromCreationToPayment_succeeds() {
        // Create order
        Order order = orderService.createOrder(tableId, OrderType.DINE_IN);
        
        // Add items
        orderService.addOrderLine(order.getId(), itemId, 2, null);
        
        // Confirm order
        orderService.confirmOrder(order.getId());
        
        // Process payment
        Payment payment = paymentService.processPayment(
            order.getId(), 
            order.getTotalAmount(), 
            PaymentMethod.CASH,
            UUID.randomUUID().toString()
        );
        
        // Verify order closed
        Order closedOrder = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CLOSED, closedOrder.getStatus());
    }
}
```

### Property-Based Test Example

```java
class TenantDataIsolationPropertyTest {
    
    @Test
    void tenantDataIsolation_queriesNeverReturnOtherTenantData() {
        qt()
            .forAll(
                uuids().all(),  // tenantId
                uuids().all(),  // otherTenantId
                strings().allPossible().ofLengthBetween(1, 50)  // data
            )
            .assuming((tenantId, otherTenantId, data) -> 
                !tenantId.equals(otherTenantId)
            )
            .checkAssert((tenantId, otherTenantId, data) -> {
                // Create data for tenant A
                createDataForTenant(tenantId, data);
                
                // Query as tenant B
                List<Data> results = queryDataForTenant(otherTenantId);
                
                // Verify no data from tenant A is returned
                assertTrue(results.stream()
                    .noneMatch(d -> d.getTenantId().equals(tenantId)));
            });
    }
}
```

## Test Data Builders

The project includes test data builders for creating test entities:

**Location**: `src/test/java/com/restaurantpos/TestDataBuilder.java`

**Usage**:
```java
Tenant tenant = TestDataBuilder.createTenant("Test Tenant");
Site site = TestDataBuilder.createSite(tenant.getId(), "Test Site");
User user = TestDataBuilder.createUser(tenant.getId(), "testuser", Role.WAITER);
DiningTable table = TestDataBuilder.createTable(tenant.getId(), site.getId(), "T1");
```

## Correctness Properties

The system implements 53 correctness properties that are tested using property-based testing:

### Core Properties (Implemented)

1. **Tenant Data Isolation** - Queries never return other tenant's data
2. **PostgreSQL RLS Enforcement** - RLS policies enforce tenant isolation
3. **JWT Token Contains Tenant** - All JWT tokens include tenant_id claim
4. **Tenant Provisioning Atomicity** - Tenant creation is atomic
5. **Token Issuance and Expiry** - Tokens have correct expiry times

### Additional Properties (Specified in Design)

- Permission Enforcement
- Password Hashing
- Refresh Token Invalidation
- Table State Transitions
- Order Confirmation State Transition
- Consumption Record Creation
- Optimistic Locking Conflict Detection
- Print Job Creation on Order Confirmation
- Printer Redirect Routing
- Idempotency Key Enforcement
- Payment Processing
- Fiscal Document Sequential Numbering
- Cash Session Variance Calculation
- And 40+ more...

See `docs/architecture/correctness-properties.md` for complete list.

## Test Coverage Goals

- **Unit Tests**: 80%+ line coverage
- **Integration Tests**: Cover all critical user flows
- **Property-Based Tests**: Cover all correctness properties

## Continuous Integration

Tests run automatically on:
- Every commit (unit tests)
- Pull requests (all tests)
- Nightly builds (all tests + coverage)

## Troubleshooting

### Tests Fail with "Cannot connect to Docker"

**Solution**: Ensure Docker is running
```bash
docker ps
```

### Tests Fail with "Port already in use"

**Solution**: Stop conflicting processes or change test port
```bash
# Find process using port
lsof -i :5432
# Kill process
kill -9 <PID>
```

### Integration Tests are Slow

**Solution**: Run unit tests only during development
```bash
mvn test -Dtest="*Test" -DexcludeGroups=integration
```

### Property Tests Fail Intermittently

**Solution**: Increase iteration count or fix flaky test
```java
qt()
    .withExamples(1000)  // Increase from default 100
    .forAll(...)
```

### Out of Memory Errors

**Solution**: Increase Maven memory
```bash
export MAVEN_OPTS="-Xmx2g"
mvn test
```

## Best Practices

### Writing Unit Tests

1. **Test one thing per test** - Each test should verify a single behavior
2. **Use descriptive names** - `methodName_condition_expectedResult`
3. **Follow AAA pattern** - Arrange, Act, Assert
4. **Mock external dependencies** - Use Mockito for mocks
5. **Avoid test interdependencies** - Tests should run in any order

### Writing Integration Tests

1. **Use Testcontainers** - Real database for realistic tests
2. **Clean up after tests** - Use `@Transactional` or manual cleanup
3. **Test complete flows** - End-to-end user scenarios
4. **Verify side effects** - Check database state, events emitted
5. **Use test data builders** - Consistent test data creation

### Writing Property-Based Tests

1. **Test universal properties** - Properties that hold for all inputs
2. **Use appropriate generators** - Match domain constraints
3. **Add assumptions** - Filter invalid input combinations
4. **Keep properties simple** - One property per test
5. **Document failing examples** - Help reproduce and fix issues

## Test Reports

### Surefire Reports

Location: `target/surefire-reports/`

Files:
- `TEST-*.xml` - JUnit XML reports
- `*.txt` - Text summaries

### Coverage Reports

Location: `target/site/jacoco/`

Open `index.html` in browser for interactive coverage report.

### Test Execution Time

View test execution times:
```bash
mvn test | grep "Time elapsed"
```

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [QuickTheories Documentation](https://github.com/quicktheories/QuickTheories)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
