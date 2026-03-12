package com.amalitech.qa.constants;

/**
 * Centralised store of all UI routes used across the test suite.
 *
 * <p>Pass values directly to {@code navigateTo()} in test classes:
 * <pre>{@code
 * navigateTo(Routes.LOGIN);
 * }</pre>
 *
 * <p>Use in URL assertions:
 * <pre>{@code
 * assertTrue(driver.getCurrentUrl().contains(Routes.CREATE));
 * }</pre>
 */
public final class Routes {

    private Routes() {}

    /** {@code /login} – Login page. */
    public static final String LOGIN    = "/login";

    /** {@code /register} – Registration page. */
    public static final String REGISTER = "/register";

    /** {@code /} – Post feed (home / dashboard). */
    public static final String FEED     = "/";

    /** {@code /create} – Create-post form. */
    public static final String CREATE   = "/create";
}
