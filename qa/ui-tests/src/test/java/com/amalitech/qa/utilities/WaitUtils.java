package com.amalitech.qa.utilities;

import com.amalitech.qa.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Thin wrapper around {@link WebDriverWait} that provides readable
 * explicit-wait helpers consumed by Page Object classes.
 *
 * <p>Usage inside a Page Object:
 * <pre>{@code
 * private final WaitUtils wait;
 *
 * public LoginPage(WebDriver driver) {
 *     this.wait = new WaitUtils(driver);
 *     PageFactory.initElements(driver, this);
 * }
 *
 * public void clickSubmit() {
 *     wait.waitForClickability(submitButton).click();
 * }
 * }</pre>
 */
public class WaitUtils {

    private final WebDriverWait wait;

    /** Uses the {@code explicit.wait} value from {@code config.properties}. */
    public WaitUtils(WebDriver driver) {
        this(driver, ConfigReader.getExplicitWait());
    }

    public WaitUtils(WebDriver driver, int timeoutSeconds) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    /** Waits until {@code element} is visible and returns it. */
    public WebElement waitForVisibility(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /** Waits until the element located by {@code locator} is visible and returns it. */
    public WebElement waitForVisibilityBy(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until {@code element} is clickable and returns it. */
    public WebElement waitForClickability(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /** Waits until the element located by {@code locator} is clickable and returns it. */
    public WebElement waitForClickabilityBy(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Waits until {@code element} is no longer visible. */
    public boolean waitForInvisibility(WebElement element) {
        return wait.until(ExpectedConditions.invisibilityOf(element));
    }

    /** Waits until the current URL contains {@code urlFragment}. */
    public void waitForUrlContaining(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }
}
