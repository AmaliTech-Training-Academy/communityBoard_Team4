package com.amalitech.qa.testdata;

/**
 * Test data for the Register feature.
 *
 * <p>Unique email addresses must be generated at test runtime (e.g. using
 * {@link java.util.UUID}) to keep tests repeatable; only static reusable
 * values are stored here.
 */
public final class RegisterTestData {

    private RegisterTestData() {}

    /** Display name used for the happy-path registration test. */
    public static final String VALID_NAME         = "Auto Test User";

    /** Display name used for sad-path / mismatch tests. */
    public static final String TEST_NAME          = "Test User";

    /** A strong password that satisfies the application's password policy. */
    public static final String VALID_PASSWORD     = "Password123!";

    /**
     * A second password that differs from {@link #VALID_PASSWORD},
     * used to trigger the "passwords do not match" validation.
     */
    public static final String MISMATCHED_PASSWORD = "DifferentPassword!";
}
