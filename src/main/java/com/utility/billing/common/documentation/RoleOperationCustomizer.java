package com.utility.billing.common.documentation;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.stream.Collectors;

@Component
public class RoleOperationCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		if (RoleAuthorizationInspector.isHiddenFromRoleDocs(handlerMethod)) {
			return operation;
		}

		var roles = RoleAuthorizationInspector.resolveRoles(handlerMethod);
		if (!roles.isEmpty()) {
			String roleText = roles.stream().collect(Collectors.joining(", "));
			operation.addExtension("x-required-roles", roles.stream().toList());
			String existing = operation.getDescription() == null ? "" : operation.getDescription();
			if (!existing.contains("Required roles:")) {
				operation.setDescription((existing.isBlank() ? "" : existing + "\n\n")
						+ "**Required roles:** `" + roleText + "`");
			}
		}
		return operation;
	}
}
