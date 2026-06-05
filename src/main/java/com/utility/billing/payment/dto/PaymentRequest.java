package com.utility.billing.payment.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Data @Schema(description = "Record payment — billId only per SRS")
public class PaymentRequest {
    @NotNull private UUID billId;
    @NotNull @DecimalMin("0.01") private BigDecimal amountPaid;
    @NotBlank private String paymentMethod;
    @NotNull @PastOrPresent private LocalDate paymentDate;
    @NotBlank private String referenceNumber;
}
