package com.amalitech.communityboard.service;

import com.amalitech.communityboard.config.JwtService;
import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.exception.DuplicateResourceException;
import com.amalitech.communityboard.exception.BadRequestException;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.auth.verification.enabled:false}")
    private boolean emailVerificationEnabled;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        boolean requiresVerification = emailVerificationEnabled;
        String verificationToken = requiresVerification ? UUID.randomUUID().toString() : null;
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
            .emailVerified(!requiresVerification)
                .emailVerificationToken(verificationToken)
            .emailVerificationExpiresAt(
                requiresVerification ? LocalDateTime.now().plusHours(24) : null
            )
                .build();
        userRepository.save(user);

        if (requiresVerification) {
            emailVerificationService.sendVerificationEmail(user);
        }

        String token = requiresVerification
            ? null
            : jwtService.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
            .id(user.getId()).token(token).email(user.getEmail())
                .name(user.getName()).role(user.getRole().name()).build();
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid credentials");
        }
        if (emailVerificationEnabled && !user.isEmailVerified()) {
            throw new UnauthorizedException("Email not verified. Please verify your email before logging in.");
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .id(user.getId()).token(token).email(user.getEmail())
                .name(user.getName()).role(user.getRole().name()).build();
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification token");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    public void resendVerification(ResendVerificationRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }
            user.setEmailVerificationToken(UUID.randomUUID().toString());
            user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            emailVerificationService.sendVerificationEmail(user);
        });
    }
}
