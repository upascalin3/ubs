package com.utility.billing.report.service;

import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.billing.entity.Bill;
import com.utility.billing.billing.entity.BillStatus;
import com.utility.billing.billing.repository.BillRepository;
import com.utility.billing.payment.entity.Payment;
import com.utility.billing.payment.repository.PaymentRepository;
import com.utility.billing.report.dto.ReportSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportService {

	private final UserRepository userRepository;
	private final BillRepository billRepository;
	private final PaymentRepository paymentRepository;

	public ReportService(UserRepository userRepository, BillRepository billRepository,
			PaymentRepository paymentRepository) {
		this.userRepository = userRepository;
		this.billRepository = billRepository;
		this.paymentRepository = paymentRepository;
	}

	public ReportSummary userReport() {
		long total = userRepository.findCustomers(org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
		long active = userRepository.findCustomers(org.springframework.data.domain.Pageable.unpaged()).stream()
				.filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
		Map<String, Long> counts = new LinkedHashMap<>();
		counts.put("ACTIVE", active);
		counts.put("INACTIVE", total - active);
		return ReportSummary.builder()
				.reportType("USERS")
				.totalRecords(total)
				.countsByStatus(counts)
				.generatedAt(now())
				.build();
	}

	public ReportSummary billingReport() {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (BillStatus status : BillStatus.values()) {
			counts.put(status.name(), billRepository.findAll().stream()
					.filter(b -> b.getStatus() == status).count());
		}
		BigDecimal totalBilled = billRepository.findAll().stream()
				.map(Bill::getBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return ReportSummary.builder()
				.reportType("BILLING")
				.totalRecords(billRepository.count())
				.totalAmount(totalBilled)
				.countsByStatus(counts)
				.generatedAt(now())
				.build();
	}

	public ReportSummary paymentReport() {
		BigDecimal totalPaid = paymentRepository.findAll().stream()
				.map(Payment::getAmountPaid)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return ReportSummary.builder()
				.reportType("PAYMENT")
				.totalRecords(paymentRepository.count())
				.totalAmount(totalPaid)
				.generatedAt(now())
				.build();
	}

	public ReportSummary revenueReport() {
		return paymentReport().toBuilder().reportType("REVENUE").build();
	}

	public ReportSummary outstandingBalances() {
		BigDecimal outstanding = billRepository.findAll().stream()
				.filter(b -> EnumSet.of(BillStatus.PENDING, BillStatus.APPROVED,
						BillStatus.PARTIALLY_PAID, BillStatus.OVERDUE).contains(b.getStatus()))
				.map(Bill::getBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		long openBills = billRepository.findAll().stream()
				.filter(b -> b.getStatus() != BillStatus.PAID && b.getStatus() != BillStatus.REJECTED)
				.count();
		return ReportSummary.builder()
				.reportType("OUTSTANDING_BALANCES")
				.totalRecords(openBills)
				.totalAmount(outstanding)
				.generatedAt(now())
				.build();
	}

	private String now() {
		return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
}
