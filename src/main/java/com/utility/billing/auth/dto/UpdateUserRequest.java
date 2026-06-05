package com.utility.billing.auth.dto;

import com.utility.billing.common.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Update user details request")
public class UpdateUserRequest {

	@Size(min = 3, max = 100)
	@Pattern(regexp = ValidationPatterns.FULL_NAME)
	private String fullName;

	@Pattern(regexp = ValidationPatterns.PHONE)
	private String phoneNumber;

	@Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "LOCKED"})
	private String status;

	private String address;
}
