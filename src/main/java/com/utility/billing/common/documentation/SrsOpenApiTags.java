package com.utility.billing.common.documentation;

import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SRS-aligned tag order for Swagger UI. User = Customer = Account Holder (one identity).
 */
public final class SrsOpenApiTags {

	private static final List<Tag> ORDERED = List.of(
			tag("Authentication", "Signup, login, OTP, password reset, logout"),
			tag("Profile", "View and update current user profile"),
			tag("Users", "Admin: create staff (OPERATOR/FINANCE), manage all users"),
			tag("Account Holders", "Users with ROLE_CUSTOMER — view account holders (operator/finance)"),
			tag("Meters", "Assign meters to userId — WATER or ELECTRICITY"),
			tag("Meter Readings", "Operator captures readings → triggers bill generation"),
			tag("Tariffs", "Admin configures tariff versions, VAT, service charge"),
			tag("Bills", "Generate, approve, reject — keyed by userId"),
			tag("Payments", "Record payment by billId only"),
			tag("Notifications", "Bill and payment notifications by userId"),
			tag("Reports", "User, billing, payment, revenue reports"),
			tag("Audit Logs", "System activity — admin only"),
			tag("Files", "Upload and download files")
	);

	private static final Map<String, Integer> ORDER = new LinkedHashMap<>();

	static {
		for (int i = 0; i < ORDERED.size(); i++) {
			ORDER.put(ORDERED.get(i).getName(), i);
		}
	}

	private SrsOpenApiTags() {
	}

	public static OpenApiCustomizer orderCustomizer() {
		return openApi -> {
			openApi.setTags(ORDERED);
			if (openApi.getPaths() != null) {
				openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(op -> {
					if (op.getTags() != null && op.getTags().size() > 1) {
						op.getTags().sort(Comparator.comparingInt(SrsOpenApiTags::indexOf));
					}
				}));
			}
		};
	}

	public static int indexOf(String tagName) {
		return ORDER.getOrDefault(tagName, ORDER.size());
	}

	private static Tag tag(String name, String description) {
		return new Tag().name(name).description(description);
	}
}
