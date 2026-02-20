package com.restaurantpos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Builder class for creating test data in integration tests.
 * 
 * Provides fluent API for creating entities with sensible defaults
 * and the ability to override specific fields.
 * 
 * Usage:
 * <pre>
 * UUID tenantId = TestDataBuilder.tenant(jdbcTemplate)
 *     .name("Test Restaurant")
 *     .build();
 * </pre>
 */
public class TestDataBuilder {

    // ========================================================================
    // Tenant Builder
    // ========================================================================

    public static TenantBuilder tenant(JdbcTemplate jdbcTemplate) {
        return new TenantBuilder(jdbcTemplate);
    }

    public static class TenantBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private String name = "Test Tenant";
        private String status = "ACTIVE";

        private TenantBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public TenantBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TenantBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TenantBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UUID build() {
            jdbcTemplate.update(
                "INSERT INTO tenants (id, name, status) VALUES (?, ?, ?)",
                id, name, status
            );
            return id;
        }
    }

    // ========================================================================
    // Site Builder
    // ========================================================================

    public static SiteBuilder site(JdbcTemplate jdbcTemplate) {
        return new SiteBuilder(jdbcTemplate);
    }

    public static class SiteBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private String name = "Test Site";
        private String address = "123 Test St";
        private String timezone = "UTC";

        private SiteBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public SiteBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SiteBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public SiteBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SiteBuilder address(String address) {
            this.address = address;
            return this;
        }

        public SiteBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public UUID build() {
            if (tenantId == null) {
                throw new IllegalStateException("tenantId is required");
            }
            jdbcTemplate.update(
                "INSERT INTO sites (id, tenant_id, name, address, timezone) VALUES (?, ?, ?, ?, ?)",
                id, tenantId, name, address, timezone
            );
            return id;
        }
    }

    // ========================================================================
    // User Builder
    // ========================================================================

    public static UserBuilder user(JdbcTemplate jdbcTemplate) {
        return new UserBuilder(jdbcTemplate);
    }

    public static class UserBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private String username = "testuser";
        private String passwordHash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYKKxd4qQyy"; // "password"
        private String email = "test@example.com";
        private String role = "WAITER";
        private String status = "ACTIVE";

        private UserBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UUID build() {
            if (tenantId == null) {
                throw new IllegalStateException("tenantId is required");
            }
            jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, username, passwordHash, email, role, status
            );
            return id;
        }
    }

    // ========================================================================
    // Dining Table Builder
    // ========================================================================

    public static DiningTableBuilder diningTable(JdbcTemplate jdbcTemplate) {
        return new DiningTableBuilder(jdbcTemplate);
    }

    public static class DiningTableBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID siteId;
        private String tableNumber = "T1";
        private String area = "Main";
        private String status = "AVAILABLE";
        private Integer capacity = 4;

        private DiningTableBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public DiningTableBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public DiningTableBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public DiningTableBuilder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public DiningTableBuilder tableNumber(String tableNumber) {
            this.tableNumber = tableNumber;
            return this;
        }

        public DiningTableBuilder area(String area) {
            this.area = area;
            return this;
        }

        public DiningTableBuilder status(String status) {
            this.status = status;
            return this;
        }

        public DiningTableBuilder capacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public UUID build() {
            if (tenantId == null || siteId == null) {
                throw new IllegalStateException("tenantId and siteId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO dining_tables (id, tenant_id, site_id, table_number, area, status, capacity) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, siteId, tableNumber, area, status, capacity
            );
            return id;
        }
    }

    // ========================================================================
    // Family Builder
    // ========================================================================

    public static FamilyBuilder family(JdbcTemplate jdbcTemplate) {
        return new FamilyBuilder(jdbcTemplate);
    }

    public static class FamilyBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private String name = "Test Family";
        private Integer displayOrder = 0;
        private Boolean active = true;

        private FamilyBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public FamilyBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public FamilyBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public FamilyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FamilyBuilder displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public FamilyBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public UUID build() {
            if (tenantId == null) {
                throw new IllegalStateException("tenantId is required");
            }
            jdbcTemplate.update(
                "INSERT INTO families (id, tenant_id, name, display_order, active) VALUES (?, ?, ?, ?, ?)",
                id, tenantId, name, displayOrder, active
            );
            return id;
        }
    }

    // ========================================================================
    // Subfamily Builder
    // ========================================================================

    public static SubfamilyBuilder subfamily(JdbcTemplate jdbcTemplate) {
        return new SubfamilyBuilder(jdbcTemplate);
    }

    public static class SubfamilyBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID familyId;
        private String name = "Test Subfamily";
        private Integer displayOrder = 0;
        private Boolean active = true;

        private SubfamilyBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public SubfamilyBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SubfamilyBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public SubfamilyBuilder familyId(UUID familyId) {
            this.familyId = familyId;
            return this;
        }

        public SubfamilyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SubfamilyBuilder displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public SubfamilyBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public UUID build() {
            if (tenantId == null || familyId == null) {
                throw new IllegalStateException("tenantId and familyId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO subfamilies (id, tenant_id, family_id, name, display_order, active) VALUES (?, ?, ?, ?, ?, ?)",
                id, tenantId, familyId, name, displayOrder, active
            );
            return id;
        }
    }

    // ========================================================================
    // Item Builder
    // ========================================================================

    public static ItemBuilder item(JdbcTemplate jdbcTemplate) {
        return new ItemBuilder(jdbcTemplate);
    }

    public static class ItemBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID subfamilyId;
        private String name = "Test Item";
        private String description = "Test Description";
        private BigDecimal basePrice = new BigDecimal("10.00");
        private Boolean available = true;

        private ItemBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public ItemBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ItemBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public ItemBuilder subfamilyId(UUID subfamilyId) {
            this.subfamilyId = subfamilyId;
            return this;
        }

        public ItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ItemBuilder basePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public ItemBuilder available(Boolean available) {
            this.available = available;
            return this;
        }

        public UUID build() {
            if (tenantId == null || subfamilyId == null) {
                throw new IllegalStateException("tenantId and subfamilyId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO items (id, tenant_id, subfamily_id, name, description, base_price, available) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, subfamilyId, name, description, basePrice, available
            );
            return id;
        }
    }

    // ========================================================================
    // Order Builder
    // ========================================================================

    public static OrderBuilder order(JdbcTemplate jdbcTemplate) {
        return new OrderBuilder(jdbcTemplate);
    }

    public static class OrderBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID siteId;
        private UUID tableId;
        private UUID customerId;
        private String orderType = "DINE_IN";
        private String status = "OPEN";
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private OrderBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public OrderBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public OrderBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public OrderBuilder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public OrderBuilder tableId(UUID tableId) {
            this.tableId = tableId;
            return this;
        }

        public OrderBuilder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public OrderBuilder orderType(String orderType) {
            this.orderType = orderType;
            return this;
        }

        public OrderBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public UUID build() {
            if (tenantId == null || siteId == null) {
                throw new IllegalStateException("tenantId and siteId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, site_id, table_id, customer_id, order_type, status, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, siteId, tableId, customerId, orderType, status, totalAmount
            );
            return id;
        }
    }

    // ========================================================================
    // Order Line Builder
    // ========================================================================

    public static OrderLineBuilder orderLine(JdbcTemplate jdbcTemplate) {
        return new OrderLineBuilder(jdbcTemplate);
    }

    public static class OrderLineBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID orderId;
        private UUID itemId;
        private Integer quantity = 1;
        private BigDecimal unitPrice = new BigDecimal("10.00");
        private String status = "PENDING";
        private String notes;

        private OrderLineBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public OrderLineBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public OrderLineBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderLineBuilder itemId(UUID itemId) {
            this.itemId = itemId;
            return this;
        }

        public OrderLineBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderLineBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public OrderLineBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderLineBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public UUID build() {
            if (orderId == null || itemId == null) {
                throw new IllegalStateException("orderId and itemId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO order_lines (id, order_id, item_id, quantity, unit_price, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, orderId, itemId, quantity, unitPrice, status, notes
            );
            return id;
        }
    }

    // ========================================================================
    // Customer Builder
    // ========================================================================

    public static CustomerBuilder customer(JdbcTemplate jdbcTemplate) {
        return new CustomerBuilder(jdbcTemplate);
    }

    public static class CustomerBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private String name = "Test Customer";
        private String phone = "5551234567";
        private String address = "123 Customer St";
        private String deliveryNotes;

        private CustomerBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public CustomerBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CustomerBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CustomerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CustomerBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public CustomerBuilder address(String address) {
            this.address = address;
            return this;
        }

        public CustomerBuilder deliveryNotes(String deliveryNotes) {
            this.deliveryNotes = deliveryNotes;
            return this;
        }

        public UUID build() {
            if (tenantId == null) {
                throw new IllegalStateException("tenantId is required");
            }
            jdbcTemplate.update(
                "INSERT INTO customers (id, tenant_id, name, phone, address, delivery_notes) VALUES (?, ?, ?, ?, ?, ?)",
                id, tenantId, name, phone, address, deliveryNotes
            );
            return id;
        }
    }

    // ========================================================================
    // Printer Builder
    // ========================================================================

    public static PrinterBuilder printer(JdbcTemplate jdbcTemplate) {
        return new PrinterBuilder(jdbcTemplate);
    }

    public static class PrinterBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID siteId;
        private String name = "Test Printer";
        private String ipAddress = "192.168.1.100";
        private String zone = "Kitchen";
        private String status = "NORMAL";

        private PrinterBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public PrinterBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PrinterBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public PrinterBuilder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public PrinterBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PrinterBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public PrinterBuilder zone(String zone) {
            this.zone = zone;
            return this;
        }

        public PrinterBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UUID build() {
            if (tenantId == null || siteId == null) {
                throw new IllegalStateException("tenantId and siteId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO printers (id, tenant_id, site_id, name, ip_address, zone, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, siteId, name, ipAddress, zone, status
            );
            return id;
        }
    }

    // ========================================================================
    // Payment Builder
    // ========================================================================

    public static PaymentBuilder payment(JdbcTemplate jdbcTemplate) {
        return new PaymentBuilder(jdbcTemplate);
    }

    public static class PaymentBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID orderId;
        private BigDecimal amount = new BigDecimal("50.00");
        private String paymentMethod = "CASH";
        private String status = "COMPLETED";
        private String idempotencyKey = UUID.randomUUID().toString();

        private PaymentBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public PaymentBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public PaymentBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public UUID build() {
            if (tenantId == null || orderId == null) {
                throw new IllegalStateException("tenantId and orderId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO payments (id, tenant_id, order_id, amount, payment_method, status, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, orderId, amount, paymentMethod, status, idempotencyKey
            );
            return id;
        }
    }

    // ========================================================================
    // Cash Register Builder
    // ========================================================================

    public static CashRegisterBuilder cashRegister(JdbcTemplate jdbcTemplate) {
        return new CashRegisterBuilder(jdbcTemplate);
    }

    public static class CashRegisterBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID siteId;
        private String registerNumber = "REG-1";
        private String status = "ACTIVE";

        private CashRegisterBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public CashRegisterBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CashRegisterBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CashRegisterBuilder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public CashRegisterBuilder registerNumber(String registerNumber) {
            this.registerNumber = registerNumber;
            return this;
        }

        public CashRegisterBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UUID build() {
            if (tenantId == null || siteId == null) {
                throw new IllegalStateException("tenantId and siteId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO cash_registers (id, tenant_id, site_id, register_number, status) VALUES (?, ?, ?, ?, ?)",
                id, tenantId, siteId, registerNumber, status
            );
            return id;
        }
    }

    // ========================================================================
    // Cash Session Builder
    // ========================================================================

    public static CashSessionBuilder cashSession(JdbcTemplate jdbcTemplate) {
        return new CashSessionBuilder(jdbcTemplate);
    }

    public static class CashSessionBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID registerId;
        private UUID employeeId;
        private BigDecimal openingAmount = new BigDecimal("100.00");
        private String status = "OPEN";
        private Instant openedAt = Instant.now();

        private CashSessionBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public CashSessionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CashSessionBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CashSessionBuilder registerId(UUID registerId) {
            this.registerId = registerId;
            return this;
        }

        public CashSessionBuilder employeeId(UUID employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public CashSessionBuilder openingAmount(BigDecimal openingAmount) {
            this.openingAmount = openingAmount;
            return this;
        }

        public CashSessionBuilder status(String status) {
            this.status = status;
            return this;
        }

        public CashSessionBuilder openedAt(Instant openedAt) {
            this.openedAt = openedAt;
            return this;
        }

        public UUID build() {
            if (tenantId == null || registerId == null || employeeId == null) {
                throw new IllegalStateException("tenantId, registerId, and employeeId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO cash_sessions (id, tenant_id, register_id, employee_id, opening_amount, status, opened_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, registerId, employeeId, openingAmount, status, java.sql.Timestamp.from(openedAt)
            );
            return id;
        }
    }

    // ========================================================================
    // Fiscal Document Builder
    // ========================================================================

    public static FiscalDocumentBuilder fiscalDocument(JdbcTemplate jdbcTemplate) {
        return new FiscalDocumentBuilder(jdbcTemplate);
    }

    public static class FiscalDocumentBuilder {
        private final JdbcTemplate jdbcTemplate;
        private UUID id = UUID.randomUUID();
        private UUID tenantId;
        private UUID siteId;
        private String documentType = "RECEIPT";
        private String documentNumber = "REC-001";
        private UUID orderId;
        private BigDecimal amount = new BigDecimal("50.00");
        private String customerNif;
        private Instant issuedAt = Instant.now();

        private FiscalDocumentBuilder(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        public FiscalDocumentBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public FiscalDocumentBuilder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public FiscalDocumentBuilder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public FiscalDocumentBuilder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        public FiscalDocumentBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        public FiscalDocumentBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public FiscalDocumentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public FiscalDocumentBuilder customerNif(String customerNif) {
            this.customerNif = customerNif;
            return this;
        }

        public FiscalDocumentBuilder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public UUID build() {
            if (tenantId == null || siteId == null || orderId == null) {
                throw new IllegalStateException("tenantId, siteId, and orderId are required");
            }
            jdbcTemplate.update(
                "INSERT INTO fiscal_documents (id, tenant_id, site_id, document_type, document_number, order_id, amount, customer_nif, issued_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, siteId, documentType, documentNumber, orderId, amount, customerNif, java.sql.Timestamp.from(issuedAt)
            );
            return id;
        }
    }
}
