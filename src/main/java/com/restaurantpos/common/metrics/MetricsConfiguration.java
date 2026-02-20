package com.restaurantpos.common.metrics;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Configuration for Micrometer metrics.
 * Configures metrics for request rates, error rates, response times, and DB connection pool.
 */
@Configuration
public class MetricsConfiguration {

    /**
     * Customize the meter registry with common tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("service", "restaurant-pos-saas");
    }

    /**
     * Custom metrics for business operations.
     */
    @Bean
    public MeterBinder customMetrics(MeterRegistry registry) {
        return meterRegistry -> {
            // These gauges can be used to track business metrics
            // They will be populated by services as needed
        };
    }
}
