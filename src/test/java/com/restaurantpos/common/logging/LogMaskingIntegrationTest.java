package com.restaurantpos.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class LogMaskingIntegrationTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LogMaskingIntegrationTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void shouldMaskPasswordInLogOutput() {
        logger.info("User authentication with password=secret123");
        
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        // The raw message should still contain the password
        assertThat(message).contains("password=secret123");
        
        // But when processed through our converter, it should be masked
        String masked = SensitiveDataMasker.mask(message);
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("secret123");
    }

    @Test
    void shouldMaskCardNumberInLogOutput() {
        logger.info("Processing payment with card 1234567890123456");
        
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        String masked = SensitiveDataMasker.mask(message);
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).doesNotContain("123456789012");
    }

    @Test
    void shouldMaskNifInLogOutput() {
        logger.info("Creating invoice for customer with nif=123456789");
        
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        String masked = SensitiveDataMasker.mask(message);
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("123456789");
    }

    @Test
    void shouldMaskMultipleSensitiveFieldsInSingleLog() {
        logger.info("Payment request: {\"password\":\"secret\",\"card\":\"1234567890123456\",\"customer_nif\":\"987654321\"}");
        
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        String masked = SensitiveDataMasker.mask(message);
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("secret");
        assertThat(masked).doesNotContain("123456789012");
        assertThat(masked).doesNotContain("987654321");
    }

    @Test
    void shouldNotMaskNormalLogMessages() {
        logger.info("Order created successfully for table 5");
        
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        String message = event.getFormattedMessage();
        
        String masked = SensitiveDataMasker.mask(message);
        assertThat(masked).isEqualTo(message);
    }
}
