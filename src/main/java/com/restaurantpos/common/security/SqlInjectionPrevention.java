package com.restaurantpos.common.security;

/**
 * Utility class for SQL injection prevention.
 * 
 * This class provides methods to sanitize user input that might be used in SQL queries.
 * While the application primarily uses JPA/JPQL with parameterized queries, this utility
 * provides additional defense-in-depth for edge cases.
 * 
 * Requirements: 13.2 - Prevent SQL injection attacks
 */
public final class SqlInjectionPrevention {
    
    private SqlInjectionPrevention() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Sanitizes a string for use in LIKE patterns by escaping SQL wildcard characters.
     * Removes '%' and '_' characters that could be used for SQL injection in LIKE clauses.
     * 
     * @param input the input string to sanitize
     * @return the sanitized string with wildcards removed
     * @throws IllegalArgumentException if input is null
     */
    public static String sanitizeLikePattern(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        return input.replace("%", "").replace("_", "");
    }
    
    /**
     * Validates that a string contains only alphanumeric characters and common safe characters.
     * Useful for validating identifiers, codes, or other structured data.
     * 
     * Allowed characters: a-z, A-Z, 0-9, space, hyphen, underscore
     * 
     * @param input the input string to validate
     * @return true if the string is safe, false otherwise
     */
    public static boolean isAlphanumericSafe(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return input.matches("^[a-zA-Z0-9 \\-_]+$");
    }
    
    /**
     * Validates that a string contains only numeric characters.
     * Useful for validating phone numbers, document numbers, etc.
     * 
     * @param input the input string to validate
     * @return true if the string contains only digits, false otherwise
     */
    public static boolean isNumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return input.matches("^[0-9]+$");
    }
    
    /**
     * Validates that a string does not contain SQL keywords or special characters
     * that could be used for SQL injection.
     * 
     * This is a defense-in-depth measure. The primary defense is parameterized queries.
     * 
     * @param input the input string to validate
     * @return true if the string appears safe, false if it contains suspicious patterns
     */
    public static boolean containsSqlInjectionPatterns(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for common SQL injection patterns
        String[] sqlKeywords = {
            "select", "insert", "update", "delete", "drop", "create", "alter",
            "exec", "execute", "union", "declare", "cast", "convert",
            "--", "/*", "*/", "xp_", "sp_", "0x", "char(", "nchar(",
            "varchar(", "nvarchar(", "||", "&&"
        };
        
        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        // Check for suspicious character sequences
        if (lowerInput.contains("';") || lowerInput.contains("\"") || 
            lowerInput.contains("1=1") || lowerInput.contains("1 = 1")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitizes a string by removing potentially dangerous characters.
     * This should only be used as a last resort. Prefer parameterized queries.
     * 
     * @param input the input string to sanitize
     * @return the sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove SQL comment markers
        String sanitized = input.replace("--", "")
                                .replace("/*", "")
                                .replace("*/", "");
        
        // Remove semicolons (statement terminators)
        sanitized = sanitized.replace(";", "");
        
        // Remove single quotes (string delimiters)
        sanitized = sanitized.replace("'", "");
        
        return sanitized;
    }
}
