package com.restaurantpos.fiscalexport.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurantpos.catalog.entity.Item;
import com.restaurantpos.catalog.repository.ItemRepository;
import com.restaurantpos.customers.entity.Customer;
import com.restaurantpos.customers.repository.CustomerRepository;
import com.restaurantpos.paymentsbilling.entity.FiscalDocument;
import com.restaurantpos.paymentsbilling.entity.Payment;
import com.restaurantpos.paymentsbilling.repository.FiscalDocumentRepository;
import com.restaurantpos.paymentsbilling.repository.PaymentRepository;

/**
 * Service for generating SAF-T PT (Standard Audit File for Tax - Portuguese) exports.
 * Generates XML files containing fiscal documents, payments, customers, and items
 * for a specified date range and tenant.
 * 
 * Requirements: 17.1, 17.2
 */
@Service
@Transactional(readOnly = true)
public class SaftExportService {
    
    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    
    public SaftExportService(
            FiscalDocumentRepository fiscalDocumentRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            ItemRepository itemRepository) {
        this.fiscalDocumentRepository = fiscalDocumentRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
    }
    
    /**
     * Generates a SAF-T PT XML export for the specified tenant and date range.
     * Queries all fiscal documents, payments, customers, and items within the date range
     * and generates XML according to SAF-T PT schema.
     * 
     * @param tenantId the tenant ID to export data for
     * @param startDate the start date of the export range (inclusive)
     * @param endDate the end date of the export range (inclusive)
     * @return XML string containing the SAF-T PT export
     * 
     * Requirements: 17.1, 17.2
     */
    public String generateExport(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        // Convert LocalDate to Instant for database queries
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Query all fiscal documents in date range
        List<FiscalDocument> fiscalDocuments = fiscalDocumentRepository
                .findByTenantIdAndIssuedAtBetween(tenantId, startInstant, endInstant);
        
        // Query all payments for the fiscal documents
        List<UUID> orderIds = fiscalDocuments.stream()
                .map(FiscalDocument::getOrderId)
                .distinct()
                .toList();
        List<Payment> payments = paymentRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
        
        // Query all customers referenced in the fiscal documents
        List<Customer> customers = customerRepository.findByTenantId(tenantId);
        
        // Query all items for the tenant
        List<Item> items = itemRepository.findByTenantId(tenantId);
        
        // Generate SAF-T PT XML
        return generateSaftXml(tenantId, fiscalDocuments, payments, customers, items, startDate, endDate);
    }
    
    /**
     * Generates the SAF-T PT XML structure from the queried data.
     */
    private String generateSaftXml(
            UUID tenantId,
            List<FiscalDocument> fiscalDocuments,
            List<Payment> payments,
            List<Customer> customers,
            List<Item> items,
            LocalDate startDate,
            LocalDate endDate) {
        
        StringBuilder xml = new StringBuilder();
        
        // XML Declaration
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        // Root element with namespace
        xml.append("<AuditFile xmlns=\"urn:OECD:StandardAuditFile-Tax:PT_1.04_01\">\n");
        
        // Header section
        appendHeader(xml, tenantId, startDate, endDate);
        
        // Master Files section (customers and items)
        appendMasterFiles(xml, customers, items);
        
        // Source Documents section (fiscal documents and payments)
        appendSourceDocuments(xml, fiscalDocuments, payments);
        
        // Close root element
        xml.append("</AuditFile>");
        
        return xml.toString();
    }
    
