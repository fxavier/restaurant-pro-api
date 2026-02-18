# SQL Injection Prevention

## Overview

This document describes the SQL injection prevention measures implemented in the Restaurant POS SaaS application.

**Requirement**: 13.2 - The System SHALL sanitize all user input to prevent SQL injection and XSS attacks

## Primary Defense: Parameterized Queries

The application's primary defense against SQL injection is the use of **parameterized queries** through JPA/JPQL and Spring Data JPA.

### JPA/JPQL Queries

All custom queries use named parameters with the `@Param` annotation:

```java
@Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.phone LIKE :suffix")
List<Customer> findByTenantIdAndPhoneSuffix(@Param("tenantId") UUID tenantId, @Param("suffix") String suffix);
```

**Why this is safe**: JPA automatically escapes and parameterizes the values, preventing SQL injection.

### Spring Data JPA Method Names

Most queries use Spring Data JPA's method name conventions:

```java
List<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);
Optional<FiscalDocument> findByIdAndTenantId(UUID id, UUID tenantId);
```

**Why this is safe**: Spring Data JPA generates parameterized queries automatically.

## Secondary Defense: Input Sanitization

For additional defense-in-depth, the application sanitizes user input in specific cases:

### 1. LIKE Pattern Sanitization

The `CustomerService.searchByPhoneSuffix()` method sanitizes phone suffix input:

```java
// Remove SQL wildcard characters
String sanitizedSuffix = suffix.trim()
    .replace("%", "")
    .replace("_", "");

// Prepend '%' for LIKE pattern
String likePattern = "%" + sanitizedSuffix;
```

**Why this is needed**: Even with parameterized queries, user-controlled LIKE patterns could match unintended records if wildcards are not sanitized.

### 2. SqlInjectionPrevention Utility

The `SqlInjectionPrevention` utility class provides additional sanitization methods:

- `sanitizeLikePattern()`: Removes SQL wildcard characters (%, _)
- `isAlphanumericSafe()`: Validates alphanumeric input
- `isNumeric()`: Validates numeric input
- `containsSqlInjectionPatterns()`: Detects common SQL injection patterns
- `sanitizeInput()`: Removes dangerous characters (last resort)

## Verification

### Repository Audit

All repositories have been audited to verify:

1. ✅ No native SQL queries (`nativeQuery = true`)
2. ✅ No `EntityManager.createNativeQuery()` usage
3. ✅ No string concatenation in `@Query` annotations
4. ✅ All custom queries use `@Param` annotations
5. ✅ No `JdbcTemplate` usage in production code (only in tests with parameterized queries)

### Custom Queries Inventory

| Repository | Query | Parameterized | Safe |
|------------|-------|---------------|------|
| CustomerRepository | findByTenantIdAndPhoneSuffix | ✅ Yes | ✅ Yes (with sanitization) |
| FiscalDocumentRepository | findMaxDocumentNumber | ✅ Yes | ✅ Yes |
| RefreshTokenRepository | revokeAllByUserId | ✅ Yes | ✅ Yes |
| RefreshTokenRepository | deleteExpiredTokens | ✅ Yes | ✅ Yes |

### Test Coverage

The following tests verify SQL injection prevention:

1. **SqlInjectionPreventionTest**: Unit tests for the utility class
2. **SqlInjectionPreventionIntegrationTest**: Integration tests that attempt actual SQL injection attacks
3. **CustomerServiceTest**: Tests for phone suffix sanitization

## Best Practices

### DO ✅

1. **Use Spring Data JPA method names** for simple queries
2. **Use `@Query` with `@Param`** for custom queries
3. **Sanitize LIKE patterns** to remove wildcards
4. **Validate input** before using in queries
5. **Use UUIDs** for identifiers (not user-controlled strings)

### DON'T ❌

1. **Never concatenate user input** into query strings
2. **Never use native SQL** unless absolutely necessary
3. **Never trust user input** without validation
4. **Never use `EntityManager.createNativeQuery()`** with user input
5. **Never disable parameterization** for convenience

## Emergency Procedures

If a SQL injection vulnerability is discovered:

1. **Immediately** disable the affected endpoint
2. **Review** database logs for suspicious queries
3. **Audit** all user input in the affected code path
4. **Add** input validation and sanitization
5. **Test** with SQL injection payloads
6. **Deploy** the fix with high priority
7. **Monitor** for similar patterns in other code

## References

- OWASP SQL Injection Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
- Spring Data JPA Documentation: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- JPA Query Language Specification: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html

## Compliance

This implementation satisfies:

- **Requirement 13.2**: Sanitize all user input to prevent SQL injection
- **OWASP Top 10 2021 - A03:2021**: Injection
- **CWE-89**: SQL Injection
