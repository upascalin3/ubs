package com.utility.billing.customer.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name = "files", schema = "customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoredFile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "original_name", nullable = false) private String originalName;
    @Column(name = "stored_name", nullable = false) private String storedName;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private Long sizeBytes;
    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private UUID createdBy;
}