    /**
     * Appends the Header section to the SAF-T XML.
     */
    private void appendHeader(StringBuilder xml, UUID tenantId, LocalDate startDate, LocalDate endDate) {
        xml.append("  <Header>\n");
        xml.append("    <AuditFileVersion>1.04_01</AuditFileVersion>\n");
        xml.append("    <CompanyID>").append(escapeXml(tenantId.toString())).append("</CompanyID>\n");
        xml.append("    <TaxRegistrationNumber>999999990</TaxRegistrationNumber>\n");
        xml.append("    <TaxAccountingBasis>F</TaxAccountingBasis>\n");
        xml.append("    <CompanyName>Restaurant POS Tenant</CompanyName>\n");
        xml.append("    <BusinessName>Restaurant POS</BusinessName>\n");
        xml.append("    <CompanyAddress>\n");
        xml.append("      <AddressDetail>Address Line 1</AddressDetail>\n");
        xml.append("      <City>Lisbon</City>\n");
        xml.append("      <PostalCode>1000-001</PostalCode>\n");
        xml.append("      <Country>PT</Country>\n");
        xml.append("    </CompanyAddress>\n");
        xml.append("    <FiscalYear>").append(startDate.getYear()).append("</FiscalYear>\n");
        xml.append("    <StartDate>").append(startDate).append("</StartDate>\n");
        xml.append("    <EndDate>").append(endDate).append("</EndDate>\n");
        xml.append("    <CurrencyCode>EUR</CurrencyCode>\n");
        xml.append("    <DateCreated>").append(LocalDate.now()).append("</DateCreated>\n");
        xml.append("    <TaxEntity>Global</TaxEntity>\n");
        xml.append("    <ProductCompanyTaxID>999999990</ProductCompanyTaxID>\n");
        xml.append("    <SoftwareCertificateNumber>0</SoftwareCertificateNumber>\n");
        xml.append("    <ProductID>Restaurant POS/1.0</ProductID>\n");
        xml.append("    <ProductVersion>1.0</ProductVersion>\n");
        xml.append("  </Header>\n");
    }
    
    /**
     * Appends the MasterFiles section (customers and items) to the SAF-T XML.
     */
    private void appendMasterFiles(StringBuilder xml, List<Customer> customers, List<Item> items) {
        xml.append("  <MasterFiles>\n");
        
        // Customers
        for (Customer customer : customers) {
            xml.append("    <Customer>\n");
            xml.append("      <CustomerID>").append(escapeXml(customer.getId().toString())).append("</CustomerID>\n");
            xml.append("      <AccountID>Desconhecido</AccountID>\n");
            xml.append("      <CustomerTaxID>999999990</CustomerTaxID>\n");
            xml.append("      <CompanyName>").append(escapeXml(customer.getName())).append("</CompanyName>\n");
            xml.append("      <BillingAddress>\n");
            xml.append("        <AddressDetail>").append(escapeXml(customer.getAddress() != null ? customer.getAddress() : "N/A")).append("</AddressDetail>\n");
            xml.append("        <City>Unknown</City>\n");
            xml.append("        <PostalCode>0000-000</PostalCode>\n");
            xml.append("        <Country>PT</Country>\n");
            xml.append("      </BillingAddress>\n");
            xml.append("      <Telephone>").append(escapeXml(customer.getPhone())).append("</Telephone>\n");
            xml.append("      <SelfBillingIndicator>0</SelfBillingIndicator>\n");
            xml.append("    </Customer>\n");
        }
        
        // Products (Items)
        for (Item item : items) {
            xml.append("    <Product>\n");
            xml.append("      <ProductType>P</ProductType>\n");
            xml.append("      <ProductCode>").append(escapeXml(item.getId().toString())).append("</ProductCode>\n");
            xml.append("      <ProductGroup>Food</ProductGroup>\n");
            xml.append("      <ProductDescription>").append(escapeXml(item.getName())).append("</ProductDescription>\n");
            xml.append("      <ProductNumberCode>").append(escapeXml(item.getId().toString())).append("</ProductNumberCode>\n");
            xml.append("    </Product>\n");
        }
        
        xml.append("  </MasterFiles>\n");
    }
    
