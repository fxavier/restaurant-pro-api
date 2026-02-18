package com.restaurantpos.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskPasswordInJson() {
        String message = "{\"username\":\"john\",\"password\":\"secret123\"}";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("secret123");
    }

    @Test
    void shouldMaskPasswordWithQuotes() {
        String message = "password='mypassword'";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("mypassword");
    }

    @Test
    void shouldMaskPasswordWithColon() {
        String message = "password: supersecret";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("supersecret");
    }

    @Test
    void shouldMaskCardNumberWithSpaces() {
        String message = "Card number: 1234 5678 9012 3456";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).doesNotContain("1234 5678 9012");
    }

    @Test
    void shouldMaskCardNumberWithDashes() {
        String message = "Card: 1234-5678-9012-3456";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).doesNotContain("1234-5678-9012");
    }

    @Test
    void shouldMaskCardNumberWithoutSeparators() {
        String message = "Card: 1234567890123456";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).doesNotContain("123456789012");
    }

    @Test
    void shouldKeepLastFourDigitsOfCard() {
        String message = "Payment with card 4532123456789012";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("9012");
        assertThat(masked).contains("****-****-****-9012");
    }

    @Test
    void shouldMaskNifInJson() {
        String message = "{\"nif\":\"123456789\"}";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("123456789");
    }

    @Test
    void shouldMaskCustomerNifField() {
        String message = "{\"customer_nif\":\"987654321\"}";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("987654321");
    }

    @Test
    void shouldMaskNifWithColon() {
        String message = "nif: 123456789";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("123456789");
    }

    @Test
    void shouldMaskMultipleSensitiveFields() {
        String message = "{\"password\":\"secret\",\"card\":\"1234567890123456\",\"nif\":\"123456789\"}";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).contains("****-****-****-3456");
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("secret");
        assertThat(masked).doesNotContain("123456789012");
    }

    @Test
    void shouldHandleNullMessage() {
        String masked = SensitiveDataMasker.mask(null);
        assertThat(masked).isNull();
    }

    @Test
    void shouldHandleEmptyMessage() {
        String masked = SensitiveDataMasker.mask("");
        assertThat(masked).isEmpty();
    }

    @Test
    void shouldNotMaskNonSensitiveData() {
        String message = "User logged in successfully";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).isEqualTo(message);
    }

    @Test
    void shouldMaskPasswordCaseInsensitive() {
        String message = "PASSWORD=secret123";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("secret123");
    }

    @Test
    void shouldMaskNifCaseInsensitive() {
        String message = "NIF: 123456789";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("*********");
        assertThat(masked).doesNotContain("123456789");
    }

    @Test
    void shouldMaskAmexCardNumber() {
        // American Express cards have 15 digits (4-4-4-3 format)
        String message = "Card: 378282246310005";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("****-****-****-005");
        assertThat(masked).doesNotContain("37828224631");
    }

    @Test
    void shouldPreserveLogStructure() {
        String message = "2024-01-15 10:30:00 [http-nio-8080-exec-1] INFO  AuthService - User login attempt with password=secret123";
        String masked = SensitiveDataMasker.mask(message);
        
        assertThat(masked).contains("2024-01-15 10:30:00");
        assertThat(masked).contains("[http-nio-8080-exec-1]");
        assertThat(masked).contains("INFO");
        assertThat(masked).contains("AuthService");
        assertThat(masked).contains("***MASKED***");
        assertThat(masked).doesNotContain("secret123");
    }
}
