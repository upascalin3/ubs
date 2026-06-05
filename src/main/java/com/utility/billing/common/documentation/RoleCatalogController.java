package com.utility.billing.common.documentation;

import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.common.security.RoleName;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON catalog of endpoints per role — use to verify Swagger role groups match security rules.
 */
@Hidden
@RestController
@RequestMapping("/api/docs")
public class RoleCatalogController {

	private static final List<String> ROLES = List.of(
			"PUBLIC",
			RoleName.ADMIN,
			RoleName.OPERATOR,
			RoleName.FINANCE,
			RoleName.CUSTOMER,
			"ROLE_AUTHENTICATED");

	private final RequestMappingHandlerMapping handlerMapping;

	public RoleCatalogController(
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		this.handlerMapping = handlerMapping;
	}

	@GetMapping("/exam-flow-catalog")
	public ApiResponse<Map<String, Object>> examFlowCatalog() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("swaggerGroups", List.of(
				"EXAM — 1. Authentication (PUBLIC)",
				"EXAM — 2. Admin Portal",
				"EXAM — 3. Operator Portal",
				"EXAM — 4. Finance Portal",
				"EXAM — 5. Customer Portal",
				"EXAM — Complete Demo Flow (all steps)"));
		result.put("pathMappings", Map.of(
				"exam /api/admin/users", "actual POST/GET /api/users",
				"exam /api/users/me", "actual GET /api/profile",
				"exam /api/customer/bills", "actual GET /api/bills",
				"exam /api/customer/payments", "actual GET /api/payments",
				"exam /api/customer/notifications", "actual GET /api/notifications/user/{userId}",
				"exam PATCH /bills/{id}/approve", "actual PUT /api/bills/{id}/approve"));
		result.put("steps", ExamFlowEndpoints.COMPLETE_STEPS);
		Map<String, List<String>> endpointsByGroup = new LinkedHashMap<>();
		ExamFlowEndpoints.BY_GROUP.forEach((group, keys) ->
				endpointsByGroup.put(group, keys.stream().sorted().toList()));
		result.put("endpointsByGroup", endpointsByGroup);
		result.put("checklist", List.of(
				"Signup", "OTP Verification", "Login", "JWT Security", "Role Authorization",
				"Create Staff", "Create Meter", "Create Tariff", "Capture Reading",
				"Auto Bill Generation", "Bill Approval", "Payment Processing",
				"Auto Status Update", "Notifications", "Customer Bill Viewing", "Customer Payment History"));
		return ApiResponse.success(result);
	}

	@GetMapping("/role-catalog")
	public ApiResponse<Map<String, List<String>>> roleCatalog() {
		Map<String, List<String>> catalog = new LinkedHashMap<>();
		for (String role : ROLES) {
			catalog.put(role, new ArrayList<>());
		}

		Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
		List<String> allPaths = new ArrayList<>();

		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlers.entrySet()) {
			HandlerMethod handler = entry.getValue();
			if (RoleAuthorizationInspector.isHiddenFromRoleDocs(handler)) {
				continue;
			}
			String path = entry.getKey().getPathPatternsCondition() != null
					? entry.getKey().getPathPatternsCondition().getPatterns().iterator().next().getPatternString()
					: entry.getKey().toString();
			String method = entry.getKey().getMethodsCondition().getMethods().stream()
					.findFirst()
					.map(Enum::name)
					.orElse("GET");
			String endpoint = method + " " + path;
			allPaths.add(endpoint);

			for (String role : ROLES) {
				if (RoleAuthorizationInspector.isAccessibleBy(handler, role)) {
					catalog.get(role).add(endpoint);
				}
			}
		}

		catalog.put("_all", allPaths.stream().sorted().distinct().toList());
		catalog.replaceAll((k, v) -> v.stream().sorted().toList());

		return ApiResponse.success(new TreeMap<>(catalog));
	}
}
