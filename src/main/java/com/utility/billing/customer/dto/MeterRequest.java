package com.utility.billing.customer.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate; import java.util.UUID;
@Data @Schema(description = "Assign meter to user (SRS: Meter.user_id)")
public class MeterRequest {
    @NotNull @Schema(description = "Account holder user ID") private UUID userId;
    @NotBlank private String meterNumber;
    @NotBlank @Schema(example = "WATER", allowableValues = {"WATER", "ELECTRICITY"}) private String meterType;
    @NotNull @PastOrPresent private LocalDate installationDate;
    @NotBlank private String status;
}
