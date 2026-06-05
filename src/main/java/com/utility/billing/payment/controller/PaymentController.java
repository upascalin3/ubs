package com.utility.billing.payment.controller;

import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.payment.dto.PaymentRequest;
import com.utility.billing.payment.dto.PaymentResponse;
import com.utility.billing.payment.service.PaymentService;
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
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {

	private final PaymentService service;

	public PaymentController(PaymentService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('FINANCE')")
	@Operation(summary = "Record payment", description = "ROLE_FINANCE — request body: billId only (no userId)")
	public ApiResponse<PaymentResponse> record(@Valid @RequestBody PaymentRequest req) {
		return ApiResponse.success(service.record(req));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "List Payments")
	public ApiResponse<Page<PaymentResponse>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.list(pageable));
	}

	@GetMapping("/bill/{billId}")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "Payments by bill")
	public ApiResponse<Page<PaymentResponse>> byBill(@PathVariable UUID billId,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.byBill(billId, pageable));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE','CUSTOMER')")
	@Operation(summary = "Payments by user", description = "GET /api/payments/user/{userId}")
	public ApiResponse<Page<PaymentResponse>> byUser(@PathVariable UUID userId,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.byUser(userId, pageable));
	}
}
