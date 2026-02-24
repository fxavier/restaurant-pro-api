# ADR 0005: API Versioning - Future Consideration

## Status

PROPOSED (Not Implemented)

## Context

The current API uses unversioned endpoints (e.g., `/api/auth/login`, `/api/orders`). As the system evolves, we may need to introduce breaking changes to the API while maintaining backward compatibility for existing clients.

API versioning is a common practice in production SaaS applications that allows:
- Introduction of breaking changes in new versions while maintaining old versions
- Graceful deprecation of old API versions
- Clear upgrade paths for API consumers
- Backward compatibility guarantees

## Decision

We document API versioning as a future enhancement to be considered when:
1. We need to introduce breaking changes to existing endpoints
2. We have production clients that cannot immediately migrate to new API versions
3. We need to maintain multiple API versions simultaneously

## Proposed Approach (When Implemented)

### URL-Based Versioning

Use URL path versioning with the format `/api/v{version}/`:

```
Current:  /api/auth/login
Future:   /api/v1/auth/login
          /api/v2/auth/login
```

### Implementation Strategy

1. **Phase 1: Add v1 to all existing endpoints**
   - Update all controller `@RequestMapping` annotations
   - Maintain `/api/*` as aliases to `/api/v1/*` for backward compatibility
   - Update security configuration
   - Update all tests
   - Update documentation

2. **Phase 2: Introduce v2 when needed**
   - Create new controllers or controller methods for v2 endpoints
   - Maintain v1 endpoints for existing clients
   - Document migration guide from v1 to v2
   - Set deprecation timeline for v1

### Affected Components

- **Controllers**: All REST controllers (9+ files)
- **Security**: SecurityConfig URL patterns
- **Tests**: All integration tests (50+ files)
- **Documentation**: README, API docs, Swagger/OpenAPI
- **Client SDKs**: Any generated client libraries

### Version Support Policy (Proposed)

- Support N and N-1 versions simultaneously
- Deprecation notice: 6 months before version removal
- Security patches: Applied to all supported versions
- Bug fixes: Applied to current version only

## Consequences

### Positive

- Enables breaking changes without disrupting existing clients
- Provides clear API evolution path
- Industry standard practice for public APIs
- Easier to communicate changes to API consumers

### Negative

- Increased maintenance burden (multiple versions)
- More complex codebase
- Requires careful planning for deprecation
- Testing overhead for multiple versions

### Neutral

- Breaking change when first implemented (all URLs change)
- Requires coordination with all API consumers
- Documentation must be maintained for each version

## Alternatives Considered

1. **Header-Based Versioning**: Using `Accept: application/vnd.restaurantpos.v1+json`
   - More RESTful but less discoverable
   - Harder to test with simple tools like curl

2. **Query Parameter Versioning**: Using `?version=1`
   - Easy to implement but not RESTful
   - Can be accidentally omitted

3. **No Versioning**: Continue with unversioned API
   - Simpler but limits ability to evolve API
   - Forces all clients to upgrade simultaneously

## When to Implement

Consider implementing API versioning when:
- Planning first breaking change to existing endpoints
- Onboarding first external API consumers
- Preparing for public API release
- Establishing SLA commitments for API stability

## References

- [Microsoft REST API Guidelines - Versioning](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#12-versioning)
- [Stripe API Versioning](https://stripe.com/docs/api/versioning)
- [GitHub API Versioning](https://docs.github.com/en/rest/overview/api-versions)

## Notes

This ADR documents a future consideration, not a current implementation. The decision to implement API versioning should be made when the need arises, considering the specific context and requirements at that time.

Current API endpoints remain unversioned (`/api/*`) until versioning is implemented.
