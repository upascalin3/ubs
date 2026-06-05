#!/usr/bin/env python3
"""Generate UBS Postman collection — SRS unified identity, valid + invalid tests."""
import json
from copy import deepcopy

# --- shared scripts ---
SAVE_TOKEN = """
function saveToken(role, json) {
    if (!json?.data?.accessToken) return;
    pm.collectionVariables.set('accessToken' + role, json.data.accessToken);
    if (json.data.refreshToken) pm.collectionVariables.set('refreshToken' + role, json.data.refreshToken);
    if (json.data.userId) {
        pm.collectionVariables.set(role.toLowerCase() + 'UserId', json.data.userId);
        if (role === 'Customer') pm.collectionVariables.set('customerUserId', json.data.userId);
    }
}
""".strip().split("\n")

def save_login(role):
    return SAVE_TOKEN + [
        f"if (pm.response.code === 200) saveToken('{role}', pm.response.json());",
        "pm.test('Login OK - if this is 401, restart the app so DataInitializer repairs seed users', () => pm.expect(pm.response.code).to.equal(200));",
    ]

SAVE_ID = """
function saveId(key) {
    if (pm.response.code === 200 || pm.response.code === 201) {
        const j = pm.response.json();
        const id = j.data?.id || j.data?.content?.[0]?.id;
        if (id) pm.collectionVariables.set(key, id);
    }
}
function saveNotificationId() {
    if (pm.response.code === 200) {
        const c = pm.response.json().data?.content;
        if (c?.[0]?.id) pm.collectionVariables.set('notificationId', c[0].id);
    }
}
""".strip().split("\n")

def save_id(key):
    return SAVE_ID + [f"saveId('{key}');", "pm.test('Success', () => pm.expect(pm.response.code).to.be.oneOf([200, 201]));"]

UNIQUE_SIGNUP = [
    "const ts = Date.now();",
    "pm.collectionVariables.set('uniqueEmail', 'cust' + ts + '@test.wasac.rw');",
    "pm.collectionVariables.set('uniquePhone', '07' + String(ts).slice(-8));",
    "pm.collectionVariables.set('uniqueNationalId', String(1190000000000 + (ts % 999999)));",
]
UNIQUE_STAFF = [
    "const ts = Date.now();",
    "pm.collectionVariables.set('uniqueStaffEmail', 'staff' + ts + '@test.wasac.rw');",
    "pm.collectionVariables.set('uniqueStaffPhone', '07' + String(ts + 1).slice(-8));",
]
UNIQUE_METER = ["pm.collectionVariables.set('uniqueMeterNumber', 'MTR-' + Date.now());"]
UNIQUE_TARIFF = ["pm.collectionVariables.set('uniqueTariffVersion', String(Math.floor(Date.now()/1000) % 100000 + 1));"]
UNIQUE_PAY = ["pm.collectionVariables.set('uniquePaymentRef', 'PAY-' + Date.now());"]
DATE_VARS = [
    "const now = new Date();",
    "const yyyy = now.getFullYear();",
    "const mm = String(now.getMonth() + 1).padStart(2, '0');",
    "const dd = String(now.getDate()).padStart(2, '0');",
    "pm.collectionVariables.set('todayDate', `${yyyy}-${mm}-${dd}`);",
    "const next = new Date(yyyy, now.getMonth() + 1, 1);",
    "pm.collectionVariables.set('billingMonth', String(next.getMonth() + 1));",
    "pm.collectionVariables.set('billingYear', String(next.getFullYear()));",
]

EXPECT_OK = ["pm.test('Success', () => pm.expect(pm.response.code).to.be.oneOf([200, 201]));"]
EXPECT_400 = ["pm.test('Validation fail', () => pm.expect(pm.response.code).to.be.oneOf([400, 422]));"]
EXPECT_403 = ["pm.test('Forbidden', () => pm.expect(pm.response.code).to.equal(403));"]
EXPECT_401_403 = ["pm.test('Unauthorized', () => pm.expect(pm.response.code).to.be.oneOf([401, 403]));"]


def req(name, method, path, body=None, bearer=None, desc="", tests=None, query=None, prerequest=None, formdata=None):
    r = {"name": name, "request": {"method": method, "header": [], "url": "{{baseUrl}}" + path, "description": desc}}
    if body is not None:
        r["request"]["header"].append({"key": "Content-Type", "value": "application/json"})
        r["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2)}
    if formdata is not None:
        r["request"]["body"] = {"mode": "formdata", "formdata": formdata}
    if query:
        r["request"]["url"] = {
            "raw": "{{baseUrl}}" + path + "?" + "&".join(f"{k}={v}" for k, v in query),
            "host": ["{{baseUrl}}"],
            "path": [p for p in path.strip("/").split("/") if p],
            "query": [{"key": k, "value": v} for k, v in query],
        }
    if bearer:
        r["request"]["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": bearer, "type": "string"}]}
    events = []
    if prerequest:
        events.append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": prerequest}})
    if tests:
        events.append({"listen": "test", "script": {"type": "text/javascript", "exec": tests}})
    if events:
        r["event"] = events
    return r


