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
