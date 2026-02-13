package com.restaurantpos.kitchenprinting.model;

/**
 * Enum representing the status of a printer.
 * 
 * Requirements: 6.3
 */
public enum PrinterStatus {
    /**
     * Printer is operating normally.
     */
    NORMAL,
    
    /**
     * Printer is in wait mode (jobs queued but not printed).
     */
    WAIT,
    
    /**
     * Printer is in ignore mode (jobs skipped).
     */
    IGNORE,
    
    /**
     * Printer is redirecting jobs to another printer.
     */
    REDIRECT
}
