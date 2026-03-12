package com.amalitech.qa.testdata;

/**
 * Test data for the Login feature.
 *
 * <p>Valid credentials are loaded at runtime from {@code config.properties}
 * via {@link com.amalitech.qa.config.ConfigReader}; only static / negative-path
 * values live here.
 */
public final class LoginTestData {

    private LoginTestData() {}

    /** A syntactically valid password that does not match any seeded account. */
    public static final String WRONG_PASSWORD  = "WrongPassword999!";

    /** An email address that has never been registered. */
    public static final String UNKNOWN_EMAIL   = "nobody@unknown-domain.com";

    /**
     * A well-formed password used alongside {@link #UNKNOWN_EMAIL} to ensure
     * the rejection is due to the unknown account, not a malformed password.
     */
    public static final String SAMPLE_PASSWORD = "Password123!";
}
