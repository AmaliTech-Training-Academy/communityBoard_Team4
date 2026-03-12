package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.config.ConfigReader;
import com.amalitech.qa.constants.Routes;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.pages.PostFeedPage;
import com.amalitech.qa.testdata.LoginTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full UI test suite for the Login feature.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Page renders all required elements</li>
 *   <li>Successful login with valid credentials</li>
 *   <li>Invalid credentials keep the user on /login</li>
 *   <li>Empty submission keeps the user on /login</li>
 * </ul>
 */
@DisplayName("Login Tests")
class LoginTest extends SetUp {

    private LoginPage loginPage;

    @BeforeEach
    void openLoginPage() {
        navigateTo(Routes.LOGIN);
        loginPage = new LoginPage(driver);
    }

    // ------------------------------------------------------------------ rendering

    @Test
    @DisplayName("Login page renders email input, password input, and submit button")
    void loginPageRendersRequiredElements() {
        // Assert
        assertTrue(loginPage.isEmailInputDisplayed(),    "Email input should be visible");
        assertTrue(loginPage.isPasswordInputDisplayed(), "Password input should be visible");
        assertTrue(loginPage.isSubmitButtonDisplayed(),  "Submit button should be visible");
    }

    // ------------------------------------------------------------------ happy path

    @Test
    @DisplayName("Valid credentials redirect the user to the post feed")
    void successfulLoginRedirectsToPostFeed() {
        // Arrange
        String email    = ConfigReader.getValidEmail();
        String password = ConfigReader.getValidPassword();

        // Act
        PostFeedPage feedPage = loginPage.loginAs(email, password);

        // Assert
        assertTrue(feedPage.isCreatePostButtonDisplayed(),
                "Create post button should appear after successful login");
        assertTrue(feedPage.isNavbarLogoDisplayed(),
                "Navbar logo should appear after successful login");
    }

    // ------------------------------------------------------------------ sad paths

    @Test
    @DisplayName("Wrong password keeps the user on the login page")
    void wrongPasswordStaysOnLoginPage() {
        // Arrange & Act
        loginPage.enterEmail(ConfigReader.getValidEmail())
                 .enterPassword(LoginTestData.WRONG_PASSWORD)
                 .clickSubmit();

        // Assert
        assertTrue(loginPage.getCurrentUrl().contains(Routes.LOGIN),
                "User should remain on /login with an incorrect password");
    }

    @Test
    @DisplayName("Unknown email keeps the user on the login page")
    void unknownEmailStaysOnLoginPage() {
        // Arrange & Act
        loginPage.enterEmail(LoginTestData.UNKNOWN_EMAIL)
                 .enterPassword(LoginTestData.SAMPLE_PASSWORD)
                 .clickSubmit();

        // Assert
        assertTrue(loginPage.getCurrentUrl().contains(Routes.LOGIN),
                "User should remain on /login with an unknown email");
    }

    @Test
    @DisplayName("Empty form submission keeps the user on the login page")
    void emptySubmissionStaysOnLoginPage() {
        // Act
        loginPage.clickSubmit();

        // Assert
        assertTrue(loginPage.getCurrentUrl().contains(Routes.LOGIN),
                "User should remain on /login when the form is submitted empty");
    }
}
