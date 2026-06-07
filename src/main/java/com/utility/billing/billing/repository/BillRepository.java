package com.utility.billing.billing.repository;

import com.utility.billing.billing.entity.Bill;
import com.utility.billing.billing.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

	Page<Bill> findByUserId(UUID userId, Pageable pageable);

	Page<Bill> findByUserIdAndStatusIn(UUID userId, Collection<BillStatus> statuses, Pageable pageable);

	Page<Bill> findByStatusIn(Collection<BillStatus> statuses, Pageable pageable);

	Page<Bill> findByStatus(BillStatus status, Pageable pageable);

	Page<Bill> findByBillNumberContainingIgnoreCaseAndStatusIn(
			String billNumber, Collection<BillStatus> statuses, Pageable pageable);

	Page<Bill> findByUserIdAndBillNumberContainingIgnoreCaseAndStatusIn(
			UUID userId, String billNumber, Collection<BillStatus> statuses, Pageable pageable);

	Optional<Bill> findByBillNumber(String billNumber);

	boolean existsByMeterIdAndBillingMonthAndBillingYear(UUID meterId, int billingMonth, int billingYear);

	Page<Bill> findByBillNumberContainingIgnoreCase(String billNumber, Pageable pageable);

	List<Bill> findByGeneratedDateGreaterThanEqual(LocalDateTime generatedDate);

	List<Bill> findByStatusInAndDueDateBefore(Collection<BillStatus> statuses, java.time.LocalDate dueDate);
}
