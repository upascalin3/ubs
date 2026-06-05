package com.utility.billing.auth.controller;

import com.utility.billing.auth.dto.ProfileResponse;
import com.utility.billing.auth.dto.ProfileUpdateRequest;
import com.utility.billing.auth.dto.UpdateUserRequest;
import com.utility.billing.auth.dto.UserResponse;
import com.utility.billing.auth.service.UserService;
import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Current user profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

	private final UserService userService;

	public ProfileController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@Operation(summary = "View profile", description = "Authenticated user profile")
	public ApiResponse<ProfileResponse> getProfile() {
		return ApiResponse.success(toProfile(userService.getCurrentProfile()));
	}

	@PutMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Update profile", description = "Required roles: ROLE_CUSTOMER")
	public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
		var update = new UpdateUserRequest();
		update.setFullName(request.getFullName());
		update.setPhoneNumber(request.getPhoneNumber());
		update.setAddress(request.getAddress());
		UserResponse user = userService.update(SecurityUtils.getCurrentUserId(), update);
		return ApiResponse.success(toProfile(user), "Profile updated");
	}

	private ProfileResponse toProfile(UserResponse user) {
		return ProfileResponse.builder()
				.id(user.getId())
				.fullName(user.getFullName())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.nationalId(user.getNationalId())
				.address(user.getAddress())
				.status(user.getStatus())
				.emailVerified(user.isEmailVerified())
				.mustChangePassword(user.isMustChangePassword())
				.roles(user.getRoles())
				.createdAt(user.getCreatedAt())
				.build();
	}
}
