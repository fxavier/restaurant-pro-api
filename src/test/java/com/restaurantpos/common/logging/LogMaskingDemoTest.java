package com.restaurantpos.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstration test showing how sensitive data masking works in practice.
 * Run this test and observe the console output to see masking in action.
 */
class LogMaskingDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(LogMaskingDemoTest.class);

    @Test
    void demonstrateSensitiveDataMasking() {
        logger.info("=== Sensitive Data Masking Demo ===");
        
        // Example 1: Password masking
        logger.info("Attempting login with password=mySecretPassword123");
        logger.info("User credentials: {\"username\":\"john\",\"password\":\"secret123\"}");
        
        // Example 2: Card number masking
        logger.info("Processing payment with card number: 4532123456789012");
        logger.info("Card details: 1234-5678-9012-3456");
        logger.info("Payment info: {\"card\":\"4532 1234 5678 9012\",\"amount\":100.00}");
        
        // Example 3: NIF masking
        logger.info("Creating invoice for customer with nif=123456789");
        logger.info("Customer data: {\"name\":\"John Doe\",\"customer_nif\":\"987654321\"}");
        
        // Example 4: Multiple sensitive fields
        logger.info("Complete transaction: {\"password\":\"secret\",\"card\":\"1234567890123456\",\"nif\":\"123456789\"}");
        
        // Example 5: Non-sensitive data (should not be masked)
        logger.info("Order created successfully for table 5 with total amount 50.00");
        
        logger.info("=== Demo Complete ===");
        logger.info("Note: In the console output above, you should see:");
        logger.info("- Passwords replaced with ***MASKED***");
        logger.info("- Card numbers showing only last 4 digits as ****-****-****-XXXX");
        logger.info("- NIF replaced with *********");
        logger.info("- Normal log messages unchanged");
    }
}
