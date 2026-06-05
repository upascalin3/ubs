package com.utility.billing.auth.service;

import com.utility.billing.auth.dto.*;
import com.utility.billing.auth.entity.*;
import com.utility.billing.auth.repository.*;
import com.utility.billing.common.exception.BusinessException;
import com.utility.billing.common.exception.ResourceNotFoundException;
import com.utility.billing.common.exception.UnauthorizedException;
import com.utility.billing.common.security.JwtProperties;
import com.utility.billing.common.security.JwtTokenProvider;
import com.utility.billing.common.security.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TokenBlacklistRepository tokenBlacklistRepository,
                       OtpService otpService, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already exists");
        }
        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("National ID already exists");
        }
        Role customerRole = roleRepository.findByRoleName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.INACTIVE)
                .emailVerified(false)
                .mustChangePassword(false)
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(user);
        otpService.generateAndSend(user, OtpPurpose.REGISTRATION);
        log.info("User registered: {}", user.getEmail());
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verify(user, request.getOtpCode(), OtpPurpose.REGISTRATION);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Email verified for: {}", user.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active. Please verify your email.");
        }

        log.info("User logged in: {}", user.getEmail());
        AuthResponse response = buildAuthResponse(user);
        response.setMustChangePassword(user.isMustChangePassword());
        return response;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return buildAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenBlacklistRepository.save(TokenBlacklist.builder()
                    .token(accessToken)
                    .expiryDate(LocalDateTime.now().plusDays(1))
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        if (refreshToken != null) {
            refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }
        log.info("User logged out");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.generateAndSend(user, OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void verifyResetOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verify(user, request.getOtpCode(), OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getRoleName).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .build();
    }
}
