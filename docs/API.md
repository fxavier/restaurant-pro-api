# Restaurant POS SaaS - API Documentation

## Overview

This document provides detailed information about the REST API endpoints available in the Restaurant POS SaaS system.

## Base URL

- **Local Development**: `http://localhost:8080`
- **Production**: `https://api.restaurantpos.com`

## Authentication

All API endpoints (except `/api/auth/login` and `/api/auth/register`) require JWT authentication.

### Registering a New Account

**Endpoint**: `POST /api/auth/register`

**Request Body**:
```json
{
  "tenantId": "uuid",
  "username": "string",
  "password": "string",
  "email": "string (optional)",
  "role": "string"
}
```

**Validation Rules**:
- `tenantId`: Required, must be a valid UUID
- `username`: Required, 3-100 characters, must be unique within tenant
- `password`: Required, minimum 8 characters
- `email`: Optional, maximum 255 characters, must be unique within tenant if provided
- `role`: Required, must be one of: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN_STAFF

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "uuid",
  "username": "string",
  "tenantId": "uuid",
  "role": "string",
  "expiresIn": 900
}
```

**Notes**:
- Registration automatically logs in the user and returns tokens
- Passwords are hashed using BCrypt before storage
- Usernames must be unique per tenant (same username can exist in different tenants)
- Email addresses must be unique per tenant if provided

### Obtaining a Token

**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "username": "admin",
  "password": "password"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

### Using the Token

Include the access token in the `Authorization` header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### Token Refresh

**Endpoint**: `POST /api/auth/refresh`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

## API Endpoints

### Authentication Module

#### Login
- **POST** `/api/auth/login`
- **Description**: Authenticate user and obtain JWT tokens
- **Authentication**: Not required
- **Request Body**:
  ```json
  {
    "tenantId": "uuid",
    "username": "string",
    "password": "string"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "userId": "uuid",
    "username": "string",
    "tenantId": "uuid",
    "role": "string",
    "expiresIn": 900
  }
  ```

#### Register
- **POST** `/api/auth/register`
- **Description**: Register a new user account
- **Authentication**: Not required
- **Request Body**:
  ```json
  {
    "tenantId": "uuid",
    "username": "string (3-100 characters)",
    "password": "string (min 8 characters)",
    "email": "string (optional, max 255 characters)",
    "role": "ADMIN|MANAGER|CASHIER|WAITER|KITCHEN_STAFF"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "userId": "uuid",
    "username": "string",
    "tenantId": "uuid",
    "role": "string",
    "expiresIn": 900
  }
  ```
- **Error Responses**:
  - `400 Bad Request` - Validation error, username/email already exists, or invalid role
  - `422 Unprocessable Entity` - Business rule violation

#### Refresh Token
- **POST** `/api/auth/refresh`
- **Description**: Refresh access token using refresh token
- **Authentication**: Not required
- **Request Body**:
  ```json
  {
    "refreshToken": "string"
  }
  ```
- **Response**: `200 OK`

#### Logout
- **POST** `/api/auth/logout`
- **Description**: Invalidate refresh token
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "refreshToken": "string"
  }
  ```
- **Response**: `200 OK`

### Tenant Provisioning Module

#### Create Tenant
- **POST** `/api/tenants`
- **Description**: Create a new tenant (admin only)
- **Authentication**: Required (ADMIN role)
- **Request Body**:
  ```json
  {
    "name": "string",
    "subscriptionPlan": "BASIC|PREMIUM|ENTERPRISE"
  }
  ```
- **Response**: `201 Created`

#### Get Tenant
- **GET** `/api/tenants/{id}`
- **Description**: Get tenant details
- **Authentication**: Required
- **Response**: `200 OK`

#### Create Site
- **POST** `/api/tenants/{id}/sites`
- **Description**: Add a new site to tenant
- **Authentication**: Required (ADMIN/MANAGER role)
- **Request Body**:
  ```json
  {
    "name": "string",
    "address": "string",
    "timezone": "string"
  }
  ```
- **Response**: `201 Created`

### Dining Room Module

