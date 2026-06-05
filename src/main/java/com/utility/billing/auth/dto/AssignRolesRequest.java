package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Assign roles to user")
public class AssignRolesRequest {

	@NotEmpty
	@Schema(example = "[\"ROLE_FINANCE\", \"ROLE_OPERATOR\"]")
	private List<String> roles;
}
