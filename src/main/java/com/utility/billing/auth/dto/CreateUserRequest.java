package com.utility.billing.auth.dto;

import com.utility.billing.common.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Admin create staff user — one role, mustChangePassword for OPERATOR/FINANCE")
public class CreateUserRequest {

	@NotBlank
	@Size(min = 3, max = 100)
	@Pattern(regexp = ValidationPatterns.FULL_NAME)
	private String fullName;

	@NotBlank
	@Email
	@Pattern(regexp = ValidationPatterns.EMAIL)
	private String email;

	@NotBlank
	@Pattern(regexp = ValidationPatterns.PASSWORD)
	private String password;

	@NotBlank
	@Pattern(regexp = ValidationPatterns.PHONE)
	private String phoneNumber;

	@NotEmpty
	@Schema(example = "[\"ROLE_OPERATOR\"]")
	private List<String> roles;
}
