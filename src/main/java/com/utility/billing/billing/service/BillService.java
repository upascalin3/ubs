package com.utility.billing.billing.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.billing.dto.BillRequest;
import com.utility.billing.billing.dto.BillResponse;
import com.utility.billing.billing.entity.Bill;
import com.utility.billing.billing.entity.BillStatus;
import com.utility.billing.billing.entity.Tariff;
import com.utility.billing.billing.repository.BillRepository;
import com.utility.billing.billing.repository.TariffRepository;
import com.utility.billing.billing.util.BillCalculator;
import com.utility.billing.billing.util.BillingPeriodValidator;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.common.security.RoleName;
import com.utility.billing.common.security.SecurityUtils;
import com.utility.billing.customer.entity.Meter;
import com.utility.billing.customer.entity.MeterStatus;
import com.utility.billing.customer.repository.MeterRepository;
import com.utility.billing.notification.service.NotificationService;
import com.utility.billing.notification.util.UtilityNotificationMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class BillService {

	private static final Logger log = LoggerFactory.getLogger(BillService.class);
	private static final int PAYMENT_DUE_DAYS = 30;

	private static final Set<BillStatus> CUSTOMER_VISIBLE_STATUSES = EnumSet.of(
			BillStatus.APPROVED, BillStatus.PARTIALLY_PAID, BillStatus.PAID, BillStatus.OVERDUE);

	private final BillRepository billRepo;
	private final TariffRepository tariffRepo;
	private final MeterRepository meterRepo;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbc;
	private final NotificationService notificationService;
	private final BillPdfService billPdfService;

	public BillService(BillRepository billRepo, TariffRepository tariffRepo, MeterRepository meterRepo,
			UserRepository userRepository, JdbcTemplate jdbc, NotificationService notificationService,
			BillPdfService billPdfService) {
		this.billRepo = billRepo;
		this.tariffRepo = tariffRepo;
		this.meterRepo = meterRepo;
		this.userRepository = userRepository;
		this.jdbc = jdbc;
		this.notificationService = notificationService;
		this.billPdfService = billPdfService;
	}

	@Transactional
	public BillResponse generate(BillRequest req) {
		BillingPeriodValidator.validateMonthYear(req.getBillingMonth(), req.getBillingYear());
		BillingPeriodValidator.validateNotFuturePeriod(req.getBillingMonth(), req.getBillingYear());

		if (req.getConsumption() == null || req.getConsumption().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Consumption must be greater than zero");
		}

		findBillableUser(req.getUserId());
		Meter meter = meterRepo.findById(req.getMeterId())
				.orElseThrow(() -> new ResourceNotFoundException("Meter not found"));

		validateMeterForBilling(meter, req);

		if (billRepo.existsByMeterIdAndBillingMonthAndBillingYear(
				req.getMeterId(), req.getBillingMonth(), req.getBillingYear())) {
			throw new BusinessException(
					"A bill already exists for this meter in " + req.getBillingMonth() + "/" + req.getBillingYear());
		}

		LocalDate billingPeriod = LocalDate.of(req.getBillingYear(), req.getBillingMonth(), 1);
		String meterType = req.getMeterType().trim().toUpperCase();
		Tariff tariff = tariffRepo
				.findTopByMeterTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByVersionDesc(
						meterType, billingPeriod)
				.orElseThrow(() -> new BusinessException(
						"No active tariff for " + meterType + " effective on or before "
								+ billingPeriod
								+ ". Set tariff effectiveDate to " + billingPeriod + " or earlier."));

		BillCalculator.BillAmounts amounts = BillCalculator.calculate(req.getConsumption(), tariff);
		String billNumber = "BILL-" + req.getBillingYear()
				+ String.format("%02d", req.getBillingMonth()) + "-"
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		Bill bill = Bill.builder()
				.userId(req.getUserId())
				.meterId(req.getMeterId())
				.billNumber(billNumber)
				.billingMonth(req.getBillingMonth())
				.billingYear(req.getBillingYear())
				.consumption(req.getConsumption())
				.amount(amounts.subtotal())
				.taxAmount(amounts.taxAmount())
				.penalty(BigDecimal.ZERO)
				.balance(amounts.total())
				.status(BillStatus.PENDING)
				.generatedDate(LocalDateTime.now())
				.build();
		bill = billRepo.save(bill);
		sendBillGeneratedEmail(bill);
		log.info("Bill generated: {} total={} RWF", billNumber, amounts.total());
		return toResponse(bill);
	}

	private void sendBillGeneratedEmail(Bill bill) {
		User user = findUser(bill.getUserId());
		String body = UtilityNotificationMessages.billGenerated(
				user.getFullName(), bill.getBillingMonth(), bill.getBillingYear(), bill.getBalance());
		notificationService.sendEmailToUser(bill.getUserId(), "Utility Bill Processed", body);
	}

	private void validateMeterForBilling(Meter meter, BillRequest req) {
		if (!meter.getUserId().equals(req.getUserId())) {
			throw new BusinessException("Meter does not belong to the specified user");
		}
		if (meter.getStatus() != MeterStatus.ACTIVE) {
			throw new BusinessException("Inactive meter cannot be billed");
		}
		if (!meter.getMeterType().name().equalsIgnoreCase(req.getMeterType().trim())) {
			throw new BusinessException(
					"Meter type mismatch: meter is " + meter.getMeterType().name()
							+ " but request specified " + req.getMeterType());
		}
		BillingPeriodValidator.validateNotBeforeInstallation(
				meter.getInstallationDate(), req.getBillingMonth(), req.getBillingYear());
	}

	@Transactional
	public BillResponse approve(UUID id) {
		Bill bill = billRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
		if (bill.getStatus() != BillStatus.PENDING) {
			throw new BusinessException("Only PENDING bills can be approved");
		}
		bill.setStatus(BillStatus.APPROVED);
		bill.setApprovedBy(SecurityUtils.getCurrentUserId());
		bill.setDueDate(LocalDate.now().plusDays(PAYMENT_DUE_DAYS));
		notifyBillApproved(bill);
		log.info("Bill {} approved", bill.getBillNumber());
		return toResponse(billRepo.save(bill));
	}

	@Transactional
	public BillResponse reject(UUID id) {
		Bill bill = billRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
		if (bill.getStatus() != BillStatus.PENDING) {
			throw new BusinessException("Only PENDING bills can be rejected");
		}
		bill.setStatus(BillStatus.REJECTED);
		notifyBillRejected(bill);
		log.info("Bill {} rejected", bill.getBillNumber());
		return toResponse(billRepo.save(bill));
	}

	public Page<BillResponse> listPending(Pageable pageable) {
		return billRepo.findByStatus(BillStatus.PENDING, pageable).map(this::toResponse);
	}

	@Transactional
	public void generateMonthlyBills() {
		LocalDateTime startedAt = LocalDateTime.now().minusSeconds(1);
		jdbc.execute("CALL billing.generate_monthly_bills()");
		billRepo.findByGeneratedDateGreaterThanEqual(startedAt).forEach(this::sendBillGeneratedEmail);
		log.info("Monthly bills generated via stored procedure");
	}

	@Transactional
	public void applyOverduePenalties() {
		jdbc.execute("CALL billing.apply_overdue_penalties()");
		log.info("Overdue penalties applied via stored procedure");
	}

	public Page<BillResponse> list(Pageable pageable) {
		if (SecurityUtils.hasRole(RoleName.CUSTOMER)) {
			return billRepo.findByUserIdAndStatusIn(
					SecurityUtils.requireCurrentUserId(), CUSTOMER_VISIBLE_STATUSES, pageable)
					.map(this::toResponse);
		}
		return billRepo.findAll(pageable).map(this::toResponse);
	}

	public Page<BillResponse> byUser(UUID userId, Pageable pageable) {
		UUID scopedUserId = SecurityUtils.resolveUserScope(userId);
		if (SecurityUtils.hasRole(RoleName.CUSTOMER)) {
			return billRepo.findByUserIdAndStatusIn(scopedUserId, CUSTOMER_VISIBLE_STATUSES, pageable)
					.map(this::toResponse);
		}
		return billRepo.findByUserId(scopedUserId, pageable).map(this::toResponse);
	}

	public Page<BillResponse> search(String billNumber, Pageable pageable) {
		if (SecurityUtils.hasRole(RoleName.CUSTOMER)) {
			return billRepo.findByUserIdAndBillNumberContainingIgnoreCaseAndStatusIn(
					SecurityUtils.requireCurrentUserId(), billNumber, CUSTOMER_VISIBLE_STATUSES, pageable)
					.map(this::toResponse);
		}
		return billRepo.findByBillNumberContainingIgnoreCase(billNumber, pageable).map(this::toResponse);
	}

	public BillResponse get(UUID id) {
		Bill bill = billRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
		SecurityUtils.assertCustomerOwns(bill.getUserId());
		if (SecurityUtils.hasRole(RoleName.CUSTOMER) && !CUSTOMER_VISIBLE_STATUSES.contains(bill.getStatus())) {
			throw new BusinessException("Bill is not yet available for viewing");
		}
		return toResponse(bill);
	}

	private User findBillableUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException("Inactive user cannot be billed");
		}
		return user;
	}

	private User findUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private void notifyBillApproved(Bill bill) {
		User user = findUser(bill.getUserId());
		notificationService.create(
				bill.getUserId(),
				"Bill Approved",
				"Your utility bill " + bill.getBillNumber()
						+ " has been approved and is ready for payment. Balance: "
						+ bill.getBalance() + " RWF.",
				billPdfService.generate(bill, user),
				billPdfName(bill));
	}

	private void notifyBillRejected(Bill bill) {
		notificationService.create(
				bill.getUserId(),
				"Bill Rejected",
				"Your utility bill " + bill.getBillNumber()
						+ " was rejected and will not be payable in its current state.");
	}

	private String billPdfName(Bill bill) {
		return bill.getBillNumber() + ".pdf";
	}

	private BillResponse toResponse(Bill bill) {
		return BillResponse.builder()
				.id(bill.getId())
				.userId(bill.getUserId())
				.meterId(bill.getMeterId())
				.billNumber(bill.getBillNumber())
				.billingMonth(bill.getBillingMonth())
				.billingYear(bill.getBillingYear())
				.consumption(bill.getConsumption())
				.amount(bill.getAmount())
				.taxAmount(bill.getTaxAmount())
				.penalty(bill.getPenalty())
				.balance(bill.getBalance())
				.status(bill.getStatus().name())
				.generatedDate(bill.getGeneratedDate())
				.build();
	}
}
