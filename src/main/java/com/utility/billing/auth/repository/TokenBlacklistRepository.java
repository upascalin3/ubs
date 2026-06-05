package com.utility.billing.auth.repository;

import com.utility.billing.auth.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    boolean existsByToken(String token);
}
