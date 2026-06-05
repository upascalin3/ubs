package com.utility.billing.payment.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.UUID;
@Data @Builder @Schema(description = "Payment response")
public class PaymentResponse {
    private UUID id; private UUID billId; private UUID userId;
    private BigDecimal amountPaid; private String paymentMethod;
    private LocalDate paymentDate; private String referenceNumber;
    private BigDecimal remainingBalance; private String billStatus;
    private LocalDateTime createdAt;
}
