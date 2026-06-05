package com.utility.billing.notification.controller;

import com.utility.billing.common.dto.ApiResponse;
import com.utility.billing.notification.dto.InternalNotificationRequest;
import com.utility.billing.notification.entity.Notification;
import com.utility.billing.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Billing and payment notifications")
public class NotificationController {

	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@io.swagger.v3.oas.annotations.Hidden
	@PostMapping("/internal")
	@Operation(summary = "Internal notification")
	public ApiResponse<Notification> internal(@RequestBody InternalNotificationRequest req) {
		return ApiResponse.success(service.createInternal(req));
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
	@Operation(summary = "Notifications by user", description = "GET /api/notifications/user/{userId}")
	public ApiResponse<Page<Notification>> byUser(@PathVariable UUID userId,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.byUser(userId, pageable));
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "List all notifications")
	public ApiResponse<Page<Notification>> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(service.list(pageable));
	}

	@PutMapping("/{id}/read")
	@PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
	@Operation(summary = "Mark notification as read")
	public ApiResponse<Notification> markRead(@PathVariable UUID id) {
		return ApiResponse.success(service.markRead(id));
	}
}
