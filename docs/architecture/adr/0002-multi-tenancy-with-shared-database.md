# ADR 0002: Multi-Tenancy with Shared Database and Row-Level Security

## Status

Accepted

## Context

As a SaaS application, we need to support multiple restaurant tenants with complete data isolation. We need to choose a multi-tenancy strategy that:
- Ensures complete data isolation between tenants
- Scales efficiently with tenant growth
- Minimizes operational complexity
- Provides good performance
- Keeps costs reasonable

We considered three multi-tenancy approaches:

1. **Database per Tenant**: Each tenant gets their own database
2. **Schema per Tenant**: Each tenant gets their own schema in a shared database
3. **Shared Database with Row-Level Security**: All tenants share tables with tenant_id filtering

## Decision

We will use a **shared database with tenant_id column** on all domain tables, with **optional PostgreSQL Row-Level Security (RLS)** for defense-in-depth.

## Rationale

### Shared Database Approach

**Data Model:**
- All domain tables include `tenant_id` column (UUID, NOT NULL, indexed)
- All queries filter by `tenant_id` from authenticated user context
- Composite indexes on `(tenant_id, <business_key>)` for performance

**Tenant Context:**
- JWT token includes `tenant_id` claim
- `TenantContextFilter` extracts tenant from JWT and sets thread-local context
- `TenantAspect` enforces tenant filtering on all repository queries
- Application code cannot bypass tenant filtering

**Optional RLS:**
- PostgreSQL RLS policies enforce `tenant_id = current_setting('app.tenant_id')`
- Provides database-level protection even if application code has bugs
- Can be enabled/disabled per deployment

### Why Shared Database?

**Advantages:**
1. **Operational Simplicity**: Single database to manage, backup, and monitor
2. **Cost Efficiency**: Shared resources reduce infrastructure costs
3. **Schema Evolution**: Single migration applies to all tenants
4. **Cross-Tenant Analytics**: Easier to run reports across tenants
5. **Resource Utilization**: Better resource sharing and efficiency

**Disadvantages:**
1. **Noisy Neighbor**: One tenant's load can affect others
2. **Scaling Limits**: Single database has upper scaling limit
3. **Blast Radius**: Database issue affects all tenants
4. **Compliance**: Some regulations may require physical separation

### Why Optional RLS?

**Defense-in-Depth:**
- Application-level filtering is primary protection
- RLS provides backup protection against bugs
- Minimal performance overhead with proper indexing

**Flexibility:**
- Can be disabled for deployments where not needed
- Can be enabled for high-security deployments
- Separate migration file makes it optional

## Implementation Details

### Tenant Context Management

```java
// TenantContextFilter extracts tenant from JWT
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        String tenantId = extractTenantFromJwt(request);
        TenantContext.setTenantId(tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

// TenantAspect enforces filtering on repositories
@Aspect
@Component
public class TenantAspect {
    @Around("execution(* com.restaurantpos..repository.*Repository+.*(..))")
    public Object enforceTenantFilter(ProceedingJoinPoint joinPoint) {
        // Inject tenant_id into query parameters
        // Throw exception if tenant context is missing
    }
}
```

### Database Schema

```sql
-- All domain tables include tenant_id
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    site_id UUID NOT NULL,
    -- other columns
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Composite indexes for performance
CREATE INDEX idx_orders_tenant_site_status 
    ON orders(tenant_id, site_id, status);

-- Optional RLS policy
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON orders
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
```

## Consequences

### Positive

- Simple operations with single database
- Cost-effective resource sharing
- Easy schema migrations
- Good performance with proper indexing
- Defense-in-depth with optional RLS

### Negative

- Potential noisy neighbor issues
- Single database scaling limit
- All tenants affected by database issues
- Need careful query optimization

### Mitigation

- Monitor per-tenant resource usage
- Implement rate limiting per tenant
- Plan for database sharding if needed
- Use connection pooling appropriately
- Regular performance testing

## Alternatives Considered

### 1. Database per Tenant

**Pros:**
- Complete isolation
- Independent scaling
- Blast radius limited to one tenant
- Easier compliance

**Cons:**
- High operational complexity
- Expensive at scale
- Schema migration complexity
- Connection pool overhead

**Rejected because:** Operational complexity and cost outweigh benefits for our scale.

### 2. Schema per Tenant

**Pros:**
- Good isolation
- Single database to manage
- Independent schema evolution possible

**Cons:**
- PostgreSQL connection overhead per schema
- Schema migration complexity
- Limited by database connection limits
- Cross-tenant queries difficult

**Rejected because:** Middle ground that doesn't provide enough benefit over shared database.

## Migration Path

If we need to move to database-per-tenant:

1. Tenant data is already isolated by `tenant_id`
2. Extract tenant data to new database
3. Update routing layer to direct tenant to their database
4. Minimal application code changes needed

## References

- [Multi-Tenancy Patterns](https://docs.microsoft.com/en-us/azure/architecture/guide/multitenant/approaches/overview)
- [PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Requirements Document](../../.kiro/specs/restaurant-pos-saas/requirements.md) - Requirement 1

## Date

2024-01-15

## Authors

Development Team
