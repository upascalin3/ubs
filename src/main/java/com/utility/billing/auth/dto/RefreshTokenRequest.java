package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(description = "Refresh token")
    private String refreshToken;
}
