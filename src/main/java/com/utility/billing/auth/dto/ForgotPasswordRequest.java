package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Forgot password request")
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    @Schema(example = "john@gmail.com")
    private String email;
}
