# UBS Role-Based Testing Checklist

Use this checklist to verify the implementation. Each item maps to a **Swagger UI** operation you can **Try it out**.

| Resource | URL |
|----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Role catalog (JSON) | `GET /api/docs/role-catalog` |
| Postman collection | `postman/UBS-Full-Test-Collection.postman_collection.json` |

**Test accounts**:

| Role | Email | Password | Swagger group |
|------|-------|----------|---------------|
| ADMIN | `admin@wasac.rw` | `Admin@123` | [ROLE_ADMIN](http://localhost:8080/swagger-ui/index.html?group=admin) |
| OPERATOR | `operator@wasac.rw` | `Password@123` | [ROLE_OPERATOR](http://localhost:8080/swagger-ui/index.html?group=operator) |
| FINANCE | `finance@wasac.rw` | `Password@123` | [ROLE_FINANCE](http://localhost:8080/swagger-ui/index.html?group=finance) |
| CUSTOMER | Self-register + OTP | Chosen during signup | [ROLE_CUSTOMER](http://localhost:8080/swagger-ui/index.html?group=customer) |
| Public (no JWT) | - | - | [PUBLIC](http://localhost:8080/swagger-ui/index.html?group=public) |

**How to test in Swagger**

1. Open the **role group** link above.
2. For secured groups: open **PUBLIC** → **Login** → copy `data.accessToken` → **Authorize** → `Bearer <token>`.
3. Expand the tag, click the operation, **Try it out**, **Execute**.
4. Check the response code and body against the expected result in each section.

---

# ROLE_ADMIN Checklist

**Swagger group:** http://localhost:8080/swagger-ui/index.html?group=admin  
**Login:** `POST /api/auth/login` → `admin@wasac.rw` / `Admin@123`

## User Management

* [ ] **Create new user** — `POST /api/users` (Users → Create user)
* [ ] **View all users** — `GET /api/users` (Users → List users)
* [ ] **Update user details** — `PUT /api/users/{id}` (Users → Update user details)
* [ ] **Activate user** — `PUT /api/users/{id}/activate` (Users → Activate user)
* [ ] **Deactivate user** — `PUT /api/users/{id}/deactivate` (Users → Deactivate user)
* [ ] **Assign roles** — `PUT /api/users/{id}/roles` (Users → Assign roles)
* [ ] **Delete user** — `DELETE /api/users/{id}` (Users → Delete user)

## Customer Management

* [ ] **Register customer** — `POST /api/customers` (Customers → Create Customer)
* [ ] **View customers** — `GET /api/customers` (Customers → List Customers)
* [ ] **Update customer details** — `PUT /api/customers/{id}` (Customers → Update Customer)
* [ ] **Activate customer** — `PUT /api/customers/{id}` with `"status": "ACTIVE"`
* [ ] **Deactivate customer** — `PUT /api/customers/{id}` with `"status": "INACTIVE"`
* [ ] **Prevent duplicate National ID** — `POST /api/customers` with same `nationalId` → expect **400**

## Meter Management

* [ ] **Create meter** — `POST /api/meters` (Meters → Create Meter)
* [ ] **Assign meter to customer** — `POST /api/meters` with `customerId` in body
* [ ] **View all meters** — `GET /api/meters` (Meters → List Meters)
* [ ] **Update meter details** — `PUT /api/meters/{id}` (Meters → Update Meter)
* [ ] **Activate meter** — `PUT /api/meters/{id}` with `"status": "ACTIVE"`
* [ ] **Deactivate meter** — `PUT /api/meters/{id}` with `"status": "INACTIVE"`
* [ ] **Prevent duplicate meter number** — `POST /api/meters` with same `meterNumber` → expect **400**

## Tariff Management

* [ ] **Create tariff** — `POST /api/tariffs` (Tariffs → Configure Tariff)
* [ ] **Create tariff version** — `POST /api/tariffs` with new `version` number
* [ ] **Configure VAT** — `POST /api/tariffs` → set `vat` field
* [ ] **Configure service charge** — `POST /api/tariffs` → set `fixedCharge` field
* [ ] **Configure late payment penalty** — `POST /api/tariffs` → set `penaltyRate` field
* [ ] **Set effective date** — `POST /api/tariffs` → set `effectiveDate` field
* [ ] **Verify old bills remain unchanged** — generate bill before tariff change; confirm amount unchanged on `GET /api/bills/{id}`

## Billing Management

* [ ] **View generated bills** — `GET /api/bills` (Bills → List bills)
* [ ] **View bill details** — `GET /api/bills/{id}` (Bills → Get bill by ID)
* [ ] **Generate monthly bills** — `POST /api/bills/generate-monthly` (Bills → Generate monthly bills)
* [ ] **Approve bills** — `PUT /api/bills/{id}/approve` (Bills → Approve bill)
* [ ] **Reject bills** — `PUT /api/bills/{id}/reject` (Bills → Reject bill)
* [ ] **View pending bills** — `GET /api/bills/pending` (Bills → List pending bills)

## Reports

* [ ] **View customer report** — `GET /api/reports/customers` (Reports → Customer report)
* [ ] **View billing report** — `GET /api/reports/billing` (Reports → Billing report)
* [ ] **View payment report** — `GET /api/reports/payments` (Reports → Payment report)
* [ ] **View revenue report** — `GET /api/reports/revenue` (Reports → Revenue report)

## Security

* [ ] **Login successfully** — PUBLIC group → `POST /api/auth/login`
* [ ] **Access Admin endpoints** — Authorize with admin JWT; any ADMIN group operation → **200**
* [ ] **Cannot access without JWT** — call `GET /api/users` without Authorize → **403**
* [ ] **Logout** — `POST /api/auth/logout` (Authentication → Logout)

---

# ROLE_OPERATOR Checklist

**Swagger group:** http://localhost:8080/swagger-ui/index.html?group=operator  
**Login:** `POST /api/auth/login` → `operator@wasac.rw` / `Password@123`

## Meter Reading Management

* [ ] **View assigned meters** — `GET /api/meters` (Meters → List Meters) — *all active meters; no per-operator assignment filter*
* [ ] **Capture reading** — `POST /api/readings` (Meter Readings → Capture Reading)
* [ ] **View readings** — `GET /api/readings` (Meter Readings → List all readings)
* [ ] **Update reading** — *not implemented* (readings are immutable for billing integrity)
* [ ] **Search readings by meter** — `GET /api/readings/search?meterId={uuid}` (Meter Readings → Search readings by meter)

## Business Rule Validation

Test via `POST /api/readings` in OPERATOR group:

* [ ] **Reject current reading less than previous reading** — `currentReading < previousReading` → **400**
* [ ] **Reject equal reading** — `currentReading = previousReading` → **400**
* [ ] **Reject reading for inactive meter** — meter `status: INACTIVE` → **400**
* [ ] **Reject duplicate reading for same month/year** — same `meterId` + `month` + `year` → **400**
* [ ] **Accept valid reading** — valid body → **201**

## Security

* [ ] **Login successfully** — PUBLIC → `POST /api/auth/login`
* [ ] **Access Operator endpoints** — `POST /api/readings` with operator JWT → **201**
* [ ] **Cannot access Admin endpoints** — `POST /api/users` with operator JWT → **403**
* [ ] **Cannot access Finance endpoints** — `POST /api/payments` with operator JWT → **403**

---

# ROLE_FINANCE Checklist

**Swagger group:** http://localhost:8080/swagger-ui/index.html?group=finance  
**Login:** `POST /api/auth/login` → `finance@wasac.rw` / `Password@123`

## Bill Approval

* [ ] **View pending bills** — `GET /api/bills/pending` (Bills → List pending bills)
* [ ] **Approve bill** — `PUT /api/bills/{id}/approve` (Bills → Approve bill)
* [ ] **Reject bill** — `PUT /api/bills/{id}/reject` (Bills → Reject bill)
* [ ] **View approved bills** — `GET /api/bills` (Bills → List bills) — filter `status: APPROVED`

## Payment Processing

* [ ] **Record payment** — `POST /api/payments` (Payments → Record Payment)
* [ ] **Record partial payment** — `POST /api/payments` with `amountPaid` < bill balance
* [ ] **Record full payment** — `POST /api/payments` with `amountPaid` = bill balance
* [ ] **Update outstanding balance** — verify `remainingBalance` in payment response + `GET /api/bills/{id}`
* [ ] **View payment history** — `GET /api/payments` (Payments → List Payments)

## Business Rule Validation

* [ ] **Partial payment updates balance correctly** — bill status → `PARTIALLY_PAID`, balance reduced
* [ ] **Full payment marks bill as PAID** — bill status → `PAID`, balance → `0`
* [ ] **Overpayment prevented** — `amountPaid` > balance → **400**
* [ ] **Notification generated after payment** — `GET /api/notifications` (admin) or customer notifications after full payment

## Reports

* [ ] **View revenue reports** — `GET /api/reports/revenue` (Reports → Revenue report)
* [ ] **View outstanding balances** — `GET /api/reports/outstanding-balances` (Reports → Outstanding balances)
* [ ] **View payment reports** — `GET /api/reports/payments` (Reports → Payment report)

## Security

* [ ] **Login successfully** — PUBLIC → `POST /api/auth/login`
* [ ] **Access Finance endpoints** — `POST /api/payments` with finance JWT → **201**
* [ ] **Cannot access Admin endpoints** — `POST /api/users` with finance JWT → **403**
* [ ] **Cannot access Operator endpoints** — `POST /api/readings` with finance JWT → **403**

---

# ROLE_CUSTOMER Checklist

**Swagger group:** http://localhost:8080/swagger-ui/index.html?group=customer

**Setup:** PUBLIC → `POST /api/auth/signup` → `POST /api/auth/verify-otp` → `POST /api/auth/login` → Authorize

## Profile

* [ ] **View profile** — `GET /api/profile` (Profile → View profile)
* [ ] **Update profile** — `PUT /api/profile` (Profile → Update profile)

## Bills

* [ ] **View current bills** — `GET /api/bills` (Bills → List bills) — *APPROVED / PARTIALLY_PAID / PAID only*
* [ ] **View bill details** — `GET /api/bills/{id}` (Bills → Get bill by ID)
* [ ] **View bill status** — check `status` field in bill response
* [ ] **View outstanding balance** — check `balance` field in bill response
* [ ] **Download bill** — `GET /api/bills/{id}` returns JSON; file upload/download via `GET /api/files/{id}` if bill PDF attached

## Payments

* [ ] **View payment history** — `GET /api/payments/customer/{customerId}` (Payments → View Payment History)
* [ ] **View payment status** — check payment response fields on history endpoint
* [ ] **View remaining balance** — `GET /api/bills/{id}` → `balance` field

## Notifications

* [ ] **View bill notifications** — `GET /api/notifications/customer/{customerId}` (Notifications → View Notifications)
* [ ] **View payment notifications** — same endpoint; filter by `type` in response
* [ ] **Mark notification read** — `PUT /api/notifications/{id}/read` (Notifications → Mark notification as read)

## Security

* [ ] **Login successfully** — PUBLIC → `POST /api/auth/login`
* [ ] **Access Customer endpoints** — `GET /api/profile` with customer JWT → **200**
* [ ] **Cannot access Admin endpoints** — `POST /api/users` → **403**
* [ ] **Cannot access Operator endpoints** — `POST /api/readings` → **403**
* [ ] **Cannot access Finance endpoints** — `POST /api/payments` → **403**

---

# System Automated Checks

These are verified by running the E2E flow in Swagger or Postman (Operator capture → Finance approve → Finance pay).

## Bill Generation

* [ ] **Bills generated only for active customers** — inactive customer → reading rejected at `POST /api/readings`
* [ ] **Bills generated only for active meters** — inactive meter → reading rejected at `POST /api/readings`
* [ ] **Correct tariff version applied** — `POST /api/bills` uses tariff matching `meterType` + `effectiveDate`
* [ ] **VAT applied correctly** — bill `vatAmount` = consumption × rate × VAT%
* [ ] **Service charge applied correctly** — bill includes `fixedCharge` from tariff
* [ ] **Bill status set to PENDING** — new bill `status: PENDING` on `GET /api/bills/{id}`
* [ ] **Notification created automatically** — after bill generation (DB trigger) → `GET /api/notifications`

## Payment Automation

* [ ] **Outstanding balance updated** — after `POST /api/payments` → `GET /api/bills/{id}` shows new balance
* [ ] **Status becomes PARTIALLY_PAID** — partial payment → bill status `PARTIALLY_PAID`
* [ ] **Status becomes PAID when balance reaches zero** — full payment → bill status `PAID`
* [ ] **Notification created automatically** — after full payment (DB trigger)

## JWT Security

* [ ] **Invalid token rejected** — Authorize with `Bearer invalid` → protected endpoint → **403**
* [ ] **Expired token rejected** — use expired JWT → **403**
* [ ] **Missing token rejected** — no Authorize → protected endpoint → **403**
* [ ] **Correct role access granted** — each role's Swagger group operations → **2xx**
* [ ] **Unauthorized role access denied** — cross-role calls listed in Security sections → **403**

---

# Expected APIs (Exam Reference)

Map each required API to its Swagger tag and role group.

### Authentication

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `POST /api/auth/signup` | Authentication → Sign up (alias) | PUBLIC |
| [ ] `POST /api/auth/login` | Authentication → Login | PUBLIC |

### Users (ROLE_ADMIN only)

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/users` | Users → List users | ROLE_ADMIN |
| [ ] `POST /api/users` | Users → Create user | ROLE_ADMIN |
| [ ] `PUT /api/users/{id}` | Users → Update user details | ROLE_ADMIN |
| [ ] `DELETE /api/users/{id}` | Users → Delete user | ROLE_ADMIN |

### Customers

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/customers` | Customers → List Customers | ROLE_ADMIN |
| [ ] `POST /api/customers` | Customers → Create Customer | ROLE_ADMIN |
| [ ] `PUT /api/customers/{id}` | Customers → Update Customer | ROLE_ADMIN |

### Meters

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/meters` | Meters → List Meters | ROLE_ADMIN / OPERATOR / FINANCE |
| [ ] `POST /api/meters` | Meters → Create Meter | ROLE_ADMIN |
| [ ] `PUT /api/meters/{id}` | Meters → Update Meter | ROLE_ADMIN |

### Readings

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/readings` | Meter Readings → List all readings | ROLE_OPERATOR |
| [ ] `POST /api/readings` | Meter Readings → Capture Reading | ROLE_OPERATOR |

### Tariffs

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/tariffs` | Tariffs → List tariffs | ROLE_ADMIN |
| [ ] `POST /api/tariffs` | Tariffs → Configure Tariff | ROLE_ADMIN |

### Bills

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/bills` | Bills → List bills | ROLE_ADMIN / FINANCE / CUSTOMER |
| [ ] `POST /api/bills/generate` | Bills → Generate bill (alias) | ROLE_ADMIN / FINANCE |
| [ ] `PUT /api/bills/{id}/approve` | Bills → Approve bill | ROLE_ADMIN / FINANCE |

### Payments

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/payments` | Payments → List Payments | ROLE_FINANCE / CUSTOMER |
| [ ] `POST /api/payments` | Payments → Record Payment | ROLE_FINANCE |

### Notifications

| API | Swagger | Group |
|-----|---------|-------|
| [ ] `GET /api/notifications` | Notifications → List all notifications | ROLE_ADMIN |
| [ ] `GET /api/notifications/customer/{customerId}` | Notifications → View Notifications | ROLE_CUSTOMER |

---

If every item in this checklist passes, the Spring Boot backend satisfies the exam requirements.
