package com.utility.billing.customer.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder; import lombok.Data;
import java.time.LocalDate; import java.time.LocalDateTime; import java.util.UUID;
@Data @Builder @Schema(description = "Meter response")
public class MeterResponse {
    private UUID id; private UUID userId; private String meterNumber;
    private String meterType; private LocalDate installationDate;
    private String status; private LocalDateTime createdAt;
}