def folder(name, items, desc="", bearer=None, prerequest=None):
    f = {"name": name, "item": items}
    if desc:
        f["description"] = desc
    if bearer:
        f["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": bearer, "type": "string"}]}
    if prerequest:
        f["event"] = [{"listen": "prerequest", "script": {"type": "text/javascript", "exec": prerequest}}]
    return f


def val_pair(name, method, path, valid, invalid_map, bearer=None, prerequest=None):
    items = [req(f"VALID — {name}", method, path, valid, bearer, "Expect 200/201", EXPECT_OK, prerequest=prerequest)]
    for key, body in invalid_map.items():
        items.append(req(f"INVALID — {name} ({key})", method, path, body, bearer, f"Expect 400: {key}", EXPECT_400, prerequest=prerequest))
    return folder(f"Validate — {name}", items, bearer=bearer)


# --- payloads (SRS: userId, no customerId on payment) ---
ADMIN_PASSWORD = "Admin@123"
DEFAULT_PASSWORD = "Password@123"

SIGNUP = {
    "fullName": "Jean Pascal",
    "email": "{{uniqueEmail}}",
    "password": DEFAULT_PASSWORD,
    "phoneNumber": "{{uniquePhone}}",
    "nationalId": "{{uniqueNationalId}}",
    "address": "Kigali, Gasabo",
}

METER = {
    "userId": "{{customerUserId}}",
    "meterNumber": "{{uniqueMeterNumber}}",
    "meterType": "WATER",
    "installationDate": "2025-06-01",
    "status": "ACTIVE",
}

TARIFF = {
    "meterType": "WATER",
    "tariffName": "Residential Water",
    "rate": 450,
    "fixedCharge": 1500,
    "vat": 18,
    "penaltyRate": 5,
    "version": "{{uniqueTariffVersion}}",
    "effectiveDate": "{{todayDate}}",
    "active": True,
}

READING = {
    "meterId": "{{meterId}}",
    "previousReading": 100,
    "currentReading": 145,
    "readingDate": "{{todayDate}}",
    "month": "{{billingMonth}}",
    "year": "{{billingYear}}",
}

BILL = {
    "userId": "{{customerUserId}}",
    "meterId": "{{meterId}}",
    "meterType": "WATER",
    "consumption": 45,
    "billingMonth": "{{billingMonth}}",
    "billingYear": "{{billingYear}}",
}

PAYMENT_PARTIAL = {
    "billId": "{{billId}}",
    "amountPaid": 5000,
    "paymentMethod": "BANK_TRANSFER",
    "paymentDate": "{{todayDate}}",
    "referenceNumber": "{{uniquePaymentRef}}",
}

CUSTOMER_USER = {
    "fullName": "Postman Customer",
    "email": "{{uniqueEmail}}",
    "password": DEFAULT_PASSWORD,
    "phoneNumber": "{{uniquePhone}}",
    "roles": ["ROLE_CUSTOMER"],
}

STAFF_USER = {
    "fullName": "New Operator",
    "email": "{{uniqueStaffEmail}}",
    "password": DEFAULT_PASSWORD,
    "phoneNumber": "{{uniqueStaffPhone}}",
    "roles": ["ROLE_OPERATOR"],
}

INVALID_SIGNUP = {
    "name_numbers": {**SIGNUP, "fullName": "Jean123"},
    "weak_password": {**SIGNUP, "password": "password"},
    "bad_phone": {**SIGNUP, "phoneNumber": "12345"},
    "missing_national_id": {k: v for k, v in SIGNUP.items() if k != "nationalId"},
    "empty_body": {},
}

INVALID_METER = {
    "missing_user": {"meterNumber": "MTR-BAD", "meterType": "WATER", "installationDate": "2025-01-01", "status": "ACTIVE"},
    "future_date": {**METER, "installationDate": "2099-12-31"},
    "bad_type": {**METER, "meterType": "GAS"},
}

INVALID_READING = {
    "less_than_previous": {**READING, "previousReading": 200, "currentReading": 150},
    "equal": {**READING, "previousReading": 145, "currentReading": 145},
    "future_date": {**READING, "readingDate": "2099-01-01"},
    "bad_month": {**READING, "month": 13},
}

