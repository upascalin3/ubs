package com.utility.billing.payment.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.billing.entity.Bill;
import com.utility.billing.billing.entity.BillStatus;
import com.utility.billing.billing.repository.BillRepository;
import com.utility.billing.billing.service.BillPdfService;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.payment.dto.PaymentRequest;
import com.utility.billing.payment.dto.PaymentResponse;
import com.utility.billing.payment.entity.Payment;
import com.utility.billing.payment.repository.PaymentRepository;
import com.utility.billing.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository paymentRepo;
	private final BillRepository billRepo;
	private final NotificationService notificationService;
	private final UserRepository userRepository;
	private final BillPdfService billPdfService;

	public PaymentService(PaymentRepository paymentRepo, BillRepository billRepo,
			NotificationService notificationService, UserRepository userRepository,
			BillPdfService billPdfService) {
		this.paymentRepo = paymentRepo;
		this.billRepo = billRepo;
		this.notificationService = notificationService;
		this.userRepository = userRepository;
		this.billPdfService = billPdfService;
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

		if (!EnumSet.of(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID).contains(bill.getStatus())) {
			throw new BusinessException("Payments can only be recorded against APPROVED or PARTIALLY_PAID bills");
		}
		if (req.getAmountPaid().compareTo(bill.getBalance()) > 0) {
			throw new BusinessException("Payment cannot exceed outstanding balance");
		}
		BigDecimal remainingBalance = bill.getBalance().subtract(req.getAmountPaid());
		if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
			remainingBalance = BigDecimal.ZERO;
		}
		BillStatus paymentStatus = remainingBalance.compareTo(BigDecimal.ZERO) == 0
				? BillStatus.PAID
				: BillStatus.PARTIALLY_PAID;

		Payment payment = Payment.builder()
				.billId(req.getBillId())
				.amountPaid(req.getAmountPaid())
				.paymentMethod(req.getPaymentMethod())
				.paymentDate(req.getPaymentDate())
				.referenceNumber(req.getReferenceNumber())
				.build();
		payment = paymentRepo.save(payment);
		bill.setBalance(remainingBalance);
		bill.setStatus(paymentStatus);
		billRepo.save(bill);
		if (paymentStatus == BillStatus.PAID) {
			notifyFullPaymentRecorded(payment, bill);
		}

		log.info("Payment recorded: {} for bill {} — outstanding={} status={}",
				req.getReferenceNumber(), bill.getBillNumber(), remainingBalance, paymentStatus);

		return PaymentResponse.builder()
				.id(payment.getId())
				.billId(payment.getBillId())
				.userId(bill.getUserId())
				.amountPaid(payment.getAmountPaid())
				.paymentMethod(payment.getPaymentMethod())
				.paymentDate(payment.getPaymentDate())
				.referenceNumber(payment.getReferenceNumber())
				.remainingBalance(remainingBalance)
				.billStatus(paymentStatus.name())
				.createdAt(payment.getCreatedAt())
				.build();
	}

	private void notifyFullPaymentRecorded(Payment payment, Bill bill) {
		notificationService.create(
				bill.getUserId(),
				"Payment Received",
				"Your payment of " + payment.getAmountPaid() + " RWF for bill "
						+ bill.getBillNumber() + " has been received. Reference: "
						+ payment.getReferenceNumber() + ". Your utility bill is fully paid.",
				billPdfService.generate(bill, findUser(bill.getUserId())),
				bill.getBillNumber() + ".pdf");
	}

	private User findUser(UUID userId) {
		return userRepository.findById(userId).orElseThrow();
	}

	public Page<PaymentResponse> list(Pageable pageable) {
		return paymentRepo.findAll(pageable).map(this::toResponse);
	}

	public Page<PaymentResponse> byBill(UUID billId, Pageable pageable) {
		return paymentRepo.findByBillId(billId, pageable).map(this::toResponse);
	}

	public Page<PaymentResponse> byUser(UUID userId, Pageable pageable) {
		return paymentRepo.findByUserId(userId, pageable).map(this::toResponse);
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
