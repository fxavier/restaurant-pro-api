package com.restaurantpos.paymentsbilling.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.model.DocumentType;

/**
 * Repository for FiscalDocument entity with tenant filtering and sequential numbering support.
 * All queries automatically filter by tenant_id.
 * 
 * Requirements: 7.6
 */
@Repository
public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {
    
    /**
     * Finds all fiscal documents for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of fiscal documents
     */
    List<FiscalDocument> findByTenantId(UUID tenantId);
    
    /**
     * Finds all fiscal documents for a specific site.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @return list of fiscal documents
     */
    List<FiscalDocument> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);
    
    /**
     * Finds all fiscal documents for a specific order.
     * 
     * @param tenantId the tenant ID
     * @param orderId the order ID
     * @return list of fiscal documents
     */
    List<FiscalDocument> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);
    
    /**
     * Finds a fiscal document by ID and tenant ID (for tenant isolation).
     * 
     * @param id the fiscal document ID
     * @param tenantId the tenant ID
     * @return the fiscal document if found
     */
    Optional<FiscalDocument> findByIdAndTenantId(UUID id, UUID tenantId);
    
    /**
     * Finds a fiscal document by document number, type, site, and tenant.
     * Used to check for duplicate document numbers.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param documentType the document type
     * @param documentNumber the document number
     * @return the fiscal document if found
     */
    Optional<FiscalDocument> findByTenantIdAndSiteIdAndDocumentTypeAndDocumentNumber(
        UUID tenantId, UUID siteId, DocumentType documentType, String documentNumber);
    
    /**
     * Finds all fiscal documents for a site and type within a date range.
     * Used for reporting and export.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param documentType the document type
     * @param startDate the start date
     * @param endDate the end date
     * @return list of fiscal documents
     */
    List<FiscalDocument> findByTenantIdAndSiteIdAndDocumentTypeAndIssuedAtBetween(
        UUID tenantId, UUID siteId, DocumentType documentType, Instant startDate, Instant endDate);
    
    /**
     * Finds the highest document number for a given tenant, site, and document type.
     * Used for sequential numbering.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param documentType the document type
     * @return the highest document number, or null if none exist
     */
    @Query("SELECT MAX(fd.documentNumber) FROM FiscalDocument fd " +
           "WHERE fd.tenantId = :tenantId AND fd.siteId = :siteId AND fd.documentType = :documentType")
    String findMaxDocumentNumber(@Param("tenantId") UUID tenantId, 
                                  @Param("siteId") UUID siteId, 
                                  @Param("documentType") DocumentType documentType);
    
    /**
     * Checks if a fiscal document exists with the given document number.
     * 
     * @param tenantId the tenant ID
     * @param siteId the site ID
     * @param documentType the document type
     * @param documentNumber the document number
     * @return true if document exists
     */
    boolean existsByTenantIdAndSiteIdAndDocumentTypeAndDocumentNumber(
        UUID tenantId, UUID siteId, DocumentType documentType, String documentNumber);


    /**
     * Finds all fiscal documents for a tenant within a date range.
     * Used for SAF-T export.
     *
     * @param tenantId the tenant ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of fiscal documents
     */
    List<FiscalDocument> findByTenantIdAndIssuedAtBetween(UUID tenantId, Instant startDate, Instant endDate);

}
