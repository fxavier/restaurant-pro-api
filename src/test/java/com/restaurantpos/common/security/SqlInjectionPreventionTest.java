package com.restaurantpos.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SqlInjectionPrevention utility class.
 * 
 * Requirements: 13.2 - Prevent SQL injection attacks
 */
class SqlInjectionPreventionTest {
    
    @Test
    void testSanitizeLikePattern_RemovesWildcards() {
        assertEquals("1234", SqlInjectionPrevention.sanitizeLikePattern("%1234"));
        assertEquals("1234", SqlInjectionPrevention.sanitizeLikePattern("1234%"));
        assertEquals("1234", SqlInjectionPrevention.sanitizeLikePattern("%1234%"));
        assertEquals("1234", SqlInjectionPrevention.sanitizeLikePattern("12_34"));
        assertEquals("1234", SqlInjectionPrevention.sanitizeLikePattern("%12_34%"));
    }
    
    @Test
    void testSanitizeLikePattern_NullInput() {
        assertThrows(IllegalArgumentException.class, 
            () -> SqlInjectionPrevention.sanitizeLikePattern(null));
    }
    
    @Test
    void testIsAlphanumericSafe_ValidInputs() {
        assertTrue(SqlInjectionPrevention.isAlphanumericSafe("abc123"));
        assertTrue(SqlInjectionPrevention.isAlphanumericSafe("Test User"));
        assertTrue(SqlInjectionPrevention.isAlphanumericSafe("user-name_123"));
        assertTrue(SqlInjectionPrevention.isAlphanumericSafe("ABC"));
    }
    
    @Test
    void testIsAlphanumericSafe_InvalidInputs() {
        assertFalse(SqlInjectionPrevention.isAlphanumericSafe("test'; DROP TABLE users--"));
        assertFalse(SqlInjectionPrevention.isAlphanumericSafe("test@example.com"));
        assertFalse(SqlInjectionPrevention.isAlphanumericSafe("test<script>"));
        assertFalse(SqlInjectionPrevention.isAlphanumericSafe(null));
        assertFalse(SqlInjectionPrevention.isAlphanumericSafe(""));
    }
    
    @Test
    void testIsNumeric_ValidInputs() {
        assertTrue(SqlInjectionPrevention.isNumeric("123"));
        assertTrue(SqlInjectionPrevention.isNumeric("0"));
        assertTrue(SqlInjectionPrevention.isNumeric("999999999"));
    }
    
    @Test
    void testIsNumeric_InvalidInputs() {
        assertFalse(SqlInjectionPrevention.isNumeric("123abc"));
        assertFalse(SqlInjectionPrevention.isNumeric("12.34"));
        assertFalse(SqlInjectionPrevention.isNumeric("12-34"));
        assertFalse(SqlInjectionPrevention.isNumeric(null));
        assertFalse(SqlInjectionPrevention.isNumeric(""));
    }
    
    @Test
    void testContainsSqlInjectionPatterns_DetectsSqlKeywords() {
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("SELECT * FROM users"));
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("test'; DROP TABLE users--"));
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("1' UNION SELECT password"));
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("admin'--"));
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("1=1"));
        assertTrue(SqlInjectionPrevention.containsSqlInjectionPatterns("test /* comment */"));
    }
    
    @Test
    void testContainsSqlInjectionPatterns_SafeInputs() {
        assertFalse(SqlInjectionPrevention.containsSqlInjectionPatterns("John Doe"));
        assertFalse(SqlInjectionPrevention.containsSqlInjectionPatterns("123456789"));
        assertFalse(SqlInjectionPrevention.containsSqlInjectionPatterns("test@example.com"));
        assertFalse(SqlInjectionPrevention.containsSqlInjectionPatterns(null));
        assertFalse(SqlInjectionPrevention.containsSqlInjectionPatterns(""));
    }
    
    @Test
    void testSanitizeInput_RemovesDangerousCharacters() {
        assertEquals("test", SqlInjectionPrevention.sanitizeInput("test';"));
        assertEquals("test", SqlInjectionPrevention.sanitizeInput("test--"));
        assertEquals("testcomment", SqlInjectionPrevention.sanitizeInput("test/*comment*/"));
        assertEquals("DROP TABLE users", SqlInjectionPrevention.sanitizeInput("DROP TABLE users;"));
    }
    
    @Test
    void testSanitizeInput_NullInput() {
        assertNull(SqlInjectionPrevention.sanitizeInput(null));
    }
}
