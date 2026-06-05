# Postman Test Suite - UBS

## Files

| File | Purpose |
|------|---------|
| `UBS-Full-Test-Collection.postman_collection.json` | Full Postman collection for API, validation, role, and payment scenarios |
| `UBS-Local.postman_environment.json` | Local environment with only `baseUrl` |
| `UBS-Test-Data.json` | Reference payloads, invalid cases, expected statuses, and coverage notes |
| `generate_collection.py` | Regenerates all Postman JSON files |

## Import

1. Open Postman and import:
   - `UBS-Full-Test-Collection.postman_collection.json`
   - `UBS-Local.postman_environment.json`
2. Select the **UBS Local** environment.
3. Start the backend on `http://localhost:8080`.

## Run Order

1. `00 - Setup (RUN FIRST)` - logs in seed users and stores tokens.
2. `01 - E2E Happy Path` - creates a customer, meter, reading, bill, approval, payment, and notification.
3. `02 - PUBLIC Authentication` - signup, register, OTP, login, refresh, password reset, logout.
4. `03` to `06` - role-focused ADMIN, OPERATOR, FINANCE, and CUSTOMER coverage.
5. `99 - Validation` - valid and invalid payload matrix.
6. `98 - Security` - missing JWT, invalid JWT, and cross-role forbidden checks.

## Seed Users

| Role | Email | Password |
|------|-------|----------|
| ADMIN | `admin@wasac.rw` | `Admin@123` |
| OPERATOR | `operator@wasac.rw` | `Password@123` |
| FINANCE | `finance@wasac.rw` | `Password@123` |

## Notes

- If `Login ADMIN` returns `401`, restart the Spring Boot app. The admin seed user is repaired on startup with password `Admin@123`.
- If `401` continues after restart, reset the local DB with `scripts/reset-db.sh`, then start the app again so Flyway and the seed initializer run.
- The automated E2E path creates an active `ROLE_CUSTOMER` user through the admin API, so it does not require OTP.
- Public signup and password reset OTP endpoints are still included. For successful OTP requests, copy the OTP from server logs and set collection variable `otpCode`.
- The collection uses dynamic dates and unique emails, phones, meter numbers, tariff versions, and payment references.
- Current SRS identity model is `auth.users` as account holders/customers; use `userId` / `customerUserId`, not legacy `customerId`.

## Regenerate

```bash
python3 postman/generate_collection.py
```
