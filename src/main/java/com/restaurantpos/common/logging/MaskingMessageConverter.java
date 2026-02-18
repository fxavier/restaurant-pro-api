package com.restaurantpos.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Custom Logback converter that masks sensitive data in log messages.
 * This converter is used in logback configuration to automatically mask
 * passwords, card numbers, and NIF in all log output.
 */
public class MaskingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = super.convert(event);
        return SensitiveDataMasker.mask(originalMessage);
    }
}
