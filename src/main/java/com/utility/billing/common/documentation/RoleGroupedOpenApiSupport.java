package com.utility.billing.common.documentation;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.web.method.HandlerMethod;

import java.util.Collections;
import java.util.List;

public final class RoleGroupedOpenApiSupport {

	private RoleGroupedOpenApiSupport() {
	}

	public static GroupedOpenApi forRole(HandlerMethodRoleRegistry registry, String role,
			String displayName, String description) {
		String groupId = role.toLowerCase().replace("role_", "");

		return GroupedOpenApi.builder()
				.group(groupId)
				.displayName(displayName)
				.pathsToMatch("/api/**")
				.addOpenApiMethodFilter(method -> registry.isMethodAccessible(method, role))
				.addOpenApiCustomizer(SrsOpenApiTags.orderCustomizer())
				.addOpenApiCustomizer(roleInfoCustomizer(role, description))
				.addOperationCustomizer(roleBadgeCustomizer(role))
				.addOperationCustomizer(securedEndpointCustomizer())
				.build();
	}

	public static GroupedOpenApi publicApis(HandlerMethodRoleRegistry registry) {
		return GroupedOpenApi.builder()
				.group("public")
				.displayName("PUBLIC — No login required")
				.pathsToMatch("/api/auth/**")
				.addOpenApiMethodFilter(method -> registry.isMethodAccessible(method, "PUBLIC"))
				.addOperationCustomizer(publicEndpointCustomizer())
				.addOpenApiCustomizer(SrsOpenApiTags.orderCustomizer())
				.addOpenApiCustomizer(openApi -> {
					openApi.setSecurity(Collections.emptyList());
					if (openApi.getInfo() != null) {
						openApi.getInfo().setTitle("UBS — Public Endpoints");
						openApi.getInfo().setDescription("""
								**No JWT required.** Customer signup (nationalId required), login, OTP, password reset.

								Flow: signup → verify-otp → login → switch to ROLE_CUSTOMER group.
								""");
					}
				})
				.build();
	}

	public static GroupedOpenApi forExamFlow(ExamFlowEndpointRegistry examRegistry, String groupId,
			String displayName, String description, java.util.Set<String> endpoints) {
		return GroupedOpenApi.builder()
				.group(groupId)
				.displayName(displayName)
				.pathsToMatch("/api/**")
				.addOpenApiMethodFilter(method -> examRegistry.matches(method, endpoints))
				.addOpenApiCustomizer(SrsOpenApiTags.orderCustomizer())
				.addOpenApiCustomizer(openApi -> {
					if (openApi.getInfo() != null) {
						openApi.getInfo().setTitle("UBS — " + displayName);
						openApi.getInfo().setDescription(description);
					}
					SrsOpenApiTags.orderCustomizer().customise(openApi);
				})
				.addOperationCustomizer(endpointSecurityCustomizer())
				.build();
	}

	public static GroupedOpenApi allApis(HandlerMethodRoleRegistry registry) {
		return GroupedOpenApi.builder()
				.group("all")
				.displayName("ALL — Complete API catalog")
				.pathsToMatch("/api/**")
				.addOpenApiMethodFilter(registry::isDocumented)
				.addOperationCustomizer(endpointSecurityCustomizer())
				.addOpenApiCustomizer(SrsOpenApiTags.orderCustomizer())
				.addOpenApiCustomizer(openApi -> {
					if (openApi.getInfo() != null) {
						openApi.getInfo().setTitle("UBS — All Endpoints");
						openApi.getInfo().setDescription("""
								**One identity:** User = Customer = Account Holder (`userId` everywhere).

								Use the **group dropdown** (top right) to filter by role.

								**Exam demo (few minutes):** pick **EXAM — Complete Demo Flow** or per-portal EXAM groups.

								| Group | Login as |
								|-------|----------|
								| EXAM — Complete Demo Flow | follow 20 steps in catalog |
								| PUBLIC / EXAM — 1. Authentication | — (signup first) |
								| ROLE_ADMIN / EXAM — 2. Admin | admin@wasac.rw |
								| ROLE_OPERATOR / EXAM — 3. Operator | operator@wasac.rw |
								| ROLE_FINANCE / EXAM — 4. Finance | finance@wasac.rw |
								| ROLE_CUSTOMER / EXAM — 5. Customer | self-register + OTP |

								Passwords: admin uses `Admin@123`; operator and finance use `Password@123`

								Verify: `GET /api/docs/exam-flow-catalog` · `GET /api/docs/role-catalog`
								""");
					}
				})
				.build();
	}

	private static OpenApiCustomizer roleInfoCustomizer(String role, String description) {
		return openApi -> {
			if (openApi.getInfo() != null) {
				openApi.getInfo().setTitle("UBS — " + role);
				openApi.getInfo().setDescription(description);
			}
			SrsOpenApiTags.orderCustomizer().customise(openApi);
		};
	}

	private static OperationCustomizer roleBadgeCustomizer(String role) {
		return (operation, handlerMethod) -> {
			operation.addExtension("x-role-group", role);
			operation.addExtension("x-required-roles",
					RoleAuthorizationInspector.resolveRoles(handlerMethod).stream().toList());
			return operation;
		};
	}

	private static OperationCustomizer publicEndpointCustomizer() {
		return (operation, handlerMethod) -> {
			operation.setSecurity(Collections.emptyList());
			return operation;
		};
	}

	private static OperationCustomizer securedEndpointCustomizer() {
		return (operation, handlerMethod) -> {
			operation.setSecurity(List.of(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth")));
			return operation;
		};
	}

	private static OperationCustomizer endpointSecurityCustomizer() {
		return (operation, handlerMethod) -> {
			if (RoleAuthorizationInspector.resolveRoles(handlerMethod).contains("PUBLIC")) {
				operation.setSecurity(Collections.emptyList());
			} else {
				operation.setSecurity(List.of(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth")));
			}
			return operation;
		};
	}
}
