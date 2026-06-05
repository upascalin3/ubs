package com.utility.billing.customer.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.entity.UserStatus;
import com.utility.billing.auth.repository.UserRepository;
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

import java.time.LocalDate;
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
		if (req.getInstallationDate().isAfter(LocalDate.now())) {
			throw new BusinessException("Installation date cannot be in the future");
		}
		Meter meter = Meter.builder()
				.userId(req.getUserId())
				.meterNumber(req.getMeterNumber())
				.meterType(MeterType.valueOf(req.getMeterType()))
				.installationDate(req.getInstallationDate())
				.status(MeterStatus.valueOf(req.getStatus()))
				.build();
		return toResponse(meterRepo.save(meter));
	}

	@Transactional
	public MeterResponse update(UUID id, MeterRequest req) {
		Meter meter = meterRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
		meter.setStatus(MeterStatus.valueOf(req.getStatus()));
		return toResponse(meterRepo.save(meter));
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
