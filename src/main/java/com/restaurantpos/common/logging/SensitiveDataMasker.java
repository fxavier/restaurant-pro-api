package com.restaurantpos.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for masking sensitive data in log messages.
 * Masks passwords, payment card numbers, and NIF (tax identification numbers).
 */
public class SensitiveDataMasker {

    // Pattern for password fields in JSON or form data
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password[\"']?\\s*[:=]\\s*[\"']?)([^\"'\\s,}]+)([\"']?)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for card numbers (13-19 digits, with optional spaces or dashes)
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile(
        "\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{3,4})\\b"
    );

    // Pattern for NIF (Portuguese tax ID - 9 digits)
    private static final Pattern NIF_PATTERN = Pattern.compile(
        "\\b(nif[\"']?\\s*[:=]\\s*[\"']?)(\\d{9})([\"']?)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for customer_nif field in JSON
    private static final Pattern CUSTOMER_NIF_PATTERN = Pattern.compile(
        "(customer_nif[\"']?\\s*[:=]\\s*[\"']?)(\\d{9})([\"']?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final String PASSWORD_MASK = "***MASKED***";
    private static final String CARD_MASK = "****-****-****-";
    private static final String NIF_MASK = "*********";

    /**
     * Masks sensitive data in the given message.
     * 
     * @param message the original message
     * @return the message with sensitive data masked
     */
    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = message;

        // Mask passwords
        masked = maskPasswords(masked);

        // Mask card numbers
        masked = maskCardNumbers(masked);

        // Mask NIF
        masked = maskNIF(masked);

        return masked;
    }

    /**
     * Masks password values in the message.
     */
    private static String maskPasswords(String message) {
        Matcher matcher = PASSWORD_PATTERN.matcher(message);
        return matcher.replaceAll("$1" + PASSWORD_MASK + "$3");
    }

    /**
     * Masks payment card numbers, keeping only the last 4 digits.
     */
    private static String maskCardNumbers(String message) {
        Matcher matcher = CARD_NUMBER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String lastFour = matcher.group(4);
            matcher.appendReplacement(sb, CARD_MASK + lastFour);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Masks NIF (tax identification numbers).
     */
    private static String maskNIF(String message) {
        // Mask NIF field
        Matcher matcher = NIF_PATTERN.matcher(message);
        String masked = matcher.replaceAll("$1" + NIF_MASK + "$3");

        // Mask customer_nif field
        matcher = CUSTOMER_NIF_PATTERN.matcher(masked);
        return matcher.replaceAll("$1" + NIF_MASK + "$3");
    }
}
