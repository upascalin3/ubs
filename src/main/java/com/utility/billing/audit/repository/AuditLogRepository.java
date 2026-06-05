package com.utility.billing.audit.repository;
import com.utility.billing.audit.entity.AuditLog;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    Page<AuditLog> findByActionContainingIgnoreCase(String action, Pageable pageable);
}
