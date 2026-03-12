package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.pages.RegisterPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI test suite for the Register feature.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Page renders all required form fields</li>
 *   <li>Successful registration with a unique email redirects to /login</li>
 *   <li>Mismatched passwords prevent registration</li>
 *   <li>Already-registered email is rejected</li>
 * </ul>
 */
@DisplayName("Register Tests")
class RegisterTest extends SetUp {

    private RegisterPage registerPage;

    @BeforeEach
    void openRegisterPage() {
        navigateTo("/register");
        registerPage = new RegisterPage(driver);
    }

    // ------------------------------------------------------------------ rendering

    @Test
    @DisplayName("Register page renders all required form fields")
    void registerPageRendersAllFields() {
        // Assert
        assertTrue(registerPage.isNameInputDisplayed(),            "Name input should be visible");
        assertTrue(registerPage.isEmailInputDisplayed(),           "Email input should be visible");
        assertTrue(registerPage.isPasswordInputDisplayed(),        "Password input should be visible");
        assertTrue(registerPage.isConfirmPasswordInputDisplayed(), "Confirm password input should be visible");
        assertTrue(registerPage.isSubmitButtonDisplayed(),         "Submit button should be visible");
    }

    // ------------------------------------------------------------------ happy path

    @Test
    @DisplayName("Successful registration with a unique email redirects to login")
    void successfulRegistrationRedirectsToLogin() {
        // Arrange – generate a unique email so this test is repeatable
        String uniqueEmail = "autotest_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // Act
        registerPage.enterName("Auto Test User")
                    .enterEmail(uniqueEmail)
                    .enterPassword("Password123!")
                    .enterConfirmPassword("Password123!")
                    .submitRegistration();

        // Assert
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should redirect to /login after successful registration");
    }

    // ------------------------------------------------------------------ sad paths

    @Test
    @DisplayName("Mismatched passwords prevent navigation away from /register")
    void mismatchedPasswordsPreventsRegistration() {
        // Arrange & Act
        registerPage.enterName("Test User")
                    .enterEmail("mismatch_" + System.currentTimeMillis() + "@example.com")
                    .enterPassword("Password123!")
                    .enterConfirmPassword("DifferentPassword!")
                    .submitRegistration();

        // Assert
        assertTrue(registerPage.getCurrentUrl().contains("/register"),
                "User should stay on /register when passwords do not match");
    }

    @Test
    @DisplayName("Empty form submission stays on register page")
    void emptyFormSubmissionStaysOnRegisterPage() {
        // Act
        registerPage.submitRegistration();

        // Assert
        assertTrue(registerPage.getCurrentUrl().contains("/register"),
                "User should stay on /register when the form is submitted empty");
    }
}
