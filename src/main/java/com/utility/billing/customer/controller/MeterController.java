package com.utility.billing.customer.controller;

import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.customer.dto.MeterRequest;
import com.utility.billing.customer.dto.MeterResponse;
import com.utility.billing.customer.service.MeterService;
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
@RequestMapping("/api/meters")
@Tag(name = "Meters", description = "Meter management — assigned to users")
public class MeterController {

	private final MeterService service;

	public MeterController(MeterService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Assign meter to user", description = "ROLE_ADMIN — body uses userId (not customerId)")
	public ApiResponse<MeterResponse> create(@Valid @RequestBody MeterRequest req) {
		return ApiResponse.success(service.create(req));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update Meter", description = "Activate/deactivate or update meter — ROLE_ADMIN")
	public ApiResponse<MeterResponse> update(@PathVariable UUID id, @Valid @RequestBody MeterRequest req) {
		return ApiResponse.success(service.update(id, req));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "Get Meter")
	public ApiResponse<MeterResponse> get(@PathVariable UUID id) {
		return ApiResponse.success(service.get(id));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "List Meters")
	public ApiResponse<Page<MeterResponse>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.list(pageable));
	}

	@GetMapping("/search")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "Search Meters")
	public ApiResponse<Page<MeterResponse>> search(@RequestParam String meterNumber,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.search(meterNumber, pageable));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','FINANCE')")
	@Operation(summary = "Meters by user", description = "GET /api/meters/user/{userId}")
	public ApiResponse<Page<MeterResponse>> byUser(@PathVariable UUID userId,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.byUser(userId, pageable));
	}
}
