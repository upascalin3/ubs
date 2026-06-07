package com.utility.billing.billing.service;

import com.utility.billing.billing.dto.TariffRequest;
import com.utility.billing.billing.entity.Tariff;
import com.utility.billing.billing.repository.TariffRepository;
import com.utility.billing.billing.util.BillingPeriodValidator;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.customer.entity.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TariffService {

	private static final int MAX_FUTURE_YEARS = 1;

	private final TariffRepository repo;

	public TariffService(TariffRepository repo) {
		this.repo = repo;
	}

	@Transactional
	public Tariff create(TariffRequest req) {
		String meterType = req.getMeterType().trim().toUpperCase();
		validateMeterType(meterType);

		if (repo.existsByMeterTypeAndVersion(meterType, req.getVersion())) {
			throw new BusinessException("Tariff version must be unique per meter type");
		}

		LocalDate effectiveDate = req.getEffectiveDate();
		LocalDate minDate = LocalDate.of(BillingPeriodValidator.MIN_YEAR, 1, 1);
		if (effectiveDate.isBefore(minDate)) {
			throw new BusinessException("Effective date cannot be before " + minDate);
		}
		if (effectiveDate.isAfter(LocalDate.now().plusYears(MAX_FUTURE_YEARS))) {
			throw new BusinessException("Effective date cannot be more than " + MAX_FUTURE_YEARS + " year(s) in the future");
		}

		Tariff tariff = Tariff.builder()
				.meterType(meterType)
				.tariffName(req.getTariffName())
				.rate(req.getRate())
				.fixedCharge(req.getFixedCharge())
				.vat(req.getVat())
				.penaltyRate(req.getPenaltyRate())
				.version(req.getVersion())
				.effectiveDate(effectiveDate)
				.active(req.isActive())
				.build();
		return repo.save(tariff);
	}

	public Page<Tariff> list(Pageable pageable) {
		return repo.findAll(pageable);
	}

	public Tariff get(UUID id) {
		return repo.findById(id).orElseThrow(() -> new BusinessException("Tariff not found"));
	}

	private void validateMeterType(String meterType) {
		try {
			MeterType.valueOf(meterType);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException("Meter type must be WATER or ELECTRICITY");
		}
	}
}
