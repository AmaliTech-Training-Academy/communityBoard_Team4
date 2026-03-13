package com.amalitech.communityboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    public void send(String from, String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP email delivery selected, but JavaMailSender is not configured.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send SMTP email to {}: {}", to, ex.getMessage());
        }
    }
}
