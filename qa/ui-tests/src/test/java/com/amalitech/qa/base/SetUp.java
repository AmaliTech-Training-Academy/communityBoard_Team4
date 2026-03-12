package com.amalitech.qa.base;

import com.amalitech.qa.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Abstract base class shared by all UI test classes.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Manages the WebDriver lifecycle (setup / teardown per test method)</li>
 *   <li>Reads browser configuration from {@link ConfigReader}</li>
 *   <li>Exposes a {@link #navigateTo(String)} helper so tests never hard-code URLs</li>
 * </ul>
 *
 * <p>Extend this class in every test class:
 * <pre>{@code
 * class LoginTest extends BaseTest {
 *     @Test
 *     void myTest() {
 *         navigateTo("/login");
 *         // ...
 *     }
 * }
 * }</pre>
 */
@ExtendWith(SetUp.TestWatcherExtension.class)
public abstract class SetUp {

    protected WebDriver driver;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (ConfigReader.isHeadless()) {
            options.addArguments("--headless=new");   // Chromium headless (stable flag)
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");

        driver = new ChromeDriver(options);
        driver.manage().timeouts()
              .implicitlyWait(Duration.ofSeconds(ConfigReader.getImplicitWait()));
        driver.manage().window().maximize();
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Navigates to ${base.url}{@code path}.
     *
     * @param path the path segment, e.g. {@code "/login"} or {@code "/create"}
     */
    protected void navigateTo(String path) {
        driver.get(ConfigReader.getBaseUrl() + path);
    }

    /**
     * Inner class: JUnit 5 Extension that watches test execution and logs test lifecycle events
     * Implements TestWatcher to monitor test success, failure, and skipped scenarios
     */
    public static class TestWatcherExtension implements TestWatcher {

        private static final Logger logger = Logger.getLogger(TestWatcherExtension.class.getName());
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        /**
         * Called when a test is successfully executed
         */
        @Override
        public void testSuccessful(ExtensionContext context) {
            String testDisplayName = context.getDisplayName();
            String timestamp = LocalDateTime.now().format(formatter);

            logger.log(Level.INFO, String.format(
                    "%s - [PASSED]: %s",
                    timestamp,
                    testDisplayName
            ));
        }

        /**
         * Called when a test execution fails
         */
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            String testDisplayName = context.getDisplayName();
            String timestamp = LocalDateTime.now().format(formatter);
            String errorMessage = cause != null ? cause.getMessage() : "Unknown error";

            logger.log(Level.SEVERE, String.format(
                    "%s - [FAILED]: %s\n Cause: %s",
                    timestamp,
                    testDisplayName,
                    errorMessage
            ));
        }

        /**
         * Called when a test is disabled (skipped)
         */
        @Override
        public void testDisabled(ExtensionContext context, Optional<String> reason) {
            String testDisplayName = context.getDisplayName();
            String timestamp = LocalDateTime.now().format(formatter);

            logger.log(Level.INFO, String.format(
                    "%s - [SKIPPED]: %s\n Reason: %s",
                    timestamp,
                    testDisplayName,
                    reason.orElse("No message provided")
            ));
        }
    }
}
