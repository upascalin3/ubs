package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "OTP verification request")
public class OtpVerifyRequest {

    @NotBlank
    @Email
    @Schema(example = "john@gmail.com")
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    @Schema(example = "123456")
    private String otpCode;
}
