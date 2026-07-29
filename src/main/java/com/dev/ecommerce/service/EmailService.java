package com.dev.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@ecommerce.local}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Reset your password";
        String body = """
                You requested a password reset.

                Use this token to reset your password:
                %s

                This token expires in 15 minutes.
                If you did not request this, please ignore this email.
                """.formatted(resetToken);

        if (!mailEnabled) {
            log.info("Mail disabled. Password reset token for {}: {}", toEmail, resetToken);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendEmail(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            log.info("Mail disabled. Email to {} - Subject: {}", toEmail, subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
