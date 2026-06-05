package com.utility.billing.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Schema(description = "Aggregated report summary response")
public class ReportSummary {

	@Schema(example = "REVENUE")
	private String reportType;
	@Schema(example = "42")
	private long totalRecords;
	@Schema(example = "125000.00")
	private BigDecimal totalAmount;
	@Schema(description = "Record counts grouped by status")
	private Map<String, Long> countsByStatus;
	@Schema(description = "Report generation timestamp", example = "2026-06-05T10:15:30")
	private String generatedAt;
}