INVALID_TARIFF = {
    "negative_rate": {**TARIFF, "rate": -10},
    "past_effective_date": {**TARIFF, "effectiveDate": "2000-01-01"},
    "missing_name": {"meterType": "WATER", "rate": 100, "fixedCharge": 0, "vat": 18, "penaltyRate": 5, "version": 1, "effectiveDate": "{{todayDate}}"},
}

INVALID_PAYMENT = {
    "zero_amount": {**PAYMENT_PARTIAL, "amountPaid": 0},
    "overpay": {**PAYMENT_PARTIAL, "amountPaid": 999999999},
    "future_date": {**PAYMENT_PARTIAL, "paymentDate": "2099-01-01"},
    "missing_bill": {"amountPaid": 100, "paymentMethod": "CASH", "paymentDate": "{{todayDate}}", "referenceNumber": "PAY-NOBILL"},
}

INVALID_STAFF = {
    "two_roles": {**STAFF_USER, "roles": ["ROLE_OPERATOR", "ROLE_FINANCE"]},
    "no_roles": {k: v for k, v in STAFF_USER.items() if k != "roles"},
}

collection = {
    "info": {
        "_postman_id": "ubs-srs-full-suite-2026",
        "name": "UBS — Full Integrity Test Suite (SRS)",
        "description": (
            "Import **UBS-Local.postman_environment.json** (only baseUrl).\n\n"
            "**Run order:** 00 Setup → 01 E2E → role folders → 99 Validation → 98 Security\n\n"
            "**OTP:** After signup, copy OTP from app console → set collection var `otpCode` → run Verify OTP.\n\n"
            "**Seed passwords:** admin uses Admin@123; operator/finance use Password@123\n\n"
            "SRS: User = Customer. Use `userId` / `customerUserId`, not customerId."
        ),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080"},
        {"key": "accessTokenAdmin", "value": ""},
        {"key": "accessTokenOperator", "value": ""},
        {"key": "accessTokenFinance", "value": ""},
        {"key": "accessTokenCustomer", "value": ""},
        {"key": "refreshTokenAdmin", "value": ""},
        {"key": "adminUserId", "value": ""},
        {"key": "operatorUserId", "value": ""},
        {"key": "financeUserId", "value": ""},
        {"key": "customerUserId", "value": ""},
        {"key": "userId", "value": ""},
        {"key": "meterId", "value": ""},
        {"key": "billId", "value": ""},
        {"key": "tariffId", "value": ""},
        {"key": "notificationId", "value": ""},
        {"key": "otpCode", "value": "123456"},
        {"key": "uniqueEmail", "value": ""},
        {"key": "uniquePhone", "value": ""},
        {"key": "uniqueNationalId", "value": ""},
        {"key": "uniqueMeterNumber", "value": ""},
        {"key": "uniqueTariffVersion", "value": "1"},
        {"key": "uniquePaymentRef", "value": ""},
        {"key": "uniqueStaffEmail", "value": ""},
        {"key": "uniqueStaffPhone", "value": ""},
        {"key": "todayDate", "value": ""},
        {"key": "billingMonth", "value": ""},
        {"key": "billingYear", "value": ""},
        {"key": "fileId", "value": ""},
    ],
    "event": [
        {"listen": "prerequest", "script": {"type": "text/javascript", "exec": DATE_VARS}}
    ],
    "item": [],
}

# 00 Setup
setup = folder("00 — Setup (RUN FIRST)", [
    req("Swagger redirect root", "GET", "/", tests=["pm.test('Redirect or OK', () => pm.expect(pm.response.code).to.be.oneOf([200, 302]));"]),
    req("Swagger redirect docs", "GET", "/docs", tests=["pm.test('Redirect or OK', () => pm.expect(pm.response.code).to.be.oneOf([200, 302]));"]),
    req("Login ADMIN", "POST", "/api/auth/login", {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD}, tests=save_login("Admin")),
    req("Login OPERATOR", "POST", "/api/auth/login", {"email": "operator@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Operator")),
    req("Login FINANCE", "POST", "/api/auth/login", {"email": "finance@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Finance")),
    req("Role Catalog", "GET", "/api/docs/role-catalog", tests=["pm.test('OK', () => pm.expect(pm.response.code).to.equal(200));"]),
], "Saves JWT tokens to collection variables.")

