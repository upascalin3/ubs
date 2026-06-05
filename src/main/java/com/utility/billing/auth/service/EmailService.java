package com.utility.billing.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otpCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("UBS - " + purpose + " OTP");
            message.setText("Your OTP code is: " + otpCode + "\n\nThis code expires in 5 minutes.");
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}: {}", to, ex.getMessage());
            log.info("OTP for {} (dev fallback): {}", to, otpCode);
        }
    }
}
