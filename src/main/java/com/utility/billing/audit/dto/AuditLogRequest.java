package com.utility.billing.audit.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;
@Data
@Schema(description = "Internal audit log request")
public class AuditLogRequest {
    @Schema(description = "Actor user ID")
    private UUID userId;
    @Schema(example = "BILL_GENERATED")
    private String action;
    @Schema(example = "Bill")
    private String entityName;
    @Schema(description = "Affected entity ID")
    private UUID entityId;
    @Schema(example = "127.0.0.1")
    private String ipAddress;
    @Schema(example = "Generated monthly bill from meter reading")
    private String details;
}
