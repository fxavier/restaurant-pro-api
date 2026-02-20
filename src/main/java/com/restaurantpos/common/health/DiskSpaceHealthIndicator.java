package com.restaurantpos.common.health;

import java.io.File;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for disk space.
 * Monitors available disk space and reports health status based on thresholds.
 */
@Component
public class DiskSpaceHealthIndicator implements HealthIndicator {

    private static final long THRESHOLD_BYTES = 10 * 1024 * 1024 * 1024L; // 10 GB
    private static final long WARNING_THRESHOLD_BYTES = 50 * 1024 * 1024 * 1024L; // 50 GB

    @Override
    public Health health() {
        try {
            File root = new File("/");
            long freeSpace = root.getFreeSpace();
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();
            
            double freeSpaceGB = freeSpace / (1024.0 * 1024.0 * 1024.0);
            double totalSpaceGB = totalSpace / (1024.0 * 1024.0 * 1024.0);
            double usableSpaceGB = usableSpace / (1024.0 * 1024.0 * 1024.0);
            double usedPercentage = ((totalSpace - freeSpace) * 100.0) / totalSpace;

            Health.Builder builder;
            String status;

            if (freeSpace < THRESHOLD_BYTES) {
                builder = Health.down();
                status = "Critical - Low disk space";
            } else if (freeSpace < WARNING_THRESHOLD_BYTES) {
                builder = Health.up();
                status = "Warning - Disk space running low";
            } else {
                builder = Health.up();
                status = "Healthy";
            }

            return builder
                    .withDetail("status", status)
                    .withDetail("free", String.format("%.2f GB", freeSpaceGB))
                    .withDetail("total", String.format("%.2f GB", totalSpaceGB))
                    .withDetail("usable", String.format("%.2f GB", usableSpaceGB))
                    .withDetail("usedPercentage", String.format("%.2f%%", usedPercentage))
                    .withDetail("threshold", String.format("%.2f GB", THRESHOLD_BYTES / (1024.0 * 1024.0 * 1024.0)))
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Unable to check disk space")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