#### Get Table Map
- **GET** `/api/tables`
- **Description**: Get all tables with current status
- **Authentication**: Required
- **Query Parameters**:
  - `siteId` (required): UUID of the site
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "uuid",
      "tableNumber": "string",
      "status": "AVAILABLE|OCCUPIED|RESERVED|BLOCKED",
      "capacity": 4,
      "area": "string"
    }
  ]
  ```

#### Open Table
- **POST** `/api/tables/{id}/open`
- **Description**: Mark table as occupied
- **Authentication**: Required
- **Response**: `200 OK`

#### Close Table
- **POST** `/api/tables/{id}/close`
- **Description**: Mark table as available
- **Authentication**: Required
- **Response**: `200 OK`

#### Transfer Table
- **POST** `/api/tables/{id}/transfer`
- **Description**: Move orders to another table
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "toTableId": "uuid"
  }
  ```
- **Response**: `200 OK`

#### Block Table
- **POST** `/api/tables/{id}/block`
- **Description**: Add table to blacklist
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "reason": "string"
  }
  ```
- **Response**: `200 OK`

#### Unblock Table
- **DELETE** `/api/tables/{id}/block`
- **Description**: Remove table from blacklist
- **Authentication**: Required (MANAGER role)
- **Response**: `200 OK`

### Catalog Module

#### Get Menu Structure
- **GET** `/api/catalog/menu`
- **Description**: Get complete menu hierarchy
- **Authentication**: Required
- **Query Parameters**:
  - `siteId` (optional): UUID of the site
- **Response**: `200 OK`
  ```json
  {
    "families": [
      {
        "id": "uuid",
        "name": "string",
        "subfamilies": [
          {
            "id": "uuid",
            "name": "string",
            "items": [
              {
                "id": "uuid",
                "name": "string",
                "description": "string",
                "basePrice": 10.50,
                "available": true
              }
            ]
          }
        ]
      }
    ]
  }
  ```

#### Create Family
- **POST** `/api/catalog/families`
- **Description**: Add menu family
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "name": "string",
    "displayOrder": 0
  }
  ```
- **Response**: `201 Created`

#### Create Item
- **POST** `/api/catalog/items`
- **Description**: Add menu item
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "subfamilyId": "uuid",
    "name": "string",
    "description": "string",
    "basePrice": 10.50,
    "available": true
  }
  ```
- **Response**: `201 Created`

#### Update Item Availability
- **PUT** `/api/catalog/items/{id}/availability`
- **Description**: Toggle item availability
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "available": true
  }
  ```
- **Response**: `200 OK`

### Orders Module

#### Create Order
- **POST** `/api/orders`
- **Description**: Create new order for table
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "tableId": "uuid",
    "orderType": "DINE_IN|DELIVERY|TAKEOUT"
  }
  ```
- **Response**: `201 Created`

#### Get Order
- **GET** `/api/orders/{id}`
- **Description**: Get order details
- **Authentication**: Required
- **Response**: `200 OK`
  ```json
  {
    "id": "uuid",
    "tableId": "uuid",
    "orderType": "DINE_IN",
    "status": "OPEN|CONFIRMED|PAID|CLOSED",
    "totalAmount": 50.00,
    "lines": [
      {
        "id": "uuid",
        "itemId": "uuid",
        "itemName": "string",
        "quantity": 2,
        "unitPrice": 10.50,
        "status": "PENDING|CONFIRMED|VOIDED"
      }
    ]
  }
  ```

#### Add Order Line
- **POST** `/api/orders/{id}/lines`
- **Description**: Add item to order
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "itemId": "uuid",
    "quantity": 2,
    "modifiers": {},
    "notes": "string"
  }
  ```
- **Response**: `201 Created`

