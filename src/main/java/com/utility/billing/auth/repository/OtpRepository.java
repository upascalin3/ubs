package com.utility.billing.auth.repository;

import com.utility.billing.auth.entity.Otp;
import com.utility.billing.auth.entity.OtpPurpose;
import com.utility.billing.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findTopByUserAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(User user, OtpPurpose purpose);
}