# 01 E2E
e2e = folder("01 — E2E Happy Path (full billing cycle)", [
    req("1 Admin login", "POST", "/api/auth/login",
        {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD}, tests=save_login("Admin")),
    req("2 Create active customer user", "POST", "/api/users", CUSTOMER_USER, "{{accessTokenAdmin}}",
        prerequest=UNIQUE_SIGNUP, tests=save_id("customerUserId")),
    req("3 Customer login", "POST", "/api/auth/login",
        {"email": "{{uniqueEmail}}", "password": DEFAULT_PASSWORD}, tests=save_login("Customer")),
    req("4 Create tariff", "POST", "/api/tariffs", TARIFF, "{{accessTokenAdmin}}", prerequest=UNIQUE_TARIFF, tests=save_id("tariffId")),
    req("5 Assign meter", "POST", "/api/meters", METER, "{{accessTokenAdmin}}", prerequest=UNIQUE_METER, tests=save_id("meterId")),
    req("6 Operator login", "POST", "/api/auth/login",
        {"email": "operator@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Operator")),
    req("7 Capture reading", "POST", "/api/readings", READING, "{{accessTokenOperator}}", tests=EXPECT_OK),
    req("8 Finance login", "POST", "/api/auth/login",
        {"email": "finance@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Finance")),
    req("9 Generate bill directly", "POST", "/api/bills", BILL, bearer="{{accessTokenFinance}}", tests=save_id("billId")),
    req("10 Generate monthly bills from queued readings", "POST", "/api/bills/generate-monthly", bearer="{{accessTokenFinance}}", tests=EXPECT_OK),
    req("11 List pending bills", "GET", "/api/bills/pending", bearer="{{accessTokenFinance}}",
        query=[("page", "0"), ("size", "20")], tests=EXPECT_OK),
    req("12 Approve bill", "PUT", "/api/bills/{{billId}}/approve", bearer="{{accessTokenFinance}}", tests=EXPECT_OK),
    req("13 Approve bill POST alias", "POST", "/api/bills/{{billId}}/approve", bearer="{{accessTokenFinance}}",
        tests=["pm.test('Already approved or OK', () => pm.expect(pm.response.code).to.be.oneOf([200, 400]));"]),
    req("14 Customer view bills", "GET", "/api/bills/user/{{customerUserId}}", bearer="{{accessTokenCustomer}}",
        query=[("page", "0"), ("size", "10")], tests=EXPECT_OK),
    req("15 Record partial payment", "POST", "/api/payments", PAYMENT_PARTIAL, "{{accessTokenFinance}}",
        prerequest=UNIQUE_PAY, tests=EXPECT_OK),
    req("16 View notifications", "GET", "/api/notifications/user/{{customerUserId}}", bearer="{{accessTokenCustomer}}",
        query=[("page", "0"), ("size", "10")],
        tests=SAVE_ID + ["saveNotificationId();", "pm.test('OK', () => pm.expect(pm.response.code).to.equal(200));"]),
], "End-to-end: create active customer → meter → reading → bill → approve → pay")

# 02 PUBLIC
public_auth = folder("02 — PUBLIC Authentication", [
    val_pair("Signup", "POST", "/api/auth/signup", SIGNUP, INVALID_SIGNUP, prerequest=UNIQUE_SIGNUP),
    req("Register (alias)", "POST", "/api/auth/register", SIGNUP, prerequest=UNIQUE_SIGNUP, tests=EXPECT_OK),
    req("Verify registration OTP", "POST", "/api/auth/verify-otp",
        {"email": "{{uniqueEmail}}", "otpCode": "{{otpCode}}"},
        desc="Run after Register/Signup and set otpCode from server logs", tests=EXPECT_OK),
    req("Verify registration OTP invalid length", "POST", "/api/auth/verify-otp",
        {"email": "{{uniqueEmail}}", "otpCode": "123"},
        tests=EXPECT_400),
    req("Login valid", "POST", "/api/auth/login", {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD}, tests=save_login("Admin")),
    req("Login wrong password", "POST", "/api/auth/login", {"email": "admin@wasac.rw", "password": "wrong"}, tests=EXPECT_401_403),
    req("Login unknown email", "POST", "/api/auth/login", {"email": "nobody@test.wasac.rw", "password": DEFAULT_PASSWORD}, tests=EXPECT_401_403),
    req("Refresh token", "POST", "/api/auth/refresh", {"refreshToken": "{{refreshTokenAdmin}}"}, tests=EXPECT_OK),
    req("Refresh invalid token", "POST", "/api/auth/refresh", {"refreshToken": "bad-token"}, tests=EXPECT_401_403),
    req("Forgot password", "POST", "/api/auth/forgot-password", {"email": "admin@wasac.rw"}, tests=EXPECT_OK),
    req("Verify reset OTP", "POST", "/api/auth/verify-reset-otp",
        {"email": "admin@wasac.rw", "otpCode": "{{otpCode}}"},
        desc="Set otpCode from server logs after forgot-password"),
    req("Reset password", "POST", "/api/auth/reset-password",
        {"email": "admin@wasac.rw", "newPassword": ADMIN_PASSWORD},
        desc="Run after verify-reset-otp. Resets to same password for repeatable tests"),
    req("Logout", "POST", "/api/auth/logout", {"refreshToken": "{{refreshTokenAdmin}}"}, bearer="{{accessTokenAdmin}}"),
])

