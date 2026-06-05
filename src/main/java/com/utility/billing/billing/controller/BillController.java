package com.utility.billing.billing.controller;

import com.utility.billing.billing.dto.BillRequest;
import com.utility.billing.billing.dto.BillResponse;
import com.utility.billing.billing.service.BillService;
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
@RequestMapping("/api/bills")
@Tag(name = "Bills", description = "Bill generation, approval, and management")
public class BillController {

	private final BillService service;

	public BillController(BillService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Generate bill", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<BillResponse> generate(@Valid @RequestBody BillRequest req) {
		return ApiResponse.success(service.generate(req));
	}

	@PostMapping("/generate")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Generate bill (alias)", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE. Alias for POST /api/bills")
	public ApiResponse<BillResponse> generateAlias(@Valid @RequestBody BillRequest req) {
		return generate(req);
	}

	@PutMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Approve bill", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<BillResponse> approvePut(@PathVariable UUID id) {
		return ApiResponse.success(service.approve(id), "Bill approved");
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Approve bill (POST)", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<BillResponse> approvePost(@PathVariable UUID id) {
		return approvePut(id);
	}

	@PutMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Reject bill", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<BillResponse> reject(@PathVariable UUID id) {
		return ApiResponse.success(service.reject(id), "Bill rejected");
	}

	@PostMapping("/generate-monthly")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Generate monthly bills", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE. Calls stored procedure.")
	public ApiResponse<Void> generateMonthly() {
		service.generateMonthlyBills();
		return ApiResponse.success(null, "Monthly bills generated");
	}

	@GetMapping("/pending")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "List pending bills", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<Page<BillResponse>> listPending(
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.listPending(pageable));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "List bills", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE, ROLE_CUSTOMER")
	public ApiResponse<Page<BillResponse>> list(
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.list(pageable));
	}

	@GetMapping("/search")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "Search bills", description = "Search by bill number")
	public ApiResponse<Page<BillResponse>> search(@RequestParam String billNumber,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.search(billNumber, pageable));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "Bills by user", description = "GET /api/bills/user/{userId} — CUSTOMER sees approved+ only")
	public ApiResponse<Page<BillResponse>> byUser(@PathVariable UUID userId,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.byUser(userId, pageable));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "Get bill by ID", description = "View bill details and outstanding balance")
	public ApiResponse<BillResponse> get(@PathVariable UUID id) {
		return ApiResponse.success(service.get(id));
	}
}
