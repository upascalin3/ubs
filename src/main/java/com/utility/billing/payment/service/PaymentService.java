package com.utility.billing.payment.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.billing.entity.Bill;
import com.utility.billing.billing.entity.BillStatus;
import com.utility.billing.billing.repository.BillRepository;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.common.security.RoleName;
import com.utility.billing.common.security.SecurityUtils;
import com.utility.billing.notification.service.NotificationService;
import com.utility.billing.notification.util.UtilityNotificationMessages;
import com.utility.billing.payment.dto.PaymentRequest;
import com.utility.billing.payment.dto.PaymentResponse;
import com.utility.billing.payment.entity.Payment;
import com.utility.billing.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository paymentRepo;
	private final BillRepository billRepo;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final EntityManager entityManager;

	public PaymentService(PaymentRepository paymentRepo, BillRepository billRepo,
			UserRepository userRepository, NotificationService notificationService,
			EntityManager entityManager) {
		this.paymentRepo = paymentRepo;
		this.billRepo = billRepo;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.entityManager = entityManager;
	}

	@Transactional
	public PaymentResponse record(PaymentRequest req) {
		if (req.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Payment amount must be greater than zero");
		}
		if (req.getPaymentDate().isAfter(LocalDate.now())) {
			throw new BusinessException("Payment date cannot be in the future");
		}

		Bill bill = billRepo.findById(req.getBillId())
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));

		if (!EnumSet.of(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID, BillStatus.OVERDUE)
				.contains(bill.getStatus())) {
			throw new BusinessException(
					"Payments can only be recorded against APPROVED, PARTIALLY_PAID, or OVERDUE bills");
		}
		if (req.getAmountPaid().compareTo(bill.getBalance()) > 0) {
			throw new BusinessException("Payment cannot exceed outstanding balance");
		}

		BigDecimal billTotal = bill.getAmount().add(bill.getTaxAmount()).add(bill.getPenalty());
		UUID userId = bill.getUserId();
		int billingMonth = bill.getBillingMonth();
		int billingYear = bill.getBillingYear();

		Payment payment = Payment.builder()
				.billId(req.getBillId())
				.amountPaid(req.getAmountPaid())
				.paymentMethod(req.getPaymentMethod())
				.paymentDate(req.getPaymentDate())
				.referenceNumber(req.getReferenceNumber())
				.build();
		payment = paymentRepo.save(payment);
		entityManager.flush();
		entityManager.clear();

		Bill updatedBill = billRepo.findById(req.getBillId())
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));

		if (updatedBill.getStatus() == BillStatus.PAID) {
			scheduleFullPaymentEmail(userId, billingMonth, billingYear, billTotal);
		} else if (updatedBill.getStatus() == BillStatus.PARTIALLY_PAID) {
			schedulePartialPaymentEmail(userId, billingMonth, billingYear,
					payment.getAmountPaid(), updatedBill.getBalance());
		}

		log.info("Payment recorded: {} for bill {} — outstanding={} status={}",
				req.getReferenceNumber(), updatedBill.getBillNumber(),
				updatedBill.getBalance(), updatedBill.getStatus());

		return PaymentResponse.builder()
				.id(payment.getId())
				.billId(payment.getBillId())
				.userId(updatedBill.getUserId())
				.amountPaid(payment.getAmountPaid())
				.paymentMethod(payment.getPaymentMethod())
				.paymentDate(payment.getPaymentDate())
				.referenceNumber(payment.getReferenceNumber())
				.remainingBalance(updatedBill.getBalance())
				.billStatus(updatedBill.getStatus().name())
				.createdAt(payment.getCreatedAt())
				.build();
	}

	private void scheduleFullPaymentEmail(UUID userId, int billingMonth, int billingYear, BigDecimal billTotal) {
		schedulePaymentEmail(userId, () -> {
			User user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				log.warn("Full payment email skipped; user {} not found", userId);
				return;
			}
			String body = UtilityNotificationMessages.fullPaymentProcessed(
					user.getFullName(), billingMonth, billingYear, billTotal);
			notificationService.sendEmailToUser(userId, "Payment Received", body);
			log.info("Full payment email sent to {}", user.getEmail());
		});
	}

	private void schedulePartialPaymentEmail(UUID userId, int billingMonth, int billingYear,
			BigDecimal amountPaid, BigDecimal remainingBalance) {
		schedulePaymentEmail(userId, () -> {
			User user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				log.warn("Partial payment email skipped; user {} not found", userId);
				return;
			}
			String body = UtilityNotificationMessages.partialPaymentReceived(
					user.getFullName(), billingMonth, billingYear, amountPaid, remainingBalance);
			notificationService.sendEmailToUser(userId, "Partial Payment Received", body);
			log.info("Partial payment email sent to {} — remaining={} FRW", user.getEmail(), remainingBalance);
		});
	}

	private void schedulePaymentEmail(UUID userId, Runnable sendAction) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sendAction.run();
			}
		});
	}

	public Page<PaymentResponse> list(Pageable pageable) {
		if (SecurityUtils.hasRole(RoleName.CUSTOMER)) {
			return paymentRepo.findByUserId(SecurityUtils.requireCurrentUserId(), pageable).map(this::toResponse);
		}
		return paymentRepo.findAll(pageable).map(this::toResponse);
	}

	public Page<PaymentResponse> byBill(UUID billId, Pageable pageable) {
		Bill bill = billRepo.findById(billId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
		SecurityUtils.assertCustomerOwns(bill.getUserId());
		return paymentRepo.findByBillId(billId, pageable).map(this::toResponse);
	}

	public Page<PaymentResponse> byUser(UUID userId, Pageable pageable) {
		UUID scopedUserId = SecurityUtils.resolveUserScope(userId);
		return paymentRepo.findByUserId(scopedUserId, pageable).map(this::toResponse);
	}

	private PaymentResponse toResponse(Payment payment) {
		Bill bill = billRepo.findById(payment.getBillId()).orElse(null);
		BigDecimal balance = bill != null ? bill.getBalance() : BigDecimal.ZERO;
		String status = bill != null ? bill.getStatus().name() : "UNKNOWN";
		UUID userId = bill != null ? bill.getUserId() : null;
		return PaymentResponse.builder()
				.id(payment.getId())
				.billId(payment.getBillId())
				.userId(userId)
				.amountPaid(payment.getAmountPaid())
				.paymentMethod(payment.getPaymentMethod())
				.paymentDate(payment.getPaymentDate())
				.referenceNumber(payment.getReferenceNumber())
				.remainingBalance(balance)
				.billStatus(status)
				.createdAt(payment.getCreatedAt())
				.build();
	}
}
