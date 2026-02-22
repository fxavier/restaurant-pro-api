# Restaurant POS SaaS

A production-ready multi-tenant Restaurant Point of Sale system built as a Spring Modulith modular monolith with PostgreSQL.

## Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [How to Run Locally](#how-to-run-locally)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Database](#database)
- [Module Structure](#module-structure)
- [Security](#security)
- [Observability](#observability)
- [Development](#development)

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

- **Java 21** - Programming language
- **Spring Boot 3.2.1** - Application framework
- **Spring Modulith 1.1.1** - Modular monolith architecture
- **Spring Security** - Authentication and authorization
- **PostgreSQL 15+** - Database
- **Flyway** - Database migrations
- **JWT** - Token-based authentication
- **Testcontainers** - Integration testing
- **QuickTheories** - Property-based testing
- **Micrometer** - Metrics and observability
- **Logback** - Structured logging
- **SpringDoc OpenAPI** - API documentation

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21 or higher** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Docker and Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/downloads)

## How to Run Locally

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd restaurant-pos-saas
```

### Step 2: Start PostgreSQL Database

The project includes a `docker-compose.yml` file that sets up PostgreSQL:

```bash
docker-compose up -d
```

This will start PostgreSQL on port 5432 with:
- Database: `restaurant_pos`
- Username: `postgres`
- Password: `postgres`

Verify the database is running:

```bash
docker-compose ps
```

### Step 3: Build the Project

```bash
mvn clean install
```

This will:
- Compile the source code
- Run all tests (unit, integration, property-based)
- Package the application

To skip tests during build:

```bash
mvn clean install -DskipTests
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

Alternatively, run the JAR directly:

```bash
java -jar target/restaurant-pos-saas-1.0.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### Step 5: Verify the Application is Running

Check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Step 6: Access API Documentation

Open your browser and navigate to:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Step 7: Stop the Application

Press `Ctrl+C` in the terminal where the application is running.

To stop the database:

```bash
docker-compose down
```

To stop and remove all data:

```bash
docker-compose down -v
```

## API Documentation

### Interactive API Documentation

The application provides interactive API documentation using Swagger UI:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Specification**: http://localhost:8080/v3/api-docs

### Authentication

Most API endpoints require JWT authentication. To authenticate:

1. **Register** a new account (if you don't have one):

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "username": "myusername",
    "password": "SecurePassword123!",
    "email": "user@example.com",
    "role": "WAITER"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "uuid",
  "username": "myusername",
  "tenantId": "uuid",
  "role": "WAITER",
  "expiresIn": 900
}
```

2. **Login** to get JWT tokens (if you already have an account):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "username": "myusername",
    "password": "SecurePassword123!"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "uuid",
  "username": "myusername",
  "tenantId": "uuid",
  "role": "WAITER",
  "expiresIn": 900
}
```

3. **Use the access token** in subsequent requests:

```bash
curl -X GET http://localhost:8080/api/tables \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### Key API Endpoints

#### Authentication
- `POST /api/auth/register` - Register new account
- `POST /api/auth/login` - Authenticate user
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Logout user

#### Tables
- `GET /api/tables` - Get table map
- `POST /api/tables/{id}/open` - Open table
- `POST /api/tables/{id}/close` - Close table

#### Orders
- `POST /api/orders` - Create order
- `GET /api/orders/{id}` - Get order details
- `POST /api/orders/{id}/lines` - Add order line
- `POST /api/orders/{id}/confirm` - Confirm order

#### Payments
- `POST /api/payments` - Process payment
- `GET /api/orders/{orderId}/payments` - Get order payments

#### Cash Register
- `POST /api/cash/sessions` - Open cash session
- `POST /api/cash/sessions/{id}/close` - Close session

For complete API documentation, visit the Swagger UI.

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Categories

**Unit Tests Only:**
```bash
mvn test -Dtest="*Test"
```

**Integration Tests:**
```bash
mvn test -Dtest="*IntegrationTest"
```

**Property-Based Tests:**
```bash
mvn test -Dtest="*PropertyTest"
```

### Test Coverage

Generate test coverage report:

```bash
mvn clean test jacoco:report
```

View the report at: `target/site/jacoco/index.html`

### Test Types

The project includes three types of tests:

1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test complete flows with real database (Testcontainers)
3. **Property-Based Tests** - Test universal properties across many inputs (QuickTheories)

## Database

### Connection Details

- **URL**: `jdbc:postgresql://localhost:5432/restaurant_pos`
- **Username**: `postgres`
- **Password**: `postgres`

### Migrations

Database schema is managed by Flyway. Migrations are located in:

```
src/main/resources/db/migration/
├── V2__baseline.sql          # Core schema
├── V3__indexes.sql           # Performance indexes
├── V4__rls_policies.sql      # Row Level Security (optional)
└── V6__fix_rls_policies.sql  # RLS fixes
```

Migrations run automatically on application startup.

### Manual Migration

To run migrations manually:

```bash
mvn flyway:migrate
```

To see migration status:

```bash
mvn flyway:info
```

### Connect to Database

Using psql:

```bash
docker exec -it restaurant-pos-saas-postgres-1 psql -U postgres -d restaurant_pos
```

Using any PostgreSQL client:
- Host: localhost
- Port: 5432
- Database: restaurant_pos
- Username: postgres
- Password: postgres

## Module Structure

Each module follows a consistent package structure:

```
com.restaurantpos.{module}/
├── entity/          # JPA entities
├── repository/      # Data access layer
├── service/         # Application services
├── controller/      # REST controllers
├── model/           # Enums and value objects
├── dto/             # Data transfer objects
├── listener/        # Event listeners
└── package-info.java # Module definition
```

### Module Dependencies

Modules communicate through:
- **Application Services** - Direct method calls to exported APIs
- **Domain Events** - Asynchronous event-driven communication
- **Shared Entities** - Read-only access to domain entities

For detailed module documentation, see:
- [Module Structure Documentation](docs/architecture/module-structure.md)
- [Architecture Decision Records](docs/architecture/adr/)

## Security

### Multi-Tenancy

The system implements multi-tenancy with:
- Tenant ID in JWT claims
- Automatic tenant filtering on all queries
- Optional PostgreSQL Row Level Security (RLS)

### Authentication

- JWT-based authentication
- Access tokens (15 min expiry)
- Refresh tokens (7 day expiry)
- BCrypt password hashing

### Authorization

Role-based access control (RBAC) with roles:
- ADMIN - Full system access
- MANAGER - Site management
- CASHIER - Payment operations
- WAITER - Order management
- KITCHEN_STAFF - Kitchen operations

### Security Features

- SQL injection prevention
- Sensitive data masking in logs
- Rate limiting on authentication endpoints
- CSRF protection
- HTTPS enforcement (production)

## Observability

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

Available metrics:
- Request rates and response times
- Database connection pool stats
- JVM memory and GC metrics
- Custom business metrics

### Structured Logging

Logs are output in JSON format with:
- Timestamp
- Log level
- Tenant ID
- User ID
- Trace ID
- Message

### Log Files

Logs are written to:
- Console (stdout)
- `logs/application.log` (file)

## Development

### Module Boundaries

Spring Modulith enforces module boundaries at compile time. Modules can only access:
- Their own internal components
- Explicitly exported APIs from other modules
- Shared domain events

### Verify Module Structure

```bash
mvn test -Dtest=ModularityTests
```

### Code Style

The project follows standard Java conventions:
- 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names
- Add JavaDoc for public APIs

### Adding a New Module

1. Create package: `com.restaurantpos.{module}`
2. Add `package-info.java` with `@org.springframework.modulith.ApplicationModule`
3. Follow the standard module structure
4. Update module documentation

### Common Issues

**Database connection failed:**
- Ensure PostgreSQL is running: `docker-compose ps`
- Check connection details in `application.yml`

**Tests failing:**
- Ensure Docker is running (for Testcontainers)
- Check test logs in `target/surefire-reports/`

**Port 8080 already in use:**
- Change port in `application.yml`: `server.port=8081`
- Or stop the process using port 8080

## Configuration

### Application Properties

Configuration is in `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/restaurant_pos
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  access-token-expiry: 900000    # 15 minutes
  refresh-token-expiry: 604800000 # 7 days
```

### Environment Variables

Override configuration with environment variables:

```bash
export JWT_SECRET=your-production-secret
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/restaurant_pos
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=prod_password
```

## License

Proprietary - All rights reserved

## Support

For questions or issues, contact:
- Email: support@restaurantpos.com
- Documentation: [docs/](docs/)
- Architecture Decisions: [docs/architecture/adr/](docs/architecture/adr/)

