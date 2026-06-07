package com.utility.billing.customer.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.billing.util.BillingPeriodValidator;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.customer.dto.MeterRequest;
import com.utility.billing.customer.dto.MeterResponse;
import com.utility.billing.customer.entity.Meter;
import com.utility.billing.customer.entity.MeterStatus;
import com.utility.billing.customer.entity.MeterType;
import com.utility.billing.customer.repository.MeterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MeterService {

	private final MeterRepository meterRepo;
	private final UserRepository userRepository;

	public MeterService(MeterRepository meterRepo, UserRepository userRepository) {
		this.meterRepo = meterRepo;
		this.userRepository = userRepository;
	}

	@Transactional
	public MeterResponse create(MeterRequest req) {
		User user = userRepository.findById(req.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException("Meters can only be assigned to ACTIVE users");
		}
		if (meterRepo.existsByMeterNumber(req.getMeterNumber())) {
			throw new BusinessException("Meter number already exists");
		}
		BillingPeriodValidator.validateInstallationDate(req.getInstallationDate());
		MeterType meterType = parseMeterType(req.getMeterType());
		MeterStatus meterStatus = parseMeterStatus(req.getStatus());
		Meter meter = Meter.builder()
				.userId(req.getUserId())
				.meterNumber(req.getMeterNumber())
				.meterType(meterType)
				.installationDate(req.getInstallationDate())
				.status(meterStatus)
				.build();
		return toResponse(meterRepo.save(meter));
	}

	@Transactional
	public MeterResponse update(UUID id, MeterRequest req) {
		Meter meter = meterRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
		meter.setStatus(parseMeterStatus(req.getStatus()));
		return toResponse(meterRepo.save(meter));
	}

	private MeterType parseMeterType(String meterType) {
		try {
			return MeterType.valueOf(meterType.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException("Meter type must be WATER or ELECTRICITY");
		}
	}

	private MeterStatus parseMeterStatus(String status) {
		try {
			return MeterStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException("Meter status must be ACTIVE or INACTIVE");
		}
	}

	public MeterResponse get(UUID id) {
		return toResponse(meterRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Meter not found")));
	}

	public Page<MeterResponse> list(Pageable pageable) {
		return meterRepo.findAll(pageable).map(this::toResponse);
	}

	public Page<MeterResponse> byUser(UUID userId, Pageable pageable) {
		return meterRepo.findByUserId(userId, pageable).map(this::toResponse);
	}

	public Page<MeterResponse> search(String meterNumber, Pageable pageable) {
		return meterRepo.findByMeterNumberContainingIgnoreCase(meterNumber, pageable).map(this::toResponse);
	}

	private MeterResponse toResponse(Meter meter) {
		return MeterResponse.builder()
				.id(meter.getId())
				.userId(meter.getUserId())
				.meterNumber(meter.getMeterNumber())
				.meterType(meter.getMeterType().name())
				.installationDate(meter.getInstallationDate())
				.status(meter.getStatus().name())
				.createdAt(meter.getCreatedAt())
				.build();
	}
}
