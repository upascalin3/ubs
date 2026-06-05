package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Authentication response with tokens")
public class AuthResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private List<String> roles;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private boolean mustChangePassword;
}
