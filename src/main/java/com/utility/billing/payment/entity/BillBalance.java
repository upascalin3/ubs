package com.utility.billing.payment.entity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name = "bill_balances", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillBalance {
    @Id @Column(name = "bill_id") private UUID billId;
    @Column(nullable = false) private BigDecimal balance;
    @Column(nullable = false) private String status;
}
