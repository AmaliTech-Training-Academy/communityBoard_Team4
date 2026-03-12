package com.amalitech.qa.testdata;

/**
 * Test data for the Analytics dashboard feature.
 *
 * <p>Contains the expected labels and headings rendered by the Analytics page
 * so that test assertions never hard-code presentation strings directly.
 */
public final class AnalyticsTestData {

    private AnalyticsTestData() {}

    // ------------------------------------------------------------------ stat cards

    /** Title of the "Total Posts" stat card. */
    public static final String STAT_TOTAL_POSTS    = "Total Posts";

    /** Title of the "Total Comments" stat card. */
    public static final String STAT_TOTAL_COMMENTS = "Total Comments";

    // ------------------------------------------------------------------ charts

    /** Title of the posts-by-category bar chart. */
    public static final String CHART_BY_CATEGORY = "Posts by Category";

    /** Title of the posts-by-day-of-week bar chart. */
    public static final String CHART_BY_DAY      = "Posts Day of Week";

    // ------------------------------------------------------------------ contributors table

    /** Heading above the top-contributors table. */
    public static final String CONTRIBUTORS_TITLE      = "Top 10 Contributors";

    /** Column header for the rank column. */
    public static final String TABLE_HEADER_RANK  = "Ranks";

    /** Column header for the contributor name column. */
    public static final String TABLE_HEADER_NAME  = "Name";

    /** Column header for the post-count column. */
    public static final String TABLE_HEADER_POSTS = "Posts";
}
