package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Current user profile update request")
public class ProfileUpdateRequest {

	private static final String FULL_NAME_PATTERN = "^[A-Za-z ]+$";
	private static final String PHONE_PATTERN = "^07[0-9]{8}$";

	@Size(min = 3, max = 100)
	@Pattern(regexp = FULL_NAME_PATTERN)
	@Schema(example = "John Doe")
	private String fullName;

	@Pattern(regexp = PHONE_PATTERN)
	@Schema(example = "0781234567")
	private String phoneNumber;

	@Schema(example = "Kigali, Rwanda")
	private String address;
}
