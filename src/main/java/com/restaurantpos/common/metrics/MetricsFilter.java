package com.restaurantpos.common.metrics;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to capture detailed HTTP metrics including request rates, error rates, and response times.
 * This complements Spring Boot's built-in HTTP metrics with additional custom tracking.
 */
@Component
@Order(1)
public class MetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            // Record response time
            Timer.builder("http.request.duration")
                    .tag("method", method)
                    .tag("uri", sanitizePath(path))
                    .tag("status", String.valueOf(status))
                    .tag("outcome", getOutcome(status))
                    .description("HTTP request duration")
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // Record request count
            Counter.builder("http.request.count")
                    .tag("method", method)
                    .tag("uri", sanitizePath(path))
                    .tag("status", String.valueOf(status))
                    .tag("outcome", getOutcome(status))
                    .description("HTTP request count")
                    .register(meterRegistry)
                    .increment();
            
            // Record error count if status >= 400
            if (status >= 400) {
                Counter.builder("http.request.errors")
                        .tag("method", method)
                        .tag("uri", sanitizePath(path))
                        .tag("status", String.valueOf(status))
                        .tag("error_type", getErrorType(status))
                        .description("HTTP request errors")
                        .register(meterRegistry)
                        .increment();
            }
        }
    }

    /**
     * Sanitize path to avoid high cardinality in metrics.
     * Replace UUIDs and numeric IDs with placeholders.
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "unknown";
        }
        
        // Replace UUIDs with {id}
        String sanitized = path.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "{id}"
        );
        
        // Replace numeric IDs with {id}
        sanitized = sanitized.replaceAll("/\\d+(/|$)", "/{id}$1");
        
        return sanitized;
    }

    /**
     * Determine outcome based on status code.
     */
    private String getOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        } else if (status >= 300 && status < 400) {
            return "REDIRECTION";
        } else if (status >= 400 && status < 500) {
            return "CLIENT_ERROR";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "UNKNOWN";
    }

    /**
     * Determine error type based on status code.
     */
    private String getErrorType(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 429 -> "TOO_MANY_REQUESTS";
            case 500 -> "INTERNAL_SERVER_ERROR";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status >= 400 && status < 500 ? "CLIENT_ERROR" : "SERVER_ERROR";
        };
    }
}
