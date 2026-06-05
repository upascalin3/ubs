package com.utility.billing.payment.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name = "payments", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "bill_id", nullable = false) private UUID billId;
    @Column(name = "amount_paid", nullable = false) private BigDecimal amountPaid;
    @Column(name = "payment_method", nullable = false) private String paymentMethod;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(name = "reference_number", nullable = false, unique = true) private String referenceNumber;
}
