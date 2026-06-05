package com.utility.billing.common.documentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimum endpoint sets for exam / demo flow — maps to actual UBS paths (SRS unified identity).
 */
public final class ExamFlowEndpoints {

	public static final String GROUP_PUBLIC = "exam-public";
	public static final String GROUP_ADMIN = "exam-admin";
	public static final String GROUP_OPERATOR = "exam-operator";
	public static final String GROUP_FINANCE = "exam-finance";
	public static final String GROUP_CUSTOMER = "exam-customer";
	public static final String GROUP_COMPLETE = "exam-flow";

	private ExamFlowEndpoints() {
	}

	public static final Set<String> PUBLIC = Set.of(
			"POST /api/auth/signup",
			"POST /api/auth/verify-otp",
			"POST /api/auth/login",
			"POST /api/auth/logout");

	public static final Set<String> ADMIN = Set.of(
			"POST /api/users",
			"GET /api/users",
			"POST /api/meters",
			"POST /api/tariffs");

	public static final Set<String> OPERATOR = Set.of(
			"GET /api/meters",
			"POST /api/readings",
			"GET /api/readings/meter/{meterId}");

	public static final Set<String> FINANCE = Set.of(
			"GET /api/bills",
			"PUT /api/bills/{id}/approve",
			"POST /api/payments",
			"GET /api/payments");

	public static final Set<String> CUSTOMER = Set.of(
			"GET /api/profile",
			"GET /api/bills",
			"GET /api/payments",
			"GET /api/notifications/user/{userId}");

	public static final Set<String> COMPLETE = Set.of(
			"POST /api/auth/signup",
			"POST /api/auth/verify-otp",
			"POST /api/auth/login",
			"POST /api/users",
			"GET /api/users",
			"POST /api/meters",
			"POST /api/tariffs",
			"POST /api/readings",
			"GET /api/bills",
			"PUT /api/bills/{id}/approve",
			"POST /api/payments",
			"GET /api/profile",
			"GET /api/payments",
			"GET /api/notifications/user/{userId}");

	public static final Map<String, Set<String>> BY_GROUP = Map.of(
			GROUP_PUBLIC, PUBLIC,
			GROUP_ADMIN, ADMIN,
			GROUP_OPERATOR, OPERATOR,
			GROUP_FINANCE, FINANCE,
			GROUP_CUSTOMER, CUSTOMER,
			GROUP_COMPLETE, COMPLETE);

	public static final List<FlowStep> COMPLETE_STEPS = List.of(
			step(1, "Register customer", "POST /api/auth/signup", GROUP_PUBLIC),
			step(2, "Verify OTP", "POST /api/auth/verify-otp", GROUP_PUBLIC),
			step(3, "Login customer", "POST /api/auth/login", GROUP_PUBLIC),
			step(4, "Admin login", "POST /api/auth/login", GROUP_PUBLIC),
			step(5, "Create operator + finance staff", "POST /api/users", GROUP_ADMIN),
			step(6, "Verify staff accounts", "GET /api/users", GROUP_ADMIN),
			step(7, "Assign meter to customer", "POST /api/meters", GROUP_ADMIN),
			step(8, "Create tariff", "POST /api/tariffs", GROUP_ADMIN),
			step(9, "Operator login", "POST /api/auth/login", GROUP_PUBLIC),
			step(10, "View meters", "GET /api/meters", GROUP_OPERATOR),
			step(11, "Capture reading (auto bill)", "POST /api/readings", GROUP_OPERATOR),
			step(12, "Finance login", "POST /api/auth/login", GROUP_PUBLIC),
			step(13, "View generated bills", "GET /api/bills", GROUP_FINANCE),
			step(14, "Approve bill", "PUT /api/bills/{id}/approve", GROUP_FINANCE),
			step(15, "Record payment", "POST /api/payments", GROUP_FINANCE),
			step(16, "Customer login", "POST /api/auth/login", GROUP_PUBLIC),
			step(17, "View profile", "GET /api/profile", GROUP_CUSTOMER),
			step(18, "View bills", "GET /api/bills", GROUP_CUSTOMER),
			step(19, "View payments", "GET /api/payments", GROUP_CUSTOMER),
			step(20, "View notifications", "GET /api/notifications/user/{userId}", GROUP_CUSTOMER));

	public static Map<String, String> groupDescriptions() {
		Map<String, String> d = new LinkedHashMap<>();
		d.put(GROUP_PUBLIC, """
				**Step 1–4, 9, 12, 16:** signup → verify-otp → login (any role)

				| Endpoint | Purpose |
				|----------|---------|
				| POST /api/auth/signup | Create customer, send OTP |
				| POST /api/auth/verify-otp | Activate account |
				| POST /api/auth/login | Obtain JWT |
				| POST /api/auth/logout | Invalidate token |
				""");
		d.put(GROUP_ADMIN, """
				**Step 5–8:** Admin creates staff, meter, tariff

				| Endpoint | Maps from exam doc |
				|----------|------------------|
				| POST /api/users | Create operator / finance (was /api/admin/users) |
				| GET /api/users | Verify created accounts |
				| POST /api/meters | Assign meter to customer `userId` |
				| POST /api/tariffs | Configure billing rates |

				Login: `admin@wasac.rw` / `Admin@123`
				""");
		d.put(GROUP_OPERATOR, """
				**Step 10–11:** Operator captures reading → bill auto-generated

				| Endpoint | Purpose |
				|----------|---------|
				| GET /api/meters | Select meter |
				| POST /api/readings | Save reading (triggers bill queue) |
				| GET /api/readings/meter/{meterId} | Verify reading history |

				Login: `operator@wasac.rw` / `Password@123`
				""");
		d.put(GROUP_FINANCE, """
				**Step 13–15:** Approve bill → record payment

				| Endpoint | Note |
				|----------|------|
				| GET /api/bills | View auto-generated bills |
				| PUT /api/bills/{id}/approve | PENDING → APPROVED (exam doc uses PATCH) |
				| POST /api/payments | Record payment, update balance |
				| GET /api/payments | Verify payment records |

				Login: `finance@wasac.rw` / `Password@123`
				""");
		d.put(GROUP_CUSTOMER, """
				**Step 17–20:** Customer self-service

				| Endpoint | Maps from exam doc |
				|----------|------------------|
				| GET /api/profile | View profile (was /api/users/me) |
				| GET /api/bills | View bills (was /api/customer/bills) |
				| GET /api/payments | Payment history (was /api/customer/payments) |
				| GET /api/notifications/user/{userId} | Bill/payment notifications |

				Use JWT from customer signup + OTP flow.
				""");
		d.put(GROUP_COMPLETE, """
				**All 20 exam steps in one group** — run top to bottom in ~few minutes.

				1. Signup → 2. OTP → 3. Customer login → 4. Admin login
				5–8. Create staff, meter, tariff → 9. Operator login → 10–11. Reading
				12. Finance login → 13–15. Approve + pay → 16. Customer login → 17–20. View data

				Verify checklist: `GET /api/docs/exam-flow-catalog`
				""");
		return d;
	}

	private static FlowStep step(int order, String title, String endpoint, String group) {
		return new FlowStep(order, title, endpoint, group);
	}

	public record FlowStep(int order, String title, String endpoint, String swaggerGroup) {
	}
}
