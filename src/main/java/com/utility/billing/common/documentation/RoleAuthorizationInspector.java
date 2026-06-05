package com.utility.billing.common.documentation;

import com.utility.billing.common.security.RoleName;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves which roles may call an endpoint from {@link PreAuthorize} and {@link ApiRoleDoc}.
 */
public final class RoleAuthorizationInspector {

	private static final Pattern ROLE_PATTERN =
			Pattern.compile("hasRole\\('([^']+)'\\)|hasAnyRole\\(([^)]+)\\)");

	private RoleAuthorizationInspector() {
	}

	public static Set<String> resolveRoles(HandlerMethod handlerMethod) {
		Set<String> roles = new LinkedHashSet<>();

		ApiRoleDoc roleDoc = handlerMethod.getMethodAnnotation(ApiRoleDoc.class);
		if (roleDoc == null) {
			roleDoc = handlerMethod.getBeanType().getAnnotation(ApiRoleDoc.class);
		}
		if (roleDoc != null) {
			for (String role : roleDoc.value()) {
				roles.add(normalizeRole(role));
			}
		}

		String expression = preAuthorizeExpression(handlerMethod);
		if (expression != null) {
			if (expression.contains("isAuthenticated()")) {
				roles.add("ROLE_AUTHENTICATED");
			}
			roles.addAll(parseRoleExpressions(expression));
		}

		if (roles.isEmpty() && isPublicAuth(handlerMethod)) {
			roles.add("PUBLIC");
		}

		return roles;
	}

	public static boolean isHiddenFromRoleDocs(HandlerMethod handlerMethod) {
		if (handlerMethod.getMethod().isAnnotationPresent(io.swagger.v3.oas.annotations.Hidden.class)) {
			return true;
		}
		RequestMapping mapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
		if (mapping != null) {
			return Arrays.stream(mapping.path()).anyMatch(p -> p.contains("/internal"));
		}
		return false;
	}

	public static boolean isAccessibleBy(HandlerMethod handlerMethod, String targetRole) {
		if (isHiddenFromRoleDocs(handlerMethod)) {
			return false;
		}

		Set<String> roles = resolveRoles(handlerMethod);

		if ("PUBLIC".equals(targetRole)) {
			return roles.contains("PUBLIC");
		}

		if (roles.contains("PUBLIC")) {
			return false;
		}

		if ("ROLE_AUTHENTICATED".equals(targetRole)) {
			return roles.contains("ROLE_AUTHENTICATED")
					&& roles.stream().noneMatch(RoleAuthorizationInspector::isSpecificRole);
		}

		if (roles.contains(targetRole)) {
			return true;
		}

		// Any authenticated user may call isAuthenticated()-only endpoints
		if (roles.contains("ROLE_AUTHENTICATED") && isSpecificRole(targetRole)) {
			return true;
		}

		return false;
	}

	private static boolean isSpecificRole(String role) {
		return RoleName.ADMIN.equals(role)
				|| RoleName.OPERATOR.equals(role)
				|| RoleName.FINANCE.equals(role)
				|| RoleName.CUSTOMER.equals(role);
	}

	private static boolean isPublicAuth(HandlerMethod handlerMethod) {
		return "AuthController".equals(handlerMethod.getBeanType().getSimpleName())
				&& !"logout".equals(handlerMethod.getMethod().getName());
	}

	private static String preAuthorizeExpression(HandlerMethod handlerMethod) {
		PreAuthorize methodAuth = handlerMethod.getMethodAnnotation(PreAuthorize.class);
		if (methodAuth != null) {
			return methodAuth.value();
		}
		PreAuthorize classAuth = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
		return classAuth != null ? classAuth.value() : null;
	}

	private static Set<String> parseRoleExpressions(String expression) {
		Set<String> roles = new LinkedHashSet<>();
		Matcher matcher = ROLE_PATTERN.matcher(expression);
		while (matcher.find()) {
			if (matcher.group(1) != null) {
				roles.add("ROLE_" + matcher.group(1));
			}
			if (matcher.group(2) != null) {
				for (String part : matcher.group(2).split(",")) {
					roles.add(normalizeRole(part.trim().replace("'", "")));
				}
			}
		}
		return roles;
	}

	private static String normalizeRole(String role) {
		if ("AUTHENTICATED".equals(role)) {
			return "ROLE_AUTHENTICATED";
		}
		return role.startsWith("ROLE_") ? role : "ROLE_" + role;
	}
}
