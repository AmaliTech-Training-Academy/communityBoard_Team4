package com.amalitech.qa.testdata;

/**
 * Test data shared by Post Feed, Create Post, and Post Details tests.
 *
 * <p>Titles that must be unique per run should still be generated at test
 * runtime using, e.g., {@code PostTestData.POST_TITLE_PREFIX + System.currentTimeMillis()}.
 */
public final class PostTestData {

    private PostTestData() {}

    // ------------------------------------------------------------------ search

    /** Keyword submitted to the feed search box. */
    public static final String SEARCH_QUERY = "community";

    // ------------------------------------------------------------------ create post

    /** Prefix for dynamically generated post titles (append a timestamp at runtime). */
    public static final String POST_TITLE_PREFIX  = "Automated Post – ";

    /** Category selected when creating or editing a post. */
    public static final String POST_CATEGORY      = "General";

    /** Body text used for automated post creation. */
    public static final String POST_BODY          = "This post was created by an automated UI test.";

    /** Title entered into the form before clicking Cancel. */
    public static final String CANCELLED_DRAFT_TITLE = "Draft that will be cancelled";

    /** Title entered into the form before clicking the close (×) button. */
    public static final String CLOSED_DRAFT_TITLE    = "Draft that will be closed";
}
