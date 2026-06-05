package com.utility.billing.payment.repository;

import com.utility.billing.billing.entity.Bill;
import com.utility.billing.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Page<Payment> findByBillId(UUID billId, Pageable pageable);

	@Query("SELECT p FROM Payment p JOIN Bill b ON p.billId = b.id WHERE b.userId = :userId")
	Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
