# Restaurant POS SaaS

A production-ready multi-tenant Restaurant Point of Sale system built as a Spring Modulith modular monolith.

## Architecture

This system follows a modular monolith architecture with 9 distinct modules:

1. **Tenant Provisioning** - Tenant onboarding and configuration
2. **Identity and Access** - Authentication, authorization, and user management
3. **Dining Room** - Table management and real-time status
4. **Catalog** - Menu structure and item management
5. **Orders** - Order creation, modification, and confirmation
6. **Kitchen and Printing** - Print job generation and printer management
7. **Payments and Billing** - Payment processing and fiscal documents
8. **Customers** - Customer data for delivery orders
9. **Cash Register** - Cash session management and financial closings

## Technology Stack

- Java 21
- Spring Boot 3.2.1
- Spring Modulith 1.1.1
- PostgreSQL 15+
- Flyway (database migrations)
- Testcontainers (integration testing)
- QuickTheories (property-based testing)
- Micrometer (observability)

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose

## Getting Started

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Run tests

```bash
mvn test
```

## Database

The application uses PostgreSQL with Flyway for schema migrations. Migrations are located in `src/main/resources/db/migration/`.

### Connection Details

- **URL**: `jdbc:postgresql://localhost:5432/restaurant_pos`
- **Username**: `postgres`
- **Password**: `postgres`

## Module Structure

Each module follows a consistent package structure:

```
com.restaurantpos.{module}/
├── domain/          # Domain entities and value objects
├── repository/      # Data access layer
├── service/         # Application services
├── controller/      # REST controllers
└── events/          # Domain events (if applicable)
```

## API Documentation

API endpoints will be available at:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`

## Development

### Module Boundaries

Spring Modulith enforces module boundaries at compile time. Modules can only access:
- Their own internal components
- Explicitly exported APIs from other modules
- Shared domain events

### Testing

The project includes:
- Unit tests for individual components
- Integration tests with Testcontainers
- Property-based tests using QuickTheories
- Module structure verification tests

## License

Proprietary - All rights reserved
