package com.utility.billing.notification.service;

import com.utility.billing.auth.entity.User;
import com.utility.billing.auth.repository.UserRepository;
import com.utility.billing.common.security.SecurityUtils;
import com.utility.billing.notification.dto.InternalNotificationRequest;
import com.utility.billing.notification.entity.Notification;
import com.utility.billing.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRepository repo;
	private final UserRepository userRepository;
	private final JavaMailSender mailSender;

	public NotificationService(NotificationRepository repo, UserRepository userRepository, JavaMailSender mailSender) {
		this.repo = repo;
		this.userRepository = userRepository;
		this.mailSender = mailSender;
	}

	@Transactional
	public Notification create(UUID userId, String title, String message) {
		return create(userId, title, message, null, null);
	}

	@Transactional
	public Notification create(UUID userId, String title, String message,
			byte[] attachment, String attachmentName) {
		Notification n = Notification.builder()
				.userId(userId)
				.title(title)
				.message(message)
				.status("SENT")
				.build();
		n = repo.save(n);
		log.info("Notification created for user {}", userId);
		deliverEmail(userId, title, message, attachment, attachmentName);
		return n;
	}

	@Transactional
	public Notification createInternal(InternalNotificationRequest req) {
		return create(req.getUserId(), req.getTitle(), req.getMessage());
	}

	public Page<Notification> byUser(UUID userId, Pageable pageable) {
		UUID scopedUserId = SecurityUtils.resolveUserScope(userId);
		return repo.findByUserId(scopedUserId, pageable);
	}

	public Page<Notification> list(Pageable pageable) {
		return repo.findAll(pageable);
	}

	@Transactional
	public Notification markRead(UUID id) {
		Notification n = repo.findById(id).orElseThrow();
		SecurityUtils.assertCustomerOwns(n.getUserId());
		n.setStatus("READ");
		return repo.save(n);
	}

	public void sendEmailToUser(UUID userId, String subject, String body) {
		deliverEmail(userId, subject, body, null, null);
	}

	public void sendEmail(String to, String subject, String body) {
		try {
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setTo(to);
			msg.setSubject(subject);
			msg.setText(body);
			mailSender.send(msg);
			log.info("Email sent to {}", to);
		} catch (Exception ex) {
			log.warn("Email send failed: {}", ex.getMessage());
		}
	}

	public void sendEmailWithAttachment(String to, String subject, String body,
			byte[] attachment, String attachmentName) {
		if (attachment == null || attachment.length == 0 || attachmentName == null || attachmentName.isBlank()) {
			sendEmail(to, subject, body);
			return;
		}
		try {
			var mimeMessage = mailSender.createMimeMessage();
			var helper = new MimeMessageHelper(mimeMessage, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(body);
			helper.addAttachment(attachmentName, new ByteArrayResource(attachment), "application/pdf");
			mailSender.send(mimeMessage);
			log.info("Email with attachment {} sent to {}", attachmentName, to);
		} catch (Exception ex) {
			log.warn("Email with attachment failed: {}", ex.getMessage());
		}
	}

	private void deliverEmail(UUID userId, String title, String message,
			byte[] attachment, String attachmentName) {
		userRepository.findById(userId)
				.map(User::getEmail)
				.filter(email -> email != null && !email.isBlank())
				.ifPresentOrElse(
						email -> sendEmailWithAttachment(email, "UBS - " + title, message, attachment, attachmentName),
						() -> log.warn("Notification email skipped; no email found for user {}", userId));
	}
}
