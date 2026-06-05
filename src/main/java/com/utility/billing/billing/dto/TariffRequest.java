package com.utility.billing.billing.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @Schema(description = "Tariff configuration request")
public class TariffRequest {
    @NotBlank private String meterType;
    @NotBlank private String tariffName;
    @NotNull @DecimalMin("0") private BigDecimal rate;
    @NotNull private BigDecimal fixedCharge;
    @NotNull private BigDecimal vat;
    @NotNull private BigDecimal penaltyRate;
    @NotNull @Min(1) private Integer version;
    @NotNull private LocalDate effectiveDate;
    private boolean active = true;
}
