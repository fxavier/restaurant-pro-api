package com.restaurantpos.common.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DatabaseHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DatabaseHealthIndicator(jdbcTemplate);
    }

    @Test
    void health_WhenDatabaseIsReachable_ReturnsUp() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "PostgreSQL");
        assertThat(health.getDetails()).containsEntry("status", "Connection successful");
    }

    @Test
    void health_WhenDatabaseReturnsUnexpectedResult_ReturnsDown() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "PostgreSQL");
        assertThat(health.getDetails()).containsEntry("status", "Unexpected query result");
    }

    @Test
    void health_WhenDatabaseConnectionFails_ReturnsDown() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "PostgreSQL");
        assertThat(health.getDetails()).containsEntry("status", "Connection failed");
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void health_WhenDatabaseReturnsNull_ReturnsDown() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("database", "PostgreSQL");
        assertThat(health.getDetails()).containsEntry("status", "Unexpected query result");
    }
}
