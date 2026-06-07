package com.utility.billing.billing.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name = "bills", schema = "billing")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bill extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "meter_id", nullable = false) private UUID meterId;
    @Column(name = "bill_number", nullable = false, unique = true) private String billNumber;
    @Column(name = "billing_month", nullable = false) private Integer billingMonth;
    @Column(name = "billing_year", nullable = false) private Integer billingYear;
    @Column(nullable = false) private BigDecimal consumption;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "tax_amount", nullable = false) private BigDecimal taxAmount;
    @Column(nullable = false) private BigDecimal penalty;
    @Column(nullable = false) private BigDecimal balance;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BillStatus status;
    @Column(name = "generated_date", nullable = false) private LocalDateTime generatedDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "approved_by") private UUID approvedBy;
}
