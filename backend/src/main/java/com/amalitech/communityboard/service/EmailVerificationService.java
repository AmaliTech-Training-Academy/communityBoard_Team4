package com.amalitech.communityboard.service;

import com.amalitech.communityboard.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.auth.verification.enabled:false}")
    private boolean verificationEnabled;

    @Value("${app.auth.verification.from:no-reply@communityboard.local}")
    private String fromAddress;

    @Value("${app.auth.verification.verify-url:http://localhost:3000/verify-email}")
    private String verifyUrl;

    public void sendVerificationEmail(User user) {
        if (!verificationEnabled) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email verification enabled but JavaMailSender is not configured.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Verify your Community Board email");
            message.setText(buildBody(user));
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send verification email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    private String buildBody(User user) {
        String link = verifyUrl + "?token=" + user.getEmailVerificationToken();
        return "Welcome to Community Board, " + user.getName() + "!\n\n"
                + "Please verify your email by clicking the link below:\n"
                + link + "\n\n"
                + "This link expires in 24 hours.";
    }
}
