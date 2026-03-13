package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.verification.enabled=true"
})
class AuthVerificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockBean
    JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        reset(javaMailSender);
    }

    @Test
    void register_createsUnverifiedUser_sendsVerificationEmail_andReturnsNoToken() throws Exception {
        String email = "verify-register@test.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"Alice Doe\"," +
                                "\"email\":\"" + email + "\"," +
                                "\"password\":\"secure123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").value(nullValue()));

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(savedUser.isEmailVerified());
        org.junit.jupiter.api.Assertions.assertNotNull(savedUser.getEmailVerificationToken());
        org.junit.jupiter.api.Assertions.assertNotNull(savedUser.getEmailVerificationExpiresAt());
        verify(javaMailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void login_withUnverifiedUser_returns403() throws Exception {
        userRepository.save(User.builder()
                .name("Pending User")
                .email("pending-login@test.com")
                .password(passwordEncoder.encode("secure123"))
                .role(Role.USER)
                .emailVerified(false)
                .emailVerificationToken("pending-token")
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(12))
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"pending-login@test.com\"," +
                                "\"password\":\"secure123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Email not verified")));
    }

    @Test
    void verifyEmail_withValidToken_marksUserVerified_andClearsVerificationFields() throws Exception {
        userRepository.save(User.builder()
                .name("Verify User")
                .email("verify-token@test.com")
                .password(passwordEncoder.encode("secure123"))
                .role(Role.USER)
                .emailVerified(false)
                .emailVerificationToken("valid-token")
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(12))
                .build());

        mockMvc.perform(post("/api/auth/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User updatedUser = userRepository.findByEmail("verify-token@test.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updatedUser.isEmailVerified());
        org.junit.jupiter.api.Assertions.assertNull(updatedUser.getEmailVerificationToken());
        org.junit.jupiter.api.Assertions.assertNull(updatedUser.getEmailVerificationExpiresAt());
    }

    @Test
    void resendVerification_rotatesToken_andSendsEmail() throws Exception {
        userRepository.save(User.builder()
                .name("Resend User")
                .email("resend@test.com")
                .password(passwordEncoder.encode("secure123"))
                .role(Role.USER)
                .emailVerified(false)
                .emailVerificationToken("old-token")
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(1))
                .build());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resend@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("verification email has been sent")));

        User updatedUser = userRepository.findByEmail("resend@test.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("old-token", updatedUser.getEmailVerificationToken());
        org.junit.jupiter.api.Assertions.assertTrue(updatedUser.getEmailVerificationExpiresAt().isAfter(LocalDateTime.now()));
        verify(javaMailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
