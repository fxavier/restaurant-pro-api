package com.restaurantpos.common.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class DiskSpaceHealthIndicatorTest {

    private DiskSpaceHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DiskSpaceHealthIndicator();
    }

    @Test
    void health_ReturnsHealthStatus() {
        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails()).containsKey("free");
        assertThat(health.getDetails()).containsKey("total");
        assertThat(health.getDetails()).containsKey("usable");
        assertThat(health.getDetails()).containsKey("usedPercentage");
        assertThat(health.getDetails()).containsKey("threshold");
    }

    @Test
    void health_IncludesStatusMessage() {
        // Act
        Health health = healthIndicator.health();

        // Assert
        String status = (String) health.getDetails().get("status");
        assertThat(status).isIn(
                "Healthy",
                "Warning - Disk space running low",
                "Critical - Low disk space"
        );
    }

    @Test
    void health_IncludesDiskSpaceMetrics() {
        // Act
        Health health = healthIndicator.health();

        // Assert
        String free = (String) health.getDetails().get("free");
        String total = (String) health.getDetails().get("total");
        String usable = (String) health.getDetails().get("usable");
        String usedPercentage = (String) health.getDetails().get("usedPercentage");

        assertThat(free).matches("\\d+\\.\\d{2} GB");
        assertThat(total).matches("\\d+\\.\\d{2} GB");
        assertThat(usable).matches("\\d+\\.\\d{2} GB");
        assertThat(usedPercentage).matches("\\d+\\.\\d{2}%");
    }
}
