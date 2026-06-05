package com.utility.billing.customer.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;
@Entity @Table(name = "meters", schema = "customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Meter extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "meter_number", nullable = false, unique = true) private String meterNumber;
    @Enumerated(EnumType.STRING) @Column(name = "meter_type", nullable = false) private MeterType meterType;
    @Column(name = "installation_date", nullable = false) private LocalDate installationDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MeterStatus status;
}
