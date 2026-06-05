package com.utility.billing.customer.repository;
import com.utility.billing.customer.entity.Meter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface MeterRepository extends JpaRepository<Meter, UUID> {
    boolean existsByMeterNumber(String meterNumber);
    Page<Meter> findByUserId(UUID userId, Pageable pageable);
    Optional<Meter> findByMeterNumber(String meterNumber);
    Page<Meter> findByMeterNumberContainingIgnoreCase(String meterNumber, Pageable pageable);
}
