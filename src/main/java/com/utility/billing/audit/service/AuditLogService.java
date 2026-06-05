package com.utility.billing.audit.service;
import com.utility.billing.audit.dto.AuditLogRequest;
import com.utility.billing.audit.entity.AuditLog;
import com.utility.billing.audit.repository.AuditLogRepository;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.util.UUID;
@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo) { this.repo = repo; }
    @Transactional
    public AuditLog log(AuditLogRequest req) {
        AuditLog entry = AuditLog.builder().userId(req.getUserId()).action(req.getAction())
            .entityName(req.getEntityName()).entityId(req.getEntityId())
            .timestamp(LocalDateTime.now()).ipAddress(req.getIpAddress()).details(req.getDetails()).build();
        log.info("Audit: {} by user {}", req.getAction(), req.getUserId());
        return repo.save(entry);
    }
    public Page<AuditLog> list(Pageable p) { return repo.findAll(p); }
    public Page<AuditLog> byUser(UUID userId, Pageable p) { return repo.findByUserId(userId, p); }
    public Page<AuditLog> search(String action, Pageable p) { return repo.findByActionContainingIgnoreCase(action, p); }
}
