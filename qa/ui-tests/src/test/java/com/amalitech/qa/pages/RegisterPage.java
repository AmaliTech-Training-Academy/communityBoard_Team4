package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Page Object for the Register page ({@code /register}).
 *
 * <p>Locators sourced from {@code frontend/src/pages/Register/Register.tsx}:
 * <pre>
 * data-testid="name-input"             → full-name &lt;input&gt;
 * data-testid="email-input"            → email &lt;input&gt;
 * data-testid="password-input"         → password &lt;input&gt;
 * data-testid="confirm-password-input" → confirm password &lt;input&gt;
 * data-testid="submit-button"          → register &lt;button&gt;
 * </pre>
 */
public class RegisterPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    @FindBy(css = "[data-testid='name-input']")
    private WebElement nameInput;

    @FindBy(css = "[data-testid='email-input']")
    private WebElement emailInput;

    @FindBy(css = "[data-testid='password-input']")
    private WebElement passwordInput;

    @FindBy(css = "[data-testid='confirm-password-input']")
    private WebElement confirmPasswordInput;

    @FindBy(css = "[data-testid='submit-button']")
    private WebElement submitButton;

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------ actions

    public RegisterPage enterName(String name) {
        wait.waitForVisibility(nameInput).clear();
        nameInput.sendKeys(name);
        return this;
    }

    public RegisterPage enterEmail(String email) {
        wait.waitForVisibility(emailInput).clear();
        emailInput.sendKeys(email);
        return this;
    }

    public RegisterPage enterPassword(String password) {
        wait.waitForVisibility(passwordInput).clear();
        passwordInput.sendKeys(password);
        return this;
    }

    public RegisterPage enterConfirmPassword(String confirmPassword) {
        wait.waitForVisibility(confirmPasswordInput).clear();
        confirmPasswordInput.sendKeys(confirmPassword);
        return this;
    }

    /**
     * Submits the registration form.
     *
     * @return {@link LoginPage} – the page the user is redirected to on success
     */
    public LoginPage submitRegistration() {
        wait.waitForClickability(submitButton).click();
        return new LoginPage(driver);
    }

    // ------------------------------------------------------------------ state queries

    public boolean isNameInputDisplayed() {
        return nameInput.isDisplayed();
    }

    public boolean isEmailInputDisplayed() {
        return emailInput.isDisplayed();
    }

    public boolean isPasswordInputDisplayed() {
        return passwordInput.isDisplayed();
    }

    public boolean isConfirmPasswordInputDisplayed() {
        return confirmPasswordInput.isDisplayed();
    }

    public boolean isSubmitButtonDisplayed() {
        return submitButton.isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
