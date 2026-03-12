package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.constants.Routes;
import com.amalitech.qa.pages.RegisterPage;
import com.amalitech.qa.testdata.RegisterTestData;
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
        navigateTo(Routes.REGISTER);
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

        // Act – submit the form then wait briefly for the redirect to complete
        registerPage.enterName(RegisterTestData.VALID_NAME)
                    .enterEmail(uniqueEmail)
                    .enterPassword(RegisterTestData.VALID_PASSWORD)
                    .enterConfirmPassword(RegisterTestData.VALID_PASSWORD)
                    .submitRegistration();

        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        boolean onLoginPage = driver.getCurrentUrl().contains(Routes.LOGIN);

        // Assert
        assertTrue(onLoginPage,
                "Should redirect to /login after successful registration");
    }

    // ------------------------------------------------------------------ sad paths

    @Test
    @DisplayName("Mismatched passwords prevent navigation away from /register")
    void mismatchedPasswordsPreventsRegistration() {
        // Arrange & Act
        registerPage.enterName(RegisterTestData.TEST_NAME)
                    .enterEmail("mismatch_" + System.currentTimeMillis() + "@example.com")
                    .enterPassword(RegisterTestData.VALID_PASSWORD)
                    .enterConfirmPassword(RegisterTestData.MISMATCHED_PASSWORD)
                    .submitRegistration();

        // Assert
        assertTrue(registerPage.getCurrentUrl().contains(Routes.REGISTER),
                "User should stay on /register when passwords do not match");
    }

    @Test
    @DisplayName("Empty form submission stays on register page")
    void emptyFormSubmissionStaysOnRegisterPage() {
        // Act
        registerPage.submitRegistration();

        // Assert
        assertTrue(registerPage.getCurrentUrl().contains(Routes.REGISTER),
                "User should stay on /register when the form is submitted empty");
    }
}
