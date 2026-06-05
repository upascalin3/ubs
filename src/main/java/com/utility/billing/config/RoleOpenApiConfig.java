package com.utility.billing.config;

import com.utility.billing.common.documentation.HandlerMethodRoleRegistry;
import com.utility.billing.common.documentation.RoleGroupedOpenApiSupport;
import com.utility.billing.common.security.RoleName;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleOpenApiConfig {

	@Bean
	public GroupedOpenApi allApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.allApis(registry);
	}

	@Bean
	public GroupedOpenApi publicApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.publicApis(registry);
	}

	@Bean
	public GroupedOpenApi adminApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.forRole(
				registry,
				RoleName.ADMIN,
				"ROLE_ADMIN — Administrator",
				"""
						**Users:** create staff, list/search, activate, assign role, delete

						**Meters:** assign to `userId`, activate/deactivate

						**Tariffs · Bills · Reports · Audit · Notifications**

						Login: `admin@wasac.rw` / `Admin@123`
						""");
	}

	@Bean
	public GroupedOpenApi operatorApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.forRole(
				registry,
				RoleName.OPERATOR,
				"ROLE_OPERATOR — Meter readings",
				"""
						**Account Holders:** `GET /api/users/customers`

						**Meters:** list, search, `GET /api/meters/user/{userId}`

						**Readings:** `POST /api/readings` (triggers bill queue)

						Login: `operator@wasac.rw` / `Password@123`
						""");
	}

	@Bean
	public GroupedOpenApi financeApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.forRole(
				registry,
				RoleName.FINANCE,
				"ROLE_FINANCE — Billing & payments",
				"""
						**Bills:** pending, approve, reject, generate

						**Payments:** `POST /api/payments` (billId only)

						**Reports:** revenue, outstanding balances

						Login: `finance@wasac.rw` / `Password@123`
						""");
	}

	@Bean
	public GroupedOpenApi customerApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.forRole(
				registry,
				RoleName.CUSTOMER,
				"ROLE_CUSTOMER — Self-service",
				"""
						**Profile:** view/update own profile

						**Bills:** `GET /api/bills/user/{userId}` (approved+ only)

						**Payments · Notifications · Files**

						Setup: PUBLIC → signup → verify-otp → login
						""");
	}

	@Bean
	public GroupedOpenApi authenticatedApis(HandlerMethodRoleRegistry registry) {
		return RoleGroupedOpenApiSupport.forRole(
				registry,
				"ROLE_AUTHENTICATED",
				"Authenticated — Any logged-in user",
				"""
						Endpoints for **any authenticated user:** logout, profile, file download.

						Use a JWT from any role after login.
						""");
	}
}
