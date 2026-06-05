package com.utility.billing.meter.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name = "meter_readings", schema = "meter", uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id","month","year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterReading extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "meter_id", nullable = false) private UUID meterId;
    @Column(name = "previous_reading", nullable = false) private BigDecimal previousReading;
    @Column(name = "current_reading", nullable = false) private BigDecimal currentReading;
    @Column(name = "reading_date", nullable = false) private LocalDate readingDate;
    @Column(nullable = false) private Integer month;
    @Column(nullable = false) private Integer year;
}
