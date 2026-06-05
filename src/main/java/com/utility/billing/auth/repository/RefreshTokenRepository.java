package com.utility.billing.auth.repository;

import com.utility.billing.auth.entity.RefreshToken;
import com.utility.billing.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
