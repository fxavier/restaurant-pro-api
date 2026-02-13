package com.restaurantpos.kitchenprinting.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurantpos.kitchenprinting.entity.PrintJob;
import com.restaurantpos.kitchenprinting.model.PrintJobStatus;

/**
 * Repository for PrintJob entity with tenant filtering.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 6.1, 6.7
 */
@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {
    
    /**
     * Finds all print jobs for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of print jobs
     */
    List<PrintJob> findByTenantId(UUID tenantId);
    
    /**
     * Finds all print jobs for a specific order.
     * 
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * @return list of print jobs
     */
    List<PrintJob> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);
    
    /**
     * Finds all print jobs for a specific printer.
     * 
     * @param tenantId the tenant ID
     * @param printerId the printer ID
     * @return list of print jobs
     */
    List<PrintJob> findByTenantIdAndPrinterId(UUID tenantId, UUID printerId);
    
    /**
     * Finds all print jobs for a printer with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param printerId the printer ID
     * @param status the print job status
     * @return list of print jobs
     */
    List<PrintJob> findByTenantIdAndPrinterIdAndStatus(UUID tenantId, UUID printerId, PrintJobStatus status);
    
    /**
     * Finds a print job by ID and tenant ID (for tenant isolation).
     * 
     * @param id the print job ID
     * @param tenantId the tenant ID
     * @return the print job if found
     */
    Optional<PrintJob> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a print job by tenant and dedupe key (for idempotency).
     * 
     * @param tenantId the tenant ID
     * @param dedupeKey the dedupe key
     * @return the print job if found
     */
    Optional<PrintJob> findByTenantIdAndDedupeKey(UUID tenantId, String dedupeKey);
    
    /**
     * Finds all print jobs with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param status the print job status
     * @return list of print jobs
     */
    List<PrintJob> findByTenantIdAndStatus(UUID tenantId, PrintJobStatus status);
    
    /**
     * Checks if a dedupe key exists for a tenant.
     * 
     * @param tenantId the tenant ID
     * @param dedupeKey the dedupe key
     * @return true if the dedupe key exists
     */
    boolean existsByTenantIdAndDedupeKey(UUID tenantId, String dedupeKey);
    
    /**
     * Counts the number of print jobs for an order.
     * 
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * @return the count of print jobs
     */
    long countByTenantIdAndOrderId(UUID tenantId, UUID orderId);
    
    /**
     * Counts the number of print jobs for a printer with a specific status.
     * 
     * @param tenantId the tenant ID
     * @param printerId the printer ID
     * @param status the print job status
     * @return the count of print jobs
     */
    long countByTenantIdAndPrinterIdAndStatus(UUID tenantId, UUID printerId, PrintJobStatus status);
}
