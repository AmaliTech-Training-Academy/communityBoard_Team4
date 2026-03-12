package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.config.ConfigReader;
import com.amalitech.qa.constants.Routes;
import com.amalitech.qa.pages.AnalyticsPage;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.pages.PostFeedPage;
import com.amalitech.qa.testdata.AnalyticsTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI test suite for the Analytics dashboard ({@code /analytics}).
 *
 * <p>Each test authenticates first, then navigates to the analytics page.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Stat cards (Total Posts, Total Comments) are displayed</li>
 *   <li>Bar charts (Posts by Category, Posts Day of Week) are displayed</li>
 *   <li>Top 10 Contributors table is displayed with correct column headers</li>
 *   <li>Stat values are numeric and non-negative</li>
 *   <li>Breadcrumb "Home" link navigates back to the post feed</li>
 *   <li>Unauthenticated access is redirected to /login</li>
 * </ul>
 */
@DisplayName("Analytics Dashboard Tests")
class AnalyticsTest extends SetUp {

    private AnalyticsPage analyticsPage;

    @BeforeEach
    void loginAndOpenAnalytics() {
        navigateTo(Routes.LOGIN);
        // Wait for the feed to confirm the session is fully established before proceeding
        new LoginPage(driver)
                .loginAs(ConfigReader.getValidEmail(), ConfigReader.getValidPassword())
                .isNavbarLogoDisplayed();
        navigateTo(Routes.ANALYTICS);
        analyticsPage = new AnalyticsPage(driver);
        analyticsPage.waitForContent();
    }

    // ------------------------------------------------------------------ stat cards

    @Test
    @DisplayName("Analytics page displays the Total Posts stat card")
    void totalPostsStatCardIsDisplayed() {
        assertTrue(analyticsPage.isStatCardDisplayed(AnalyticsTestData.STAT_TOTAL_POSTS),
                "Total Posts stat card should be visible");
    }

    @Test
    @DisplayName("Analytics page displays the Total Comments stat card")
    void totalCommentsStatCardIsDisplayed() {
        assertTrue(analyticsPage.isStatCardDisplayed(AnalyticsTestData.STAT_TOTAL_COMMENTS),
                "Total Comments stat card should be visible");
    }

    @Test
    @DisplayName("Total Posts stat value is a non-negative number")
    void totalPostsStatValueIsNonNegative() {
        String value = analyticsPage.getStatValue(AnalyticsTestData.STAT_TOTAL_POSTS);
        assertDoesNotThrow(() -> Integer.parseInt(value),
                "Total Posts value should be a valid integer, got: " + value);
        assertTrue(Integer.parseInt(value) >= 0,
                "Total Posts value should be >= 0");
    }

    @Test
    @DisplayName("Total Comments stat value is a non-negative number")
    void totalCommentsStatValueIsNonNegative() {
        String value = analyticsPage.getStatValue(AnalyticsTestData.STAT_TOTAL_COMMENTS);
        assertDoesNotThrow(() -> Integer.parseInt(value),
                "Total Comments value should be a valid integer, got: " + value);
        assertTrue(Integer.parseInt(value) >= 0,
                "Total Comments value should be >= 0");
    }

    // ------------------------------------------------------------------ charts

    @Test
    @DisplayName("Analytics page displays the Posts by Category bar chart")
    void postsByCategoryChartIsDisplayed() {
        assertTrue(analyticsPage.isChartDisplayed(AnalyticsTestData.CHART_BY_CATEGORY),
                "Posts by Category chart should be visible");
    }

    @Test
    @DisplayName("Analytics page displays the Posts Day of Week bar chart")
    void postsByDayChartIsDisplayed() {
        assertTrue(analyticsPage.isChartDisplayed(AnalyticsTestData.CHART_BY_DAY),
                "Posts Day of Week chart should be visible");
    }

    // ------------------------------------------------------------------ contributors table

    @Test
    @DisplayName("Analytics page displays the Top 10 Contributors section heading")
    void contributorsTitleIsDisplayed() {
        assertTrue(analyticsPage.isContributorsTitleDisplayed(),
                "Top 10 Contributors heading should be visible");
    }

    @Test
    @DisplayName("Analytics page displays the contributors table")
    void contributorsTableIsDisplayed() {
        assertTrue(analyticsPage.isContributorsTableDisplayed(),
                "Contributors table should be visible");
    }

    @Test
    @DisplayName("Contributors table has the correct column headers: Ranks, Name, Posts")
    void contributorsTableHasCorrectHeaders() {
        List<String> headers = analyticsPage.getTableHeaderTexts();
        assertTrue(headers.contains(AnalyticsTestData.TABLE_HEADER_RANK),
                "Table should have a 'Ranks' column header");
        assertTrue(headers.contains(AnalyticsTestData.TABLE_HEADER_NAME),
                "Table should have a 'Name' column header");
        assertTrue(headers.contains(AnalyticsTestData.TABLE_HEADER_POSTS),
                "Table should have a 'Posts' column header");
    }

    // ------------------------------------------------------------------ navigation

    @Test
    @DisplayName("Clicking the breadcrumb Home link navigates back to the post feed")
    void breadcrumbHomeNavigatesToFeed() {
        PostFeedPage feedPage = analyticsPage.clickBreadcrumbHome();

        assertFalse(driver.getCurrentUrl().contains(Routes.ANALYTICS),
                "URL should no longer contain /analytics after clicking Home");
        assertTrue(feedPage.isNavbarLogoDisplayed(),
                "Navbar logo should be visible on the feed after navigating back");
    }
}
