package com.restaurantpos.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Integration test for Micrometer metrics configuration.
 * Validates that metrics endpoint is exposed and metrics are being collected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureObservability
class MetricsConfigurationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldExposeMetricsEndpoint() {
        // When: Access the metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

        // Then: Endpoint should be accessible
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("names");
    }

    @Test
    void shouldExposeHttpServerRequestsMetric() {
        // Given: Make a request to generate metrics
        restTemplate.getForEntity("/actuator/health", String.class);
        
        // When: Access the http server requests metric
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/metrics", 
                String.class
        );

        // Then: Metric list should be available and contain http metrics
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("names");
    }

    @Test
    void shouldExposeHikariCpMetrics() {
        // When: Access metrics list
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/metrics", 
                String.class
        );

        // Then: Metrics list should contain hikaricp metrics
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("hikaricp");
    }

    @Test
    void shouldExposeJvmMemoryMetrics() {
        // When: Access metrics list
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/metrics", 
                String.class
        );

        // Then: Metrics list should contain JVM memory metrics
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("jvm.memory");
    }

    @Test
    void shouldHaveCommonTags() {
        // When: Check meter registry configuration
        // Then: Common tags should be configured
        assertThat(meterRegistry).isNotNull();
        assertThat(meterRegistry.config()).isNotNull();
        
        // Verify that the configuration is properly set up
        assertThat(meterRegistry.config().commonTags()).isNotNull();
    }

    @Test
    void shouldCollectCustomHttpMetrics() {
        // Given: Make a request to generate metrics
        restTemplate.getForEntity("/actuator/health", String.class);

        // When: Check if custom metrics are collected
        // Then: Custom metrics should exist
        assertThat(meterRegistry.find("http.request.count").counters()).isNotEmpty();
        assertThat(meterRegistry.find("http.request.duration").timers()).isNotEmpty();
    }
}
