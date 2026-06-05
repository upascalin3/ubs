package com.utility.billing.common.documentation;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
public class HandlerMethodRoleRegistry {

	private final RequestMappingHandlerMapping handlerMapping;
	private final List<HandlerMethod> handlers = new ArrayList<>();

	public HandlerMethodRoleRegistry(
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		this.handlerMapping = handlerMapping;
	}

	@PostConstruct
	void init() {
		handlerMapping.getHandlerMethods().forEach((RequestMappingInfo info, HandlerMethod handler) ->
				handlers.add(handler));
	}

	public boolean isMethodAccessible(Method method, String role) {
		return handlers.stream()
				.filter(h -> h.getMethod().equals(method))
				.anyMatch(h -> RoleAuthorizationInspector.isAccessibleBy(h, role));
	}

	public boolean isDocumented(Method method) {
		return handlers.stream()
				.filter(h -> h.getMethod().equals(method))
				.anyMatch(h -> !RoleAuthorizationInspector.isHiddenFromRoleDocs(h));
	}
}
