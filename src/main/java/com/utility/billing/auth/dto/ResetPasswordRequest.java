package com.utility.billing.auth.dto;

import com.utility.billing.common.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Reset password request")
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = ValidationPatterns.PASSWORD,
            message = "Password must be at least 8 characters with uppercase, lowercase, number and special character")
    private String newPassword;
}
