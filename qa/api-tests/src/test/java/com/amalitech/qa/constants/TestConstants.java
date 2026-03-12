package com.amalitech.qa.constants;

/**
 * Shared test constants — eliminates hardcoded literals from test files.
 * All fixed strings and values used during test setup or assertions live here.
 */
public final class TestConstants {

    private TestConstants() {
    }

    // -------------------------------------------------------------------------
    // Credentials / Auth
    // -------------------------------------------------------------------------
    public static final String DEFAULT_PASSWORD   = "password123";
    public static final String BLANK              = "";

    // -------------------------------------------------------------------------
    // Category values
    // -------------------------------------------------------------------------
    public static final String CATEGORY_NEWS       = "NEWS";
    public static final String CATEGORY_EVENT      = "EVENT";
    public static final String CATEGORY_DISCUSSION = "DISCUSSION";
    public static final String CATEGORY_ALERT      = "ALERT";
    public static final int    TOTAL_CATEGORIES    = 4;
    public static final String INVALID_CATEGORY    = "INVALID_CATEGORY";

    // -------------------------------------------------------------------------
    // Post test fixture data
    // -------------------------------------------------------------------------
    public static final String POST_AUTHOR_NAME             = "Post Author";
    public static final String POST_OTHER_USER_NAME         = "Post Other User";

    public static final String POST_UPDATED_BY_AUTHOR_TITLE = "Updated by Author";
    public static final String POST_UPDATED_BY_ADMIN_TITLE  = "Updated by Admin";
    public static final String POST_UNAUTHORIZED_TITLE      = "Unauthorized update attempt";

    // -------------------------------------------------------------------------
    // Comment test fixture data
    // -------------------------------------------------------------------------
    public static final String COMMENT_AUTHOR_NAME         = "Comment Author";
    public static final String COMMENT_OTHER_USER_NAME     = "Comment Other User";
    public static final String COMMENT_TEST_POST_TITLE     = "Comment Test Post";

    public static final String COMMENT_UPDATED_BY_AUTHOR   = "Updated by the author.";
    public static final String COMMENT_UPDATED_BY_ADMIN    = "Updated by admin.";
    public static final String COMMENT_UNAUTHORIZED_UPDATE = "Unauthorized update attempt.";

    // -------------------------------------------------------------------------
    // Search test fixture data
    // -------------------------------------------------------------------------
    public static final String SEARCH_FIXTURE_OWNER_NAME  = "Search Fixture Owner";
    public static final String SEARCH_POST_TITLE_PREFIX   = "Search Test ";
    public static final String SEARCH_POST_BODY_PREFIX    = "Search test body content for category ";
    public static final String SEARCH_KEYWORD             = "Search Test";
    public static final String SEARCH_KEYWORD_NEWS        = "Search Test NEWS";
    public static final String SEARCH_NO_MATCH_KEYWORD    = "zzznomatch999xyz";

    // -------------------------------------------------------------------------
    // Miscellanea
    // -------------------------------------------------------------------------
    /** Non-existent resource ID used to trigger 404 responses. */
    public static final long   NON_EXISTENT_ID = 999_999_999L;
}
