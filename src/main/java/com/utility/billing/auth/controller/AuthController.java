package com.utility.billing.auth.controller;

import com.utility.billing.auth.dto.*;
import com.utility.billing.auth.service.AuthService;
import com.utility.billing.common.documentation.ApiRoleDoc;
import com.utility.billing.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Customer signup (ROLE_CUSTOMER), login, OTP, password reset")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register customer", description = "PUBLIC — creates User (ROLE_CUSTOMER, INACTIVE). Requires nationalId. Sends OTP.")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null, "Registration successful. Please verify OTP sent to your email.");
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up (alias)", description = "PUBLIC — alias for POST /api/auth/register")
    public ApiResponse<Void> signup(@Valid @RequestBody RegisterRequest request) {
        return register(request);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify registration OTP", description = "Public endpoint. Activates account after OTP verification.")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyOtp(request);
        return ApiResponse.success(null, "Email verified successfully. You can now login.");
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Public endpoint. Returns JWT access token and refresh token.")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Public endpoint. Exchange refresh token for new tokens.")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request), "Token refreshed");
    }

    @PostMapping("/logout")
    @ApiRoleDoc(value = {"AUTHENTICATED"}, description = "Requires valid JWT access token")
    @Operation(summary = "Logout", description = "Authenticated endpoint. Blacklists JWT and revokes refresh token.")
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    @RequestBody(required = false) RefreshTokenRequest body) {
        String bearer = request.getHeader("Authorization");
        String accessToken = bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : null;
        String refreshToken = body != null ? body.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.success(null, "Logout successful");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Public endpoint. Sends OTP to email for password reset.")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(null, "OTP sent to your email");
    }

    @PostMapping("/verify-reset-otp")
    @Operation(summary = "Verify reset OTP", description = "Public endpoint. Verifies OTP before password reset.")
    public ApiResponse<Void> verifyResetOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyResetOtp(request);
        return ApiResponse.success(null, "OTP verified. You can now reset your password.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Public endpoint. Resets password after OTP verification.")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password reset successful");
    }
}
