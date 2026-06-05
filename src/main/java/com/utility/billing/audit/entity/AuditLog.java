package com.utility.billing.audit.entity;
import jakarta.persistence.*; import lombok.*;
import java.time.LocalDateTime; import java.util.UUID;
@Entity @Table(name = "audit_logs", schema = "audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id") private UUID userId;
    @Column(nullable = false) private String action;
    @Column(name = "entity_name") private String entityName;
    @Column(name = "entity_id") private UUID entityId;
    @Column(nullable = false) private LocalDateTime timestamp;
    @Column(name = "ip_address") private String ipAddress;
    private String details;
}
