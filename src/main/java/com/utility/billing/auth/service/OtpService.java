package com.utility.billing.auth.service;

import com.utility.billing.auth.entity.Otp;
import com.utility.billing.auth.entity.OtpPurpose;
import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.repository.OtpRepository;
import com.utility.billing.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        Otp otp = Otp.builder()
                .user(user)
                .otpCode(code)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .purpose(purpose)
                .build();
        otpRepository.save(otp);
        emailService.sendOtp(user.getEmail(), code, purpose.name());
    }

    @Transactional
    public void verify(User user, String code, OtpPurpose purpose) {
        Otp otp = otpRepository.findTopByUserAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> new BusinessException("No OTP found for this user"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired");
        }
        if (!otp.getOtpCode().equals(code)) {
            throw new BusinessException("Invalid OTP code");
        }
        otp.setVerified(true);
        otpRepository.save(otp);
    }
}