# 03 ADMIN
admin = folder("03 — ROLE_ADMIN", [
    folder("Users (staff)", [
        req("Create staff", "POST", "/api/users", STAFF_USER, bearer="{{accessTokenAdmin}}", prerequest=UNIQUE_STAFF, tests=save_id("userId")),
        req("List users", "GET", "/api/users", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Search users", "GET", "/api/users/search", bearer="{{accessTokenAdmin}}", query=[("keyword", "admin"), ("page", "0"), ("size", "10")]),
        req("Get user", "GET", "/api/users/{{userId}}", bearer="{{accessTokenAdmin}}"),
        req("Update user", "PUT", "/api/users/{{userId}}", {"fullName": "Updated Staff"}, bearer="{{accessTokenAdmin}}"),
        req("Activate user", "PUT", "/api/users/{{userId}}/activate", bearer="{{accessTokenAdmin}}"),
        req("Deactivate user", "PUT", "/api/users/{{userId}}/deactivate", bearer="{{accessTokenAdmin}}"),
        req("Assign role", "PUT", "/api/users/{{userId}}/roles", {"roles": ["ROLE_OPERATOR"]}, bearer="{{accessTokenAdmin}}"),
        req("Delete user", "DELETE", "/api/users/{{userId}}", bearer="{{accessTokenAdmin}}"),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Account holders", [
        req("List account holders", "GET", "/api/users/customers", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Search account holders", "GET", "/api/users/customers/search", bearer="{{accessTokenAdmin}}",
            query=[("keyword", "Jean"), ("page", "0"), ("size", "10")]),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Meters", [
        req("Assign meter", "POST", "/api/meters", METER, bearer="{{accessTokenAdmin}}", prerequest=UNIQUE_METER, tests=save_id("meterId")),
        req("List meters", "GET", "/api/meters", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Search meters", "GET", "/api/meters/search", bearer="{{accessTokenAdmin}}", query=[("meterNumber", "MTR"), ("page", "0"), ("size", "10")]),
        req("Meters by user", "GET", "/api/meters/user/{{customerUserId}}", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "10")]),
        req("Get meter", "GET", "/api/meters/{{meterId}}", bearer="{{accessTokenAdmin}}"),
        req("Update meter", "PUT", "/api/meters/{{meterId}}", {**METER, "status": "INACTIVE"}, bearer="{{accessTokenAdmin}}"),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Tariffs", [
        req("Create tariff", "POST", "/api/tariffs", TARIFF, bearer="{{accessTokenAdmin}}", prerequest=UNIQUE_TARIFF, tests=save_id("tariffId")),
        req("List tariffs", "GET", "/api/tariffs", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "10")]),
        req("Get tariff", "GET", "/api/tariffs/{{tariffId}}", bearer="{{accessTokenAdmin}}"),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Bills", [
        req("Generate bill", "POST", "/api/bills/generate", BILL, bearer="{{accessTokenAdmin}}", tests=save_id("billId")),
        req("List bills", "GET", "/api/bills", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Pending bills", "GET", "/api/bills/pending", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Bills by user", "GET", "/api/bills/user/{{customerUserId}}", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "10")]),
        req("Get bill", "GET", "/api/bills/{{billId}}", bearer="{{accessTokenAdmin}}"),
        req("Approve bill", "PUT", "/api/bills/{{billId}}/approve", bearer="{{accessTokenAdmin}}"),
        req("Reject bill", "PUT", "/api/bills/{{billId}}/reject", bearer="{{accessTokenAdmin}}"),
        req("Search bills", "GET", "/api/bills/search", bearer="{{accessTokenAdmin}}", query=[("billNumber", "BILL"), ("page", "0"), ("size", "10")]),
        req("FORBIDDEN — record payment", "POST", "/api/payments", PAYMENT_PARTIAL, bearer="{{accessTokenAdmin}}", prerequest=UNIQUE_PAY, tests=EXPECT_403),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Reports & Audit", [
        req("User report", "GET", "/api/reports/users", bearer="{{accessTokenAdmin}}"),
        req("Billing report", "GET", "/api/reports/billing", bearer="{{accessTokenAdmin}}"),
        req("Payment report", "GET", "/api/reports/payments", bearer="{{accessTokenAdmin}}"),
        req("Revenue report", "GET", "/api/reports/revenue", bearer="{{accessTokenAdmin}}"),
        req("Outstanding balances", "GET", "/api/reports/outstanding-balances", bearer="{{accessTokenAdmin}}"),
        req("Internal audit log", "POST", "/api/audit/internal", {
            "userId": "{{adminUserId}}",
            "action": "POSTMAN_INTERNAL_TEST",
            "entityName": "PostmanCollection",
            "entityId": "{{billId}}",
            "ipAddress": "127.0.0.1",
            "details": "Internal audit endpoint coverage"
        }, bearer="{{accessTokenAdmin}}", tests=EXPECT_OK),
        req("Audit logs", "GET", "/api/audit", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
        req("Audit search", "GET", "/api/audit/search", bearer="{{accessTokenAdmin}}", query=[("action", "LOGIN"), ("page", "0"), ("size", "10")]),
        req("Audit by user", "GET", "/api/audit/user/{{userId}}", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "10")]),
        req("Internal notification", "POST", "/api/notifications/internal", {
            "userId": "{{customerUserId}}",
            "title": "Postman internal notification",
            "message": "Internal notification endpoint coverage"
        }, bearer="{{accessTokenAdmin}}", tests=save_id("notificationId")),
        req("List notifications", "GET", "/api/notifications", bearer="{{accessTokenAdmin}}", query=[("page", "0"), ("size", "20")]),
    ], bearer="{{accessTokenAdmin}}"),
    folder("Files", [
        req("Upload file", "POST", "/api/files/upload", bearer="{{accessTokenAdmin}}",
            desc="Select a PDF/PNG/JPG under 5MB in Body → form-data → file",
            formdata=[
                {"key": "file", "type": "file", "src": ""},
                {"key": "entityType", "value": "BILL", "type": "text"},
                {"key": "entityId", "value": "{{billId}}", "type": "text"},
            ], tests=save_id("fileId")),
        req("Download file", "GET", "/api/files/{{fileId}}", bearer="{{accessTokenAdmin}}",
            tests=["pm.test('OK', () => pm.expect(pm.response.code).to.equal(200));"]),
    ], bearer="{{accessTokenAdmin}}"),
], bearer="{{accessTokenAdmin}}")

