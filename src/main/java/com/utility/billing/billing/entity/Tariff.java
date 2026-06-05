package com.utility.billing.billing.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name = "tariffs", schema = "billing", uniqueConstraints = @UniqueConstraint(columnNames = {"meter_type","version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tariff extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "meter_type", nullable = false) private String meterType;
    @Column(name = "tariff_name", nullable = false) private String tariffName;
    @Column(nullable = false) private BigDecimal rate;
    @Column(name = "fixed_charge", nullable = false) private BigDecimal fixedCharge;
    @Column(nullable = false) private BigDecimal vat;
    @Column(name = "penalty_rate", nullable = false) private BigDecimal penaltyRate;
    @Column(nullable = false) private Integer version;
    @Column(name = "effective_date", nullable = false) private LocalDate effectiveDate;
    @Column(nullable = false) private boolean active;
}
