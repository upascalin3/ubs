package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Current user profile")
public class ProfileResponse {

	private UUID id;
	private String fullName;
	private String email;
	private String phoneNumber;
	private String nationalId;
	private String address;
	private String status;
	private boolean emailVerified;
	private boolean mustChangePassword;
	private List<String> roles;
	private LocalDateTime createdAt;
}