# 04 OPERATOR
operator = folder("04 — ROLE_OPERATOR", [
    req("List account holders", "GET", "/api/users/customers", bearer="{{accessTokenOperator}}", query=[("page", "0"), ("size", "20")]),
    req("List meters", "GET", "/api/meters", bearer="{{accessTokenOperator}}", query=[("page", "0"), ("size", "20")]),
    req("Capture reading", "POST", "/api/readings", READING, bearer="{{accessTokenOperator}}", tests=EXPECT_OK),
    req("List readings", "GET", "/api/readings", bearer="{{accessTokenOperator}}", query=[("page", "0"), ("size", "20")]),
    req("Readings by meter", "GET", "/api/readings/meter/{{meterId}}", bearer="{{accessTokenOperator}}", query=[("page", "0"), ("size", "10")]),
    req("Search readings", "GET", "/api/readings/search", bearer="{{accessTokenOperator}}", query=[("meterId", "{{meterId}}"), ("page", "0"), ("size", "10")]),
    req("View profile", "GET", "/api/profile", bearer="{{accessTokenOperator}}"),
    req("FORBIDDEN — create user", "POST", "/api/users", STAFF_USER, bearer="{{accessTokenOperator}}", prerequest=UNIQUE_STAFF, tests=EXPECT_403),
    req("FORBIDDEN — record payment", "POST", "/api/payments", PAYMENT_PARTIAL, bearer="{{accessTokenOperator}}", prerequest=UNIQUE_PAY, tests=EXPECT_403),
], bearer="{{accessTokenOperator}}")

