package com.utility.billing.billing.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.UUID;
@Data @Builder @Schema(description = "Bill response")
public class BillResponse {
    private UUID id; @Schema(description = "Account holder user ID") private UUID userId; private UUID meterId; private String billNumber;
    private Integer billingMonth; private Integer billingYear; private BigDecimal consumption;
    private BigDecimal amount; private BigDecimal taxAmount; private BigDecimal penalty;
    private BigDecimal balance; private String status; private LocalDateTime generatedDate;
}