#### Update Order Line
- **PUT** `/api/orders/{id}/lines/{lineId}`
- **Description**: Modify order line
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "quantity": 3,
    "modifiers": {},
    "notes": "string"
  }
  ```
- **Response**: `200 OK`

#### Confirm Order
- **POST** `/api/orders/{id}/confirm`
- **Description**: Confirm order (Pedir) - creates consumptions and print jobs
- **Authentication**: Required
- **Response**: `200 OK`

#### Void Order Line
- **POST** `/api/orders/{id}/lines/{lineId}/void`
- **Description**: Cancel order line
- **Authentication**: Required (requires permission after confirmation)
- **Request Body**:
  ```json
  {
    "reason": "string",
    "recordWaste": false
  }
  ```
- **Response**: `200 OK`

#### Apply Discount
- **POST** `/api/orders/{id}/discounts`
- **Description**: Apply discount to order
- **Authentication**: Required (requires permission)
- **Request Body**:
  ```json
  {
    "type": "PERCENTAGE|FIXED_AMOUNT",
    "amount": 10.00,
    "reason": "string",
    "orderLineId": "uuid (optional)"
  }
  ```
- **Response**: `200 OK`

#### Get Orders for Table
- **GET** `/api/tables/{tableId}/orders`
- **Description**: Get all orders for a table
- **Authentication**: Required
- **Response**: `200 OK`

### Kitchen and Printing Module

#### List Printers
- **GET** `/api/printers`
- **Description**: Get all printers with status
- **Authentication**: Required
- **Query Parameters**:
  - `siteId` (required): UUID of the site
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "uuid",
      "name": "string",
      "ipAddress": "string",
      "zone": "string",
      "status": "NORMAL|WAIT|IGNORE|REDIRECT"
    }
  ]
  ```

#### Update Printer Status
- **PUT** `/api/printers/{id}/status`
- **Description**: Change printer state
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "status": "NORMAL|WAIT|IGNORE|REDIRECT"
  }
  ```
- **Response**: `200 OK`

#### Set Printer Redirect
- **POST** `/api/printers/{id}/redirect`
- **Description**: Redirect printer to another printer
- **Authentication**: Required (MANAGER role)
- **Request Body**:
  ```json
  {
    "targetPrinterId": "uuid"
  }
  ```
- **Response**: `200 OK`

#### Test Printer
- **POST** `/api/printers/{id}/test`
- **Description**: Send test print
- **Authentication**: Required (MANAGER role)
- **Response**: `200 OK`

#### Reprint Job
- **POST** `/api/print-jobs/{id}/reprint`
- **Description**: Manually reprint order
- **Authentication**: Required (requires permission)
- **Response**: `200 OK`

### Payments and Billing Module

#### Process Payment
- **POST** `/api/payments`
- **Description**: Process payment for order
- **Authentication**: Required
- **Headers**:
  - `Idempotency-Key`: UUID (required)
- **Request Body**:
  ```json
  {
    "orderId": "uuid",
    "amount": 50.00,
    "paymentMethod": "CASH|CARD|MOBILE|VOUCHER|MIXED"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "uuid",
    "orderId": "uuid",
    "amount": 50.00,
    "paymentMethod": "CASH",
    "status": "COMPLETED",
    "change": 0.00
  }
  ```

#### Void Payment
- **POST** `/api/payments/{id}/void`
- **Description**: Cancel payment
- **Authentication**: Required (requires permission)
- **Request Body**:
  ```json
  {
    "reason": "string"
  }
  ```
- **Response**: `200 OK`

#### Get Order Payments
- **GET** `/api/orders/{orderId}/payments`
- **Description**: Get all payments for order
- **Authentication**: Required
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "uuid",
      "amount": 50.00,
      "paymentMethod": "CASH",
      "status": "COMPLETED",
      "createdAt": "2024-01-01T12:00:00Z"
    }
  ]
  ```

#### Generate Fiscal Document
- **POST** `/api/billing/documents`
- **Description**: Create invoice or receipt
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "orderId": "uuid",
    "documentType": "INVOICE|RECEIPT",
    "customerNif": "string (required for INVOICE)"
  }
  ```
- **Response**: `201 Created`

#### Void Fiscal Document
- **POST** `/api/billing/documents/{id}/void`
- **Description**: Void document
- **Authentication**: Required (requires permission)
- **Request Body**:
  ```json
  {
    "reason": "string"
  }
  ```
- **Response**: `200 OK`

#### Print Subtotal
- **POST** `/api/billing/subtotal`
- **Description**: Print intermediate bill
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "orderId": "uuid"
  }
  ```
- **Response**: `200 OK`

