package com.amalitech.qa.testdata;

import com.amalitech.qa.dto.AuthRequest;
import com.amalitech.qa.dto.RegisterRequest;
import com.amalitech.qa.utils.ConfigManager;
import org.junit.jupiter.params.provider.Arguments;

import java.util.UUID;
import java.util.stream.Stream;

public final class AuthTestData {
    public static RegisterRequest defaultRegisterRequest() {
        return new RegisterRequest("QA Tester", uniqueEmail("register"), "password123");
    }

    public static AuthRequest defaultValidLoginRequest() {
        return new AuthRequest(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
    }

    public static Stream<Arguments> validRegisterRequests() {
        return Stream.of(
                Arguments.of(defaultRegisterRequest())
        );
    }

    public static Stream<Arguments> invalidEmailRegisterRequests() {
        return Stream.of(
                Arguments.of(new RegisterRequest("QA Tester", "invalid-email-format", "password123"))
        );
    }

    public static Stream<Arguments> shortPasswordRegisterRequests() {
        return Stream.of(
                Arguments.of(new RegisterRequest("QA Tester", uniqueEmail("shortpass"), "123"))
        );
    }

    public static Stream<Arguments> validLoginRequests() {
        return Stream.of(
                Arguments.of(new AuthRequest(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword())),
                Arguments.of(new AuthRequest(ConfigManager.getUserEmail(), ConfigManager.getUserPassword()))
        );
    }

    public static Stream<Arguments> validLoginRegistrationRequests() {
        return Stream.of(
                Arguments.of(new RegisterRequest("Login Seed User 1", uniqueEmail("login-seed-1"), "password123")),
                Arguments.of(new RegisterRequest("Login Seed User 2", uniqueEmail("login-seed-2"), "password123"))
        );
    }

    public static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of(new AuthRequest("missing.user@amalitech.com", "password123"), 404),
                Arguments.of(new AuthRequest(ConfigManager.getAdminEmail(), "wrong-password"), 404),
                Arguments.of(new AuthRequest("", ""), 400)
        );
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.com";
    }
}
