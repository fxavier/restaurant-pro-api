# Sensitive Data Masking in Logs

## Overview

This module implements automatic masking of sensitive data in application logs to comply with security requirement 13.3. All log output is automatically processed to mask passwords, payment card numbers, and tax identification numbers (NIF).

## What Gets Masked

### 1. Passwords
- Pattern: `password=value`, `password: value`, `"password":"value"`
- Masked as: `***MASKED***`
- Case-insensitive matching

### 2. Payment Card Numbers
- Pattern: 13-19 digit numbers (with or without spaces/dashes)
- Masked as: `****-****-****-XXXX` (keeps last 4 digits)
- Supports formats:
  - `1234567890123456`
  - `1234 5678 9012 3456`
  - `1234-5678-9012-3456`

### 3. NIF (Tax Identification Numbers)
- Pattern: `nif=value`, `customer_nif=value`, `"nif":"value"`
- Masked as: `*********`
- Case-insensitive matching

## Implementation

### Components

1. **SensitiveDataMasker**: Core utility class that performs the masking using regex patterns
2. **MaskingMessageConverter**: Logback converter that integrates the masker into the logging pipeline
3. **logback-spring.xml**: Logback configuration that registers and uses the custom converter

### Configuration

The masking is configured in `src/main/resources/logback-spring.xml`:

```xml
<conversionRule conversionWord="maskedMsg" 
                converterClass="com.restaurantpos.common.logging.MaskingMessageConverter" />

<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %maskedMsg%n</pattern>
    </encoder>
</appender>
```

The `%maskedMsg` conversion word replaces the standard `%msg` to apply masking.

## Usage

No code changes are required. All logging automatically applies masking:

```java
// This log statement
logger.info("User login with password=secret123");

// Will output as
// 2024-01-15 10:30:00 [main] INFO  AuthService - User login with password=***MASKED***

// This log statement
logger.info("Processing payment with card 1234567890123456");

// Will output as
// 2024-01-15 10:30:00 [main] INFO  PaymentService - Processing payment with card ****-****-****-3456
```

## Testing

Comprehensive tests are provided:

- **SensitiveDataMaskerTest**: Unit tests for the masking logic
- **LogMaskingIntegrationTest**: Integration tests verifying masking works with actual logging

Run tests with:
```bash
mvn test -Dtest=SensitiveDataMaskerTest
mvn test -Dtest=LogMaskingIntegrationTest
```

## Performance Considerations

- Masking is applied at log formatting time, not at log statement evaluation
- Regex patterns are compiled once and reused
- Minimal performance impact on logging throughput
- No impact when logging is disabled for a level

## Compliance

This implementation satisfies:
- **Requirement 13.3**: "THE System SHALL mask sensitive data in logs: passwords, payment card numbers, full NIF"

## Extending the Masker

To add new sensitive data patterns:

1. Add a new Pattern constant in `SensitiveDataMasker`
2. Create a masking method for the pattern
3. Call the method in the `mask()` method
4. Add tests in `SensitiveDataMaskerTest`

Example:
```java
private static final Pattern EMAIL_PATTERN = Pattern.compile(
    "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
);

private static String maskEmails(String message) {
    Matcher matcher = EMAIL_PATTERN.matcher(message);
    return matcher.replaceAll("***@***.***");
}
```
