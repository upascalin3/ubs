package com.utility.billing.common.documentation;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ExamFlowEndpointRegistry {

	private final RequestMappingHandlerMapping handlerMapping;
	private final Map<String, Set<Method>> methodsByEndpointKey = new HashMap<>();

	public ExamFlowEndpointRegistry(
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		this.handlerMapping = handlerMapping;
	}

	@PostConstruct
	void init() {
		handlerMapping.getHandlerMethods().forEach(this::register);
	}

	private void register(RequestMappingInfo info, HandlerMethod handler) {
		if (RoleAuthorizationInspector.isHiddenFromRoleDocs(handler)) {
			return;
		}
		String httpMethod = info.getMethodsCondition().getMethods().stream()
				.findFirst()
				.map(Enum::name)
				.orElse("GET");
		if (info.getPathPatternsCondition() == null) {
			return;
		}
		info.getPathPatternsCondition().getPatterns().forEach(pattern -> {
			String key = httpMethod + " " + pattern.getPatternString();
			methodsByEndpointKey.computeIfAbsent(key, k -> new HashSet<>()).add(handler.getMethod());
		});
	}

	public boolean matches(Method method, Set<String> allowedEndpointKeys) {
		return allowedEndpointKeys.stream()
				.anyMatch(key -> methodsByEndpointKey.getOrDefault(key, Set.of()).contains(method));
	}

	public Set<String> resolveKeys(Method method) {
		Set<String> keys = new HashSet<>();
		methodsByEndpointKey.forEach((key, methods) -> {
			if (methods.contains(method)) {
				keys.add(key);
			}
		});
		return keys;
	}
}