# 05 FINANCE
finance = folder("05 — ROLE_FINANCE", [
    req("Pending bills", "GET", "/api/bills/pending", bearer="{{accessTokenFinance}}", query=[("page", "0"), ("size", "20")]),
    req("Approve bill", "PUT", "/api/bills/{{billId}}/approve", bearer="{{accessTokenFinance}}"),
    req("Reject bill", "PUT", "/api/bills/{{billId}}/reject", bearer="{{accessTokenFinance}}"),
    req("Generate bill", "POST", "/api/bills/generate", BILL, bearer="{{accessTokenFinance}}"),
    req("Record payment", "POST", "/api/payments", PAYMENT_PARTIAL, bearer="{{accessTokenFinance}}", prerequest=UNIQUE_PAY, tests=EXPECT_OK),
    req("List payments", "GET", "/api/payments", bearer="{{accessTokenFinance}}", query=[("page", "0"), ("size", "20")]),
    req("Payments by bill", "GET", "/api/payments/bill/{{billId}}", bearer="{{accessTokenFinance}}", query=[("page", "0"), ("size", "10")]),
    req("Payments by user", "GET", "/api/payments/user/{{customerUserId}}", bearer="{{accessTokenFinance}}", query=[("page", "0"), ("size", "10")]),
    req("Revenue report", "GET", "/api/reports/revenue", bearer="{{accessTokenFinance}}"),
    req("Outstanding balances", "GET", "/api/reports/outstanding-balances", bearer="{{accessTokenFinance}}"),
    req("FORBIDDEN — assign meter", "POST", "/api/meters", METER, bearer="{{accessTokenFinance}}", prerequest=UNIQUE_METER, tests=EXPECT_403),
    req("FORBIDDEN — capture reading", "POST", "/api/readings", READING, bearer="{{accessTokenFinance}}", tests=EXPECT_403),
], bearer="{{accessTokenFinance}}")

# 06 CUSTOMER
customer_f = folder("06 — ROLE_CUSTOMER", [
    req("View profile", "GET", "/api/profile", bearer="{{accessTokenCustomer}}"),
    req("Update profile", "PUT", "/api/profile", {"fullName": "Jean Updated", "phoneNumber": "{{uniquePhone}}"}, bearer="{{accessTokenCustomer}}"),
    req("List bills", "GET", "/api/bills", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "20")]),
    req("Bills by user", "GET", "/api/bills/user/{{customerUserId}}", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "10")]),
    req("Get bill", "GET", "/api/bills/{{billId}}", bearer="{{accessTokenCustomer}}"),
    req("Payment history", "GET", "/api/payments/user/{{customerUserId}}", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "10")]),
    req("Search bills", "GET", "/api/bills/search", bearer="{{accessTokenCustomer}}", query=[("billNumber", "BILL"), ("page", "0"), ("size", "10")]),
    req("List payments", "GET", "/api/payments", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "10")]),
    req("Payments by bill", "GET", "/api/payments/bill/{{billId}}", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "10")]),
    req("Notifications", "GET", "/api/notifications/user/{{customerUserId}}", bearer="{{accessTokenCustomer}}", query=[("page", "0"), ("size", "10")],
        tests=SAVE_ID + ["saveNotificationId();"]),
    req("Mark notification read", "PUT", "/api/notifications/{{notificationId}}/read", bearer="{{accessTokenCustomer}}"),
    req("Upload file", "POST", "/api/files/upload", bearer="{{accessTokenCustomer}}",
        desc="Attach PDF/PNG/JPG in form-data",
        formdata=[{"key": "file", "type": "file", "src": ""}], tests=save_id("fileId")),
    req("FORBIDDEN — approve bill", "PUT", "/api/bills/{{billId}}/approve", bearer="{{accessTokenCustomer}}", tests=EXPECT_403),
    req("FORBIDDEN — create tariff", "POST", "/api/tariffs", TARIFF, bearer="{{accessTokenCustomer}}", prerequest=UNIQUE_TARIFF, tests=EXPECT_403),
], bearer="{{accessTokenCustomer}}")

# 99 Validation (needs setup logins + E2E ids for some)
validation = folder("99 — Validation (valid + invalid payloads)", [
    req("Setup — Login ADMIN", "POST", "/api/auth/login", {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD}, tests=save_login("Admin")),
    req("Setup — Login OPERATOR", "POST", "/api/auth/login", {"email": "operator@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Operator")),
    req("Setup — Login FINANCE", "POST", "/api/auth/login", {"email": "finance@wasac.rw", "password": DEFAULT_PASSWORD}, tests=save_login("Finance")),
    val_pair("Signup", "POST", "/api/auth/signup", SIGNUP, INVALID_SIGNUP, prerequest=UNIQUE_SIGNUP),
    val_pair("Staff user", "POST", "/api/users", STAFF_USER, INVALID_STAFF, "{{accessTokenAdmin}}", UNIQUE_STAFF),
    val_pair("Meter", "POST", "/api/meters", METER, INVALID_METER, "{{accessTokenAdmin}}", UNIQUE_METER),
    val_pair("Tariff", "POST", "/api/tariffs", TARIFF, INVALID_TARIFF, "{{accessTokenAdmin}}", UNIQUE_TARIFF),
    val_pair("Reading", "POST", "/api/readings", READING, INVALID_READING, "{{accessTokenOperator}}"),
    val_pair("Payment", "POST", "/api/payments", PAYMENT_PARTIAL, INVALID_PAYMENT, "{{accessTokenFinance}}", UNIQUE_PAY),
    folder("Duplicate national ID", [
        req("Signup first user", "POST", "/api/auth/signup", SIGNUP, prerequest=UNIQUE_SIGNUP, tests=EXPECT_OK),
        req("Duplicate nationalId", "POST", "/api/auth/signup", {
            "fullName": "Duplicate User",
            "email": "dup-second@test.wasac.rw",
            "password": DEFAULT_PASSWORD,
            "phoneNumber": "0788888888",
            "nationalId": "{{uniqueNationalId}}",
        }, tests=EXPECT_400),
    ]),
], "Valid expects 200/201; invalid expects 400")