#### Split Bill
- **POST** `/api/billing/split`
- **Description**: Divide order for split payment
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "orderId": "uuid",
    "splitCount": 2
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "splitAmounts": [25.00, 25.00]
  }
  ```

### Customers Module

#### Search Customers by Phone
- **GET** `/api/customers/search`
- **Description**: Search customers by phone number
- **Authentication**: Required
- **Query Parameters**:
  - `phone` (optional): Full phone number
  - `suffix` (optional): Last N digits
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "uuid",
      "name": "string",
      "phone": "string",
      "address": "string"
    }
  ]
  ```

#### Create Customer
- **POST** `/api/customers`
- **Description**: Create new customer
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "name": "string",
    "phone": "string",
    "address": "string",
    "deliveryNotes": "string"
  }
  ```
- **Response**: `201 Created`

#### Update Customer
- **PUT** `/api/customers/{id}`
- **Description**: Modify customer details
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "name": "string",
    "phone": "string",
    "address": "string",
    "deliveryNotes": "string"
  }
  ```
- **Response**: `200 OK`

#### Get Customer Order History
- **GET** `/api/customers/{id}/orders`
- **Description**: Get customer's previous orders
- **Authentication**: Required
- **Response**: `200 OK`

### Cash Register Module

#### Open Cash Session
- **POST** `/api/cash/sessions`
- **Description**: Start cash session
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "registerId": "uuid",
    "openingAmount": 100.00
  }
  ```
- **Response**: `201 Created`

#### Close Cash Session
- **POST** `/api/cash/sessions/{id}/close`
- **Description**: Close session and calculate variance
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "actualAmount": 500.00
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "expectedClose": 500.00,
    "actualClose": 500.00,
    "variance": 0.00
  }
  ```

#### Record Cash Movement
- **POST** `/api/cash/sessions/{id}/movements`
- **Description**: Record manual cash movement
- **Authentication**: Required
- **Request Body**:
  ```json
  {
    "movementType": "DEPOSIT|WITHDRAWAL",
    "amount": 50.00,
    "reason": "string"
  }
  ```
- **Response**: `201 Created`

#### Get Session Summary
- **GET** `/api/cash/sessions/{id}`
- **Description**: Get session details with movements
- **Authentication**: Required
- **Response**: `200 OK`

#### Close Register
- **POST** `/api/cash/closings/register`
- **Description**: Close register for period
- **Authentication**: Required (requires permission)
- **Request Body**:
  ```json
  {
    "registerId": "uuid",
    "periodStart": "2024-01-01T00:00:00Z",
    "periodEnd": "2024-01-01T23:59:59Z"
  }
  ```
- **Response**: `201 Created`

#### Close Day
- **POST** `/api/cash/closings/day`
- **Description**: Close day for site
- **Authentication**: Required (requires permission)
- **Request Body**:
  ```json
  {
    "siteId": "uuid",
    "date": "2024-01-01"
  }
  ```
- **Response**: `201 Created`

#### Get Closing Report
- **GET** `/api/cash/closings/{id}/report`
- **Description**: Get closing report
- **Authentication**: Required
- **Response**: `200 OK`

#### Reprint Closing Report
- **POST** `/api/cash/closings/{id}/reprint`
- **Description**: Reprint previous report
- **Authentication**: Required (requires permission)
- **Response**: `200 OK`

### SAF-T Export Module (Optional)

#### Generate SAF-T Export
- **POST** `/api/exports/saft-pt`
- **Description**: Generate SAF-T PT XML export
- **Authentication**: Required (ADMIN/MANAGER role)
- **Request Body**:
  ```json
  {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  }
  ```
- **Response**: `200 OK` (XML file download)

## Error Responses

All error responses follow RFC 7807 Problem Details format:

```json
{
  "type": "https://api.restaurantpos.com/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Invalid request parameters",
  "instance": "/api/orders",
  "traceId": "uuid",
  "errors": [
    {
      "field": "quantity",
      "message": "must be greater than 0"
    }
  ]
}
```

### HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Concurrent modification conflict
- `422 Unprocessable Entity` - Business rule violation
- `500 Internal Server Error` - Server error

## Rate Limiting

Authentication endpoints are rate limited to 5 requests per minute per IP/username.

## Pagination

List endpoints support pagination with query parameters:
- `page` - Page number (0-indexed)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

## Interactive Documentation

For interactive API testing, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
