package com.utility.billing.meter.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.UUID;
@Data @Builder @Schema(description = "Meter reading response")
public class ReadingResponse {
    private UUID id; private UUID meterId; private BigDecimal previousReading;
    private BigDecimal currentReading; private BigDecimal consumption;
    private LocalDate readingDate; private Integer month; private Integer year;
    private LocalDateTime createdAt;
}
