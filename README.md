# Utility Billing System (UBS) — Monolith

Secure backend platform for WASAC & REG utility billing: customers, meters, readings, billing, payments, notifications, and audit logs.

## Architecture

Single **Spring Boot 3.5** monolith (`port 8080`) with modular packages:

| Package | Responsibility |
|---------|----------------|
| `auth` | Registration, login, JWT, refresh tokens, OTP, password reset |
| `customer` | Customers, meters, file uploads |
| `meter` | Meter readings |
| `billing` | Tariffs, bills, stored procedures |
| `payment` | Partial/full payments |
| `notification` | In-app & email notifications |
| `audit` | Audit logs |
| `common` | JWT, validation, exceptions, OpenAPI |

**Database:** PostgreSQL (schema-per-domain: `auth`, `customer`, `meter`, `billing`, `payment`, `notification`, `audit`)

## Prerequisites

- Java 21
- Docker (PostgreSQL)
- Maven Wrapper (`./mvnw`)

## Quick Start

```bash
# 1. Environment
cp .env.example .env
# Edit MAIL_USERNAME, MAIL_PASSWORD, JWT_SECRET

# 2. PostgreSQL
docker compose up -d

# 3. Build & run
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

## Swagger / OpenAPI

- **UI:** http://localhost:8080/swagger-ui.html
- Use the **dropdown** to switch role groups: All, PUBLIC, ROLE_ADMIN, ROLE_OPERATOR, ROLE_FINANCE, ROLE_CUSTOMER, Authenticated
- Each endpoint shows **Required roles** in its description
- DTO request/response schemas are visible in the Swagger UI **Schemas** section and referenced by each endpoint body/response.

## Testing

See **[TESTING_CHECKLIST.md](TESTING_CHECKLIST.md)** for a full role-based verification checklist with endpoints.

### Postman

Import **`postman/UBS-Full-Test-Collection.postman_collection.json`** and **`postman/UBS-Local.postman_environment.json`**.

Includes ✅ valid and ❌ invalid payloads for every validation rule. See **[postman/README.md](postman/README.md)**.

**Seed accounts**:

| Role | Email | Password |
|------|-------|----------|
| ADMIN | admin@wasac.rw | `Admin@123` |
| OPERATOR | operator@wasac.rw | `Password@123` |
| FINANCE | finance@wasac.rw | `Password@123` |

## Default Admin

- Email: `admin@wasac.rw`
- Password: `Admin@123`

## Roles

| Role | Capabilities |
|------|-------------|
| ROLE_ADMIN | Users, tariffs, customers, meters, audit |
| ROLE_OPERATOR | Capture meter readings, view customers/meters |
| ROLE_FINANCE | Approve bills, record payments |
| ROLE_CUSTOMER | View bills, payments, notifications |

## API Examples

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/verify-otp
POST /api/auth/logout
GET  /api/customers?page=0&size=10&sort=fullName
GET  /api/customers/search?keyword=Pascal
POST /api/readings
POST /api/bills
POST /api/bills/generate-monthly
POST /api/payments
POST /api/files/upload
GET  /api/audit?page=0&size=20
```

## Monthly Billing Cycle

```text
ADMIN registers customer → assigns meter(s)
OPERATOR captures monthly reading → queued for billing
SYSTEM (scheduler or POST /api/bills/generate-monthly) → generates bills
FINANCE approves bill → customer can view
FINANCE records payment → PARTIALLY_PAID or PAID + notification
```

**Bill formula:** `(Consumption × Tariff) + ServiceCharge + VAT`

**Statuses:** `PENDING` → `APPROVED` → `PARTIALLY_PAID` / `PAID`

Customers only see `APPROVED`, `PARTIALLY_PAID`, `PAID`, and `OVERDUE` bills.

## Database Routines

- `billing.generate_monthly_bills()` — batch bill generation from pending readings (tariff by effective date)
- `trg_bill_notification` — personalized notification on bill insert
- `trg_payment_completed` — balance update, `PARTIALLY_PAID`/`PAID`, payment notification

# ubs
