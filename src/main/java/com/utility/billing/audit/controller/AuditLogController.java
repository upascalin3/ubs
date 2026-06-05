package com.utility.billing.audit.controller;
import com.utility.billing.audit.dto.AuditLogRequest;
import com.utility.billing.audit.entity.AuditLog;
import com.utility.billing.audit.service.AuditLogService;
import com.utility.billing.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/audit")
@Tag(name = "Audit Logs", description = "System logs, user activities, security logs")
public class AuditLogController {
    private final AuditLogService service;
    public AuditLogController(AuditLogService s) { service=s; }
    @io.swagger.v3.oas.annotations.Hidden
    @PostMapping("/internal") @Operation(summary = "Internal audit log", description = "Called by other microservices")
    public ApiResponse<AuditLog> internal(@RequestBody AuditLogRequest req) { return ApiResponse.success(service.log(req)); }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit logs", description = "Accessible only by ROLE_ADMIN")
    public ApiResponse<Page<AuditLog>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.list(pageable)); }
    @GetMapping("/user/{userId}") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Audit logs by user")
    public ApiResponse<Page<AuditLog>> byUser(@PathVariable UUID userId, @ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.byUser(userId, pageable)); }
    @GetMapping("/search") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search audit logs by action")
    public ApiResponse<Page<AuditLog>> search(@RequestParam String action, @ParameterObject @PageableDefault(size = 20) Pageable pageable) { return ApiResponse.success(service.search(action, pageable)); }
}
