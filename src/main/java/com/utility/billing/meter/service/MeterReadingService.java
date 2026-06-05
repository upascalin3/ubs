package com.utility.billing.meter.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.customer.entity.Meter;
import com.utility.billing.customer.entity.MeterStatus;
import com.utility.billing.customer.repository.MeterRepository;
import com.utility.billing.meter.dto.ReadingRequest;
import com.utility.billing.meter.dto.ReadingResponse;
import com.utility.billing.meter.entity.MeterReading;
import com.utility.billing.meter.repository.MeterReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class MeterReadingService {

	private static final Logger log = LoggerFactory.getLogger(MeterReadingService.class);

	private final MeterReadingRepository readingRepo;
	private final MeterRepository meterRepo;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbc;

	public MeterReadingService(MeterReadingRepository readingRepo, MeterRepository meterRepo,
			UserRepository userRepository, JdbcTemplate jdbc) {
		this.readingRepo = readingRepo;
		this.meterRepo = meterRepo;
		this.userRepository = userRepository;
		this.jdbc = jdbc;
	}

	@Transactional
	public ReadingResponse capture(ReadingRequest req) {
		if (req.getCurrentReading().compareTo(req.getPreviousReading()) <= 0) {
			throw new BusinessException("Current reading must be greater than previous reading");
		}
		if (req.getReadingDate().isAfter(LocalDate.now())) {
			throw new BusinessException("Reading date cannot be in the future");
		}
		if (readingRepo.existsByMeterIdAndMonthAndYear(req.getMeterId(), req.getMonth(), req.getYear())) {
			throw new BusinessException("One reading per meter per month is allowed");
		}

		Meter meter = meterRepo.findById(req.getMeterId())
				.orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
		if (meter.getStatus() != MeterStatus.ACTIVE) {
			throw new BusinessException("Inactive meter cannot receive readings");
		}
		User user = userRepository.findById(meter.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException("Inactive user cannot be billed");
		}

		MeterReading reading = MeterReading.builder()
				.meterId(req.getMeterId())
				.previousReading(req.getPreviousReading())
				.currentReading(req.getCurrentReading())
				.readingDate(req.getReadingDate())
				.month(req.getMonth())
				.year(req.getYear())
				.build();
		reading = readingRepo.save(reading);

		BigDecimal consumption = req.getCurrentReading().subtract(req.getPreviousReading());
		jdbc.update("""
				INSERT INTO billing.pending_bill_generation
				(user_id, meter_id, meter_type, consumption, billing_month, billing_year)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				meter.getUserId(), meter.getId(), meter.getMeterType().name(),
				consumption, req.getMonth(), req.getYear());

		log.info("Reading captured for meter {} month {}/{}", req.getMeterId(), req.getMonth(), req.getYear());
		return toResponse(reading);
	}

	public Page<ReadingResponse> history(UUID meterId, Pageable pageable) {
		return readingRepo.findByMeterId(meterId, pageable).map(this::toResponse);
	}

	public Page<ReadingResponse> list(Pageable pageable) {
		return readingRepo.findAll(pageable).map(this::toResponse);
	}

	private ReadingResponse toResponse(MeterReading reading) {
		return ReadingResponse.builder()
				.id(reading.getId())
				.meterId(reading.getMeterId())
				.previousReading(reading.getPreviousReading())
				.currentReading(reading.getCurrentReading())
				.consumption(reading.getCurrentReading().subtract(reading.getPreviousReading()))
				.readingDate(reading.getReadingDate())
				.month(reading.getMonth())
				.year(reading.getYear())
				.createdAt(reading.getCreatedAt())
				.build();
	}
}
