package com.utility.billing.notification.repository;

import com.utility.billing.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findByUserId(UUID userId, Pageable pageable);
}
