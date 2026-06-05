package com.utility.billing.meter.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Data @Schema(description = "Capture meter reading request")
public class ReadingRequest {
    @NotNull private UUID meterId;
    @NotNull @DecimalMin("0") private BigDecimal previousReading;
    @NotNull @DecimalMin("0") private BigDecimal currentReading;
    @NotNull @PastOrPresent private LocalDate readingDate;
    @NotNull @Min(1) @Max(12) private Integer month;
    @NotNull @Min(2000) private Integer year;
}
