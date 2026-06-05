package com.utility.billing.billing.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal; import java.util.UUID;
@Data @Schema(description = "Generate bill request (SRS: Bill.user_id)")
public class BillRequest {
    @NotNull @Schema(description = "Account holder user ID") private UUID userId;
    @NotNull private UUID meterId;
    @NotBlank private String meterType;
    @NotNull @DecimalMin("0") private BigDecimal consumption;
    @NotNull @Min(1) @Max(12) private Integer billingMonth;
    @NotNull @Min(2000) private Integer billingYear;
}
