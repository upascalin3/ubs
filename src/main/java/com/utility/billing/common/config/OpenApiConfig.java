package com.utility.billing.common.config;

import com.utility.billing.common.documentation.SrsOpenApiTags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		final String scheme = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("Utility Billing System (UBS)")
						.description("""
								WASAC/REG Utility Billing System — **SRS-aligned, role-filtered API docs**.

								## One identity model
								`User = Customer = Account Holder` — all operations use **`userId`**, not `customerId`.

								## How to use Swagger UI
								1. Pick a **role group** from the dropdown (top right).
								2. **PUBLIC** → signup/login (no Authorize).
								3. Secured groups → login → **Authorize** → `Bearer <accessToken>`.
								4. Each group shows **only** endpoints that role may call.

								| Group | Account | Password |
								|-------|---------|----------|
								| ROLE_ADMIN | admin@wasac.rw | Admin@123 |
								| ROLE_OPERATOR | operator@wasac.rw | Password@123 |
								| ROLE_FINANCE | finance@wasac.rw | Password@123 |
								| ROLE_CUSTOMER | self-register + OTP | — |

								**Role catalog:** `GET /api/docs/role-catalog`
								""")
						.version("1.0.0")
						.contact(new Contact().name("WASAC/REG").email("support@wasac.rw"))
						.license(new License().name("Proprietary")))
				.components(new Components().addSecuritySchemes(scheme,
						new SecurityScheme()
								.name(scheme)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT from POST /api/auth/login")));
	}

	@Bean
	public OpenApiCustomizer srsTagOrderCustomizer() {
		return SrsOpenApiTags.orderCustomizer();
	}
}
