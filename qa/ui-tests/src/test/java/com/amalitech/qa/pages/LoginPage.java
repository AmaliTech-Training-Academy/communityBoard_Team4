package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Page Object for the Login page ({@code /login}).
 *
 * <p>All locators use {@code data-testid} attributes sourced from
 * {@code frontend/src/pages/Login/Login.tsx}.
 *
 * <pre>
 * data-testid="email-input"    → email &lt;input&gt;
 * data-testid="password-input" → password &lt;input&gt;
 * data-testid="submit-button"  → login &lt;button&gt;
 * </pre>
 */
public class LoginPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    @FindBy(css = "[data-testid='email-input']")
    private WebElement emailInput;

    @FindBy(css = "[data-testid='password-input']")
    private WebElement passwordInput;

    @FindBy(css = "[data-testid='submit-button']")
    private WebElement submitButton;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------ actions

    public LoginPage enterEmail(String email) {
        wait.waitForVisibility(emailInput).clear();
        emailInput.sendKeys(email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        wait.waitForVisibility(passwordInput).clear();
        passwordInput.sendKeys(password);
        return this;
    }

    public void clickSubmit() {
        wait.waitForClickability(submitButton).click();
    }

    /**
     * Convenience method: fills the form and submits, returning the next page.
     *
     * @return {@link PostFeedPage} – the page the user lands on after login
     */
    public PostFeedPage loginAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSubmit();
        return new PostFeedPage(driver);
    }

    // ------------------------------------------------------------------ state queries

    public boolean isEmailInputDisplayed() {
        return emailInput.isDisplayed();
    }

    public boolean isPasswordInputDisplayed() {
        return passwordInput.isDisplayed();
    }

    public boolean isSubmitButtonDisplayed() {
        return submitButton.isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
