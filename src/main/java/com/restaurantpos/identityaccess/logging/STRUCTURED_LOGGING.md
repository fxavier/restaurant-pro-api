# Structured Logging Configuration

## Overview

The Restaurant POS SaaS system uses structured logging with JSON format to enable efficient log aggregation, searching, and monitoring in production environments.

## Requirements

- **14.1**: Structured Log Format - All logs are in JSON format with standard fields
- **14.2**: API Request Logging - All API requests are logged with contextual information
- **14.6**: Correlation ID Propagation - Trace IDs are propagated across all log entries for a request

## Components

### LoggingFilter

The `LoggingFilter` is a Spring filter that populates the MDC (Mapped Diagnostic Context) with contextual information for each request:

- **tenant_id**: Current tenant from `TenantContext`
- **user_id**: Current authenticated user ID from JWT token
- **trace_id**: Correlation ID for request tracing (from `X-Trace-Id` header or auto-generated)

The filter runs after the `TenantContextFilter` to ensure tenant context is available.

### Logback Configuration

The `logback-spring.xml` configuration includes:

1. **Console Appender**: Human-readable format for development
2. **JSON Appender**: Structured JSON format for production
3. **File Appender**: Rolling file appender with retention policy

#### JSON Format

The JSON appender uses `LogstashEncoder` to produce structured logs with the following fields:

```json
{
  "timestamp": "2026-02-18T17:00:00.000Z",
  "level": "INFO",
  "logger": "com.restaurantpos.orders.service.OrderService",
  "thread": "http-nio-8080-exec-1",
  "message": "Order confirmed successfully",
  "tenant_id": "123e4567-e89b-12d3-a456-426614174000",
  "user_id": "987fcdeb-51a2-43f7-9abc-123456789012",
  "trace_id": "abc-123-def-456",
  "application": "restaurant-pos-saas"
}
```

## Usage

### In Application Code

Simply use SLF4J logging as usual. The MDC fields are automatically populated by the filter:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public void confirmOrder(UUID orderId) {
        // MDC fields (tenant_id, user_id, trace_id) are automatically included
        logger.info("Confirming order: {}", orderId);
        
        // ... business logic ...
        
        logger.info("Order confirmed successfully");
    }
}
```

### Correlation ID Propagation

To enable request tracing across services, include the `X-Trace-Id` header in API requests:

```bash
curl -H "X-Trace-Id: my-correlation-id-123" \
     -H "Authorization: Bearer <token>" \
     https://api.example.com/api/orders
```

If the header is not provided, a new UUID will be generated automatically.

## Spring Profiles

### Development Profile (`dev`)

Uses console appender with human-readable format:

```
2026-02-18 17:00:00 [http-nio-8080-exec-1] INFO  c.r.orders.service.OrderService - Order confirmed successfully
```

### Production Profile (`prod`)

Uses JSON appender for structured logging:

```json
{"timestamp":"2026-02-18T17:00:00.000Z","level":"INFO","logger":"com.restaurantpos.orders.service.OrderService","message":"Order confirmed successfully","tenant_id":"123e4567-e89b-12d3-a456-426614174000","user_id":"987fcdeb-51a2-43f7-9abc-123456789012","trace_id":"abc-123-def-456"}
```

## Integration with Log Aggregation

The structured JSON logs can be easily integrated with log aggregation systems:

- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Splunk**: Direct JSON ingestion
- **CloudWatch Logs**: AWS log aggregation
- **Datadog**: Application monitoring and logging

## MDC Cleanup

The `LoggingFilter` automatically clears MDC fields after each request to prevent:

- Memory leaks from thread-local storage
- Cross-request contamination in thread pools

## Testing

### Unit Tests

- `LoggingFilterTest`: Tests MDC population and cleanup
- `StructuredLoggingIntegrationTest`: Tests MDC fields in log events

### Manual Testing

To verify structured logging in production mode:

```bash
# Start application with prod profile
java -jar -Dspring.profiles.active=prod restaurant-pos-saas.jar

# Make an API request
curl -H "X-Trace-Id: test-123" \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/orders

# Check logs for JSON format with MDC fields
```

## Security Considerations

Sensitive data masking is applied to log messages through the `MaskingMessageConverter` to prevent exposure of:

- Passwords
- Payment card numbers
- Tax identification numbers (NIF)

See `SENSITIVE_DATA_MASKING.md` for details.
