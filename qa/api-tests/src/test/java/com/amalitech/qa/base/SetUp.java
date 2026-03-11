package com.amalitech.qa.base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestWatcher;

import com.amalitech.qa.utils.ConfigManager;

import org.junit.jupiter.api.extension.ExtensionContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Base test class for all REST Assured API tests
 * Provides common setup and teardown functionality
 * Uses TestWatcherExtension to monitor and log test execution events
 */
@ExtendWith(SetUp.TestWatcherExtension.class)
public class SetUp {

    /**
     * Reusable request specification for all API requests
     * Configured with common headers and base URI
     */
    protected RequestSpecification requestSpec;

    /**
     * Setup method that runs before each test
     * Initializes REST Assured configuration, request specification
     */
    @BeforeEach
    public void setUp() {
        // Initialize REST Assured base URI
        RestAssured.baseURI = ConfigManager.getBaseUrl();

        // Build request specification
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getBaseUrl())
                .setContentType("application/json")
                .addHeader("Accept", "application/json")
                .build();


        // Set request specification as default
        RestAssured.requestSpecification = requestSpec;

    }

    /**
     * Teardown method that runs after each test
     * Performs cleanup operations
     * This prevents leftover settings from affecting subsequent tests
     */
    @AfterEach
    public void tearDown() {
        // Reset REST Assured configuration
        RestAssured.reset();
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
