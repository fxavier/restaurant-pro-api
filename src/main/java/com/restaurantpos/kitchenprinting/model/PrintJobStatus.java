package com.restaurantpos.kitchenprinting.model;

/**
 * Enum representing the status of a print job.
 * 
 * Requirements: 6.1
 */
public enum PrintJobStatus {
    /**
     * Print job is pending and waiting to be processed.
     */
    PENDING,
    
    /**
     * Print job has been successfully printed.
     */
    PRINTED,
    
    /**
     * Print job failed to print.
     */
    FAILED,
    
    /**
     * Print job was skipped (printer in IGNORE mode).
     */
    SKIPPED
}
