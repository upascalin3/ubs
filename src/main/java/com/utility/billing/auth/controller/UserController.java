package com.utility.billing.auth.controller;

import com.utility.billing.auth.dto.AssignRolesRequest;
import com.utility.billing.auth.dto.CreateUserRequest;
import com.utility.billing.auth.dto.UpdateUserRequest;
import com.utility.billing.auth.dto.UserResponse;
import com.utility.billing.auth.service.UserService;
import com.utility.billing.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Admin — staff and user management (one role per user)")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create staff user", description = "ROLE_ADMIN — create OPERATOR/FINANCE. Sets mustChangePassword=true. Exactly one role.")
	public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		return ApiResponse.success(userService.create(request), "User created");
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "List users", description = "Accessible only by ROLE_ADMIN. Supports pagination.")
	public ApiResponse<Page<UserResponse>> list(
			@ParameterObject @PageableDefault(size = 20, sort = "email") Pageable pageable) {
		return ApiResponse.success(userService.list(pageable));
	}

	@GetMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Search users", description = "Accessible only by ROLE_ADMIN.")
	public ApiResponse<Page<UserResponse>> search(@RequestParam String keyword,
			@ParameterObject @PageableDefault(size = 20, sort = "email") Pageable pageable) {
		return ApiResponse.success(userService.search(keyword, pageable));
	}

	@GetMapping("/customers")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "List account holders", tags = {"Account Holders"},
			description = "Users with ROLE_CUSTOMER — operator/finance view (replaces legacy /api/customers)")
	public ApiResponse<Page<UserResponse>> listCustomers(
			@ParameterObject @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
		return ApiResponse.success(userService.listCustomers(pageable));
	}

	@GetMapping("/customers/search")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "Search account holders", tags = {"Account Holders"},
			description = "Search by name, email, or nationalId")
	public ApiResponse<Page<UserResponse>> searchCustomers(@RequestParam String keyword,
			@ParameterObject @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
		return ApiResponse.success(userService.searchCustomers(keyword, pageable));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get user by ID", description = "Accessible only by ROLE_ADMIN.")
	public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
		return ApiResponse.success(userService.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update user details", description = "Accessible only by ROLE_ADMIN.")
	public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.success(userService.update(id, request), "User updated");
	}

	@PutMapping("/{id}/activate")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Activate user", description = "Accessible only by ROLE_ADMIN.")
	public ApiResponse<UserResponse> activate(@PathVariable UUID id) {
		return ApiResponse.success(userService.activate(id), "User activated");
	}

	@PutMapping("/{id}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Deactivate user", description = "Accessible only by ROLE_ADMIN.")
	public ApiResponse<UserResponse> deactivate(@PathVariable UUID id) {
		return ApiResponse.success(userService.deactivate(id), "User deactivated");
	}

	@PutMapping("/{id}/roles")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Assign role", description = "ROLE_ADMIN — exactly one role per user (SRS)")
	public ApiResponse<UserResponse> assignRoles(@PathVariable UUID id,
			@Valid @RequestBody AssignRolesRequest request) {
		return ApiResponse.success(userService.assignRoles(id, request), "Roles assigned");
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete user", description = "Accessible only by ROLE_ADMIN. Cannot delete own account.")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		userService.delete(id);
		return ApiResponse.success(null, "User deleted");
	}
}
