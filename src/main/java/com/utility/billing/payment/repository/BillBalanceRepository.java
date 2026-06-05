package com.utility.billing.payment.repository;
import com.utility.billing.payment.entity.BillBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface BillBalanceRepository extends JpaRepository<BillBalance, UUID> {}
