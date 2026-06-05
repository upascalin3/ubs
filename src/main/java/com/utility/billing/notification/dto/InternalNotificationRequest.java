package com.utility.billing.notification.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;
@Data
@Schema(description = "Internal notification creation request")
public class InternalNotificationRequest {
    @Schema(description = "Recipient user ID")
    private UUID userId;
    @Schema(example = "Bill generated")
    private String title;
    @Schema(example = "Your monthly bill is ready for review.")
    private String message;
}