# 98 Security
security = folder("98 — Security (JWT & roles)", [
    req("No JWT — list users", "GET", "/api/users", tests=EXPECT_401_403),
    req("Invalid JWT", "GET", "/api/users", bearer="invalid.token.here", tests=EXPECT_401_403),
    req("Operator cannot list all users", "GET", "/api/users", bearer="{{accessTokenOperator}}", tests=EXPECT_403),
    req("Customer cannot POST users", "POST", "/api/users", STAFF_USER, bearer="{{accessTokenCustomer}}", prerequest=UNIQUE_STAFF, tests=EXPECT_403),
    req("Operator cannot approve bill", "PUT", "/api/bills/{{billId}}/approve", bearer="{{accessTokenOperator}}", tests=EXPECT_403),
], "Run after 00 Setup")

collection["item"] = [setup, e2e, public_auth, admin, operator, finance, customer_f, validation, security]

OUT = "/home/rca/projects/java/rw/postman"
with open(f"{OUT}/UBS-Full-Test-Collection.postman_collection.json", "w") as f:
    json.dump(collection, f, indent=2)

env = {
    "id": "ubs-local-env",
    "name": "UBS Local",
    "values": [{"key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": True}],
    "_postman_variable_scope": "environment",
}
with open(f"{OUT}/UBS-Local.postman_environment.json", "w") as f:
    json.dump(env, f, indent=2)

# Reference payloads for manual testing (mirrors collection)
test_data = {
    "meta": {
        "collection": "UBS-Full-Test-Collection.postman_collection.json",
        "environment": "UBS-Local.postman_environment.json",
        "baseUrl": "http://localhost:8080",
        "defaultPassword": DEFAULT_PASSWORD,
        "adminPassword": ADMIN_PASSWORD,
        "runOrder": ["00 Setup", "01 E2E", "02-06 role folders", "99 Validation", "98 Security"],
        "otpNote": "After signup/forgot-password, copy OTP from app console into collection variable otpCode"
    },
    "seedUsers": {
        "admin": {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD, "role": "ROLE_ADMIN"},
        "operator": {"email": "operator@wasac.rw", "password": DEFAULT_PASSWORD, "role": "ROLE_OPERATOR"},
        "finance": {"email": "finance@wasac.rw", "password": DEFAULT_PASSWORD, "role": "ROLE_FINANCE"}
    },
    "validPayloads": {
        "signup": SIGNUP,
        "verifyOtp": {"email": "{{uniqueEmail}}", "otpCode": "{{otpCode}}"},
        "login": {"email": "admin@wasac.rw", "password": ADMIN_PASSWORD},
        "staffUser": STAFF_USER,
        "meter": METER,
        "tariff": TARIFF,
        "reading": READING,
        "bill": BILL,
        "payment": PAYMENT_PARTIAL,
        "profileUpdate": {"fullName": "Jean Updated", "phoneNumber": "0781234567", "address": "Kigali"}
    },
    "invalidPayloads": {
        "signup": INVALID_SIGNUP,
        "staffUser": INVALID_STAFF,
        "meter": INVALID_METER,
        "tariff": INVALID_TARIFF,
        "reading": INVALID_READING,
        "payment": INVALID_PAYMENT
    },
    "expectedStatus": {
        "valid": [200, 201],
        "validationError": [400, 422],
        "forbidden": [403],
        "unauthorized": [401, 403]
    },
    "coverage": {
        "modules": ["auth", "users", "accountHolders", "meters", "readings", "tariffs", "bills", "payments", "notifications", "reports", "audit", "files", "profile"],
        "securityTests": ["no JWT", "bad JWT", "cross-role 403"],
        "businessRules": ["duplicate nationalId", "reading < previous", "overpayment", "single role per staff", "FINANCE-only payments", "CUSTOMER-only profile update"]
    }
}
with open(f"{OUT}/UBS-Test-Data.json", "w") as f:
    json.dump(test_data, f, indent=2)

print("Generated:")
print("  postman/UBS-Full-Test-Collection.postman_collection.json")
print("  postman/UBS-Local.postman_environment.json")
print("  postman/UBS-Test-Data.json")
