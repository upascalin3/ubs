package com.utility.billing.meter.repository;
import com.utility.billing.meter.entity.MeterReading;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; import java.util.UUID;
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    Page<MeterReading> findByMeterId(UUID meterId, Pageable pageable);
    boolean existsByMeterIdAndMonthAndYear(UUID meterId, int month, int year);
    Optional<MeterReading> findTopByMeterIdOrderByYearDescMonthDesc(UUID meterId);
}
