package com.utility.billing.auth.dto;

import com.utility.billing.common.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    @Pattern(regexp = ValidationPatterns.FULL_NAME, message = "Full name must contain only letters and spaces")
    @Schema(example = "John Doe")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "Email must be lowercase and valid")
    @Schema(example = "john@gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = ValidationPatterns.PASSWORD,
            message = "Password must be at least 8 characters with uppercase, lowercase, number and special character")
    @Schema(example = "Password@123")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = ValidationPatterns.PHONE, message = "Phone must be in format 07XXXXXXXX")
    @Schema(example = "0781234567")
    private String phoneNumber;

    @NotBlank(message = "National ID is required")
    @Schema(example = "1199880012345678")
    private String nationalId;

    private String address;
}
