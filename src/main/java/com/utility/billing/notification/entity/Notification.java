package com.utility.billing.notification.entity;
import com.utility.billing.common.entity.BaseAuditEntity;
import jakarta.persistence.*; import lombok.*;
import java.util.UUID;
@Entity @Table(name = "notifications", schema = "notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(nullable = false) private String status;
}
