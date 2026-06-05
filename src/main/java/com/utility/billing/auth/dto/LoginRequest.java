package com.utility.billing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(example = "john@gmail.com")
    private String email;

    @NotBlank
    @Schema(example = "Password@123")
    private String password;
}
