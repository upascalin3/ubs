package com.utility.billing.billing.repository;

import com.utility.billing.billing.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {

	boolean existsByMeterTypeAndVersion(String meterType, int version);

	Optional<Tariff> findTopByMeterTypeAndActiveTrueOrderByVersionDesc(String meterType);

	Optional<Tariff> findTopByMeterTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByVersionDesc(
			String meterType, LocalDate billingPeriodStart);
}
