package com.utility.billing.report.controller;

import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.report.dto.ReportSummary;
import com.utility.billing.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "User, billing, payment, and revenue reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Account holder report", description = "GET /api/reports/users — ROLE_CUSTOMER count")
	public ApiResponse<ReportSummary> users() {
		return ApiResponse.success(reportService.userReport());
	}

	@GetMapping("/billing")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Billing report", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<ReportSummary> billing() {
		return ApiResponse.success(reportService.billingReport());
	}

	@GetMapping("/payments")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Payment report", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<ReportSummary> payments() {
		return ApiResponse.success(reportService.paymentReport());
	}

	@GetMapping("/revenue")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Revenue report", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<ReportSummary> revenue() {
		return ApiResponse.success(reportService.revenueReport());
	}

	@GetMapping("/outstanding-balances")
	@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
	@Operation(summary = "Outstanding balances", description = "Required roles: ROLE_ADMIN, ROLE_FINANCE")
	public ApiResponse<ReportSummary> outstandingBalances() {
		return ApiResponse.success(reportService.outstandingBalances());
	}
}