    /**
     * Appends the SourceDocuments section (fiscal documents and payments) to the SAF-T XML.
     */
    private void appendSourceDocuments(StringBuilder xml, List<FiscalDocument> fiscalDocuments, List<Payment> payments) {
        xml.append("  <SourceDocuments>\n");
        xml.append("    <SalesInvoices>\n");
        xml.append("      <NumberOfEntries>").append(fiscalDocuments.size()).append("</NumberOfEntries>\n");
        xml.append("      <TotalDebit>").append(calculateTotalDebit(fiscalDocuments)).append("</TotalDebit>\n");
        xml.append("      <TotalCredit>").append(calculateTotalCredit(fiscalDocuments)).append("</TotalCredit>\n");
        
        // Fiscal Documents
        for (FiscalDocument doc : fiscalDocuments) {
            xml.append("      <Invoice>\n");
            xml.append("        <InvoiceNo>").append(escapeXml(doc.getDocumentNumber())).append("</InvoiceNo>\n");
            xml.append("        <DocumentStatus>\n");
            xml.append("          <InvoiceStatus>").append(doc.isVoided() ? "A" : "N").append("</InvoiceStatus>\n");
            xml.append("          <InvoiceStatusDate>").append(doc.getIssuedAt()).append("</InvoiceStatusDate>\n");
            xml.append("          <SourceID>User</SourceID>\n");
            xml.append("          <SourceBilling>P</SourceBilling>\n");
            xml.append("        </DocumentStatus>\n");
            xml.append("        <Hash>0</Hash>\n");
            xml.append("        <HashControl>0</HashControl>\n");
            xml.append("        <InvoiceDate>").append(doc.getIssuedAt().toString().substring(0, 10)).append("</InvoiceDate>\n");
            xml.append("        <InvoiceType>").append(doc.getDocumentType()).append("</InvoiceType>\n");
            xml.append("        <SourceID>User</SourceID>\n");
            xml.append("        <SystemEntryDate>").append(doc.getCreatedAt()).append("</SystemEntryDate>\n");
            xml.append("        <CustomerID>").append(doc.getCustomerNif() != null ? escapeXml(doc.getCustomerNif()) : "Desconhecido").append("</CustomerID>\n");
            xml.append("        <Line>\n");
            xml.append("          <LineNumber>1</LineNumber>\n");
            xml.append("          <ProductCode>PRODUCT</ProductCode>\n");
            xml.append("          <Quantity>1</Quantity>\n");
            xml.append("          <UnitOfMeasure>UN</UnitOfMeasure>\n");
            xml.append("          <UnitPrice>").append(doc.getAmount()).append("</UnitPrice>\n");
            xml.append("          <TaxPointDate>").append(doc.getIssuedAt().toString().substring(0, 10)).append("</TaxPointDate>\n");
            xml.append("          <Description>Order Items</Description>\n");
            xml.append("          <CreditAmount>").append(doc.getAmount()).append("</CreditAmount>\n");
            xml.append("          <Tax>\n");
            xml.append("            <TaxType>IVA</TaxType>\n");
            xml.append("            <TaxCountryRegion>PT</TaxCountryRegion>\n");
            xml.append("            <TaxCode>NOR</TaxCode>\n");
            xml.append("            <TaxPercentage>23.00</TaxPercentage>\n");
            xml.append("          </Tax>\n");
            xml.append("        </Line>\n");
            xml.append("        <DocumentTotals>\n");
            xml.append("          <TaxPayable>0.00</TaxPayable>\n");
            xml.append("          <NetTotal>").append(doc.getAmount()).append("</NetTotal>\n");
            xml.append("          <GrossTotal>").append(doc.getAmount()).append("</GrossTotal>\n");
            
            // Add payment information
            List<Payment> docPayments = payments.stream()
                    .filter(p -> p.getOrderId().equals(doc.getOrderId()))
                    .toList();
            for (Payment payment : docPayments) {
                xml.append("          <Payment>\n");
                xml.append("            <PaymentMechanism>").append(payment.getPaymentMethod()).append("</PaymentMechanism>\n");
                xml.append("            <PaymentAmount>").append(payment.getAmount()).append("</PaymentAmount>\n");
                xml.append("            <PaymentDate>").append(payment.getCreatedAt().toString().substring(0, 10)).append("</PaymentDate>\n");
                xml.append("          </Payment>\n");
            }
            
            xml.append("        </DocumentTotals>\n");
            xml.append("      </Invoice>\n");
        }
        
        xml.append("    </SalesInvoices>\n");
        xml.append("  </SourceDocuments>\n");
    }
    
    /**
     * Calculates total debit amount from fiscal documents.
     */
    private String calculateTotalDebit(List<FiscalDocument> fiscalDocuments) {
        return fiscalDocuments.stream()
                .filter(doc -> !doc.isVoided() && !doc.isCreditNote())
                .map(FiscalDocument::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .toString();
    }
    
    /**
     * Calculates total credit amount from fiscal documents.
     */
    private String calculateTotalCredit(List<FiscalDocument> fiscalDocuments) {
        return fiscalDocuments.stream()
                .filter(doc -> !doc.isVoided() && doc.isCreditNote())
                .map(FiscalDocument::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .toString();
    }
    
    /**
     * Escapes special XML characters in text content.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
