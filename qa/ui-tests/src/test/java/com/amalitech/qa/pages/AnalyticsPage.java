package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * Page Object for the Analytics dashboard ({@code /analytics}).
 *
 * <p>The Analytics page has no {@code data-testid} attributes; locators
 * use the stable CSS class names defined in {@code Analytics.css}.
 *
 * <pre>
 * .analytics-content           → rendered only when data has loaded
 * .analytics-stat-title        → "Total Posts" / "Total Comments" headings
 * .analytics-stat-value        → numeric stat values
 * .analytics-chart-title       → "Posts by Category" / "Posts Day of Week"
 * .analytics-contributors-title → "Top 10 Contributors" heading
 * .analytics-table             → contributors &lt;table&gt;
 * .analytics-th                → table column headers
 * .breadcrumb-clickable        → breadcrumb "Home" link
 * </pre>
 */
public class AnalyticsPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    /** Present only after data has loaded successfully (replaces the loading spinner). */
    @FindBy(css = ".analytics-content")
    private WebElement analyticsContent;

    @FindBy(css = ".analytics-contributors-title")
    private WebElement contributorsTitle;

    @FindBy(css = ".analytics-table")
    private WebElement contributorsTable;

    public AnalyticsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------ wait helpers

    /** Blocks until the data section is visible (loading complete, no error). */
    public AnalyticsPage waitForContent() {
        wait.waitForVisibility(analyticsContent);
        return this;
    }

    // ------------------------------------------------------------------ state queries

    /**
     * Returns {@code true} when a stat card with the given title heading is visible.
     *
     * @param title e.g. {@code "Total Posts"} or {@code "Total Comments"}
     */
    public boolean isStatCardDisplayed(String title) {
        List<WebElement> elements = driver.findElements(
                By.xpath("//h3[contains(@class,'analytics-stat-title')"
                        + " and normalize-space(text())='" + title + "']"));
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    /**
     * Returns the numeric text of the stat value for the given card title.
     *
     * @param title e.g. {@code "Total Posts"}
     */
    public String getStatValue(String title) {
        WebElement card = driver.findElement(
                By.xpath("//h3[contains(@class,'analytics-stat-title')"
                        + " and normalize-space(text())='" + title + "']"
                        + "/ancestor::div[contains(@class,'analytics-stat-card')]"));
        return card.findElement(By.cssSelector(".analytics-stat-value")).getText();
    }

    /**
     * Returns {@code true} when a bar chart with the given title is visible.
     *
     * @param title e.g. {@code "Posts by Category"} or {@code "Posts Day of Week"}
     */
    public boolean isChartDisplayed(String title) {
        List<WebElement> elements = driver.findElements(
                By.xpath("//h3[contains(@class,'analytics-chart-title')"
                        + " and normalize-space(text())='" + title + "']"));
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    /** Returns {@code true} when the "Top 10 Contributors" heading is visible. */
    public boolean isContributorsTitleDisplayed() {
        return contributorsTitle.isDisplayed();
    }

    /** Returns {@code true} when the contributors table is visible. */
    public boolean isContributorsTableDisplayed() {
        return contributorsTable.isDisplayed();
    }

    /** Returns the text content of every {@code <th>} in the contributors table. */
    public List<String> getTableHeaderTexts() {
        return driver.findElements(By.cssSelector(".analytics-th"))
                .stream()
                .map(WebElement::getText)
                .toList();
    }

    // ------------------------------------------------------------------ navigation

    /**
     * Clicks the "Home" breadcrumb link and returns the Post Feed page.
     */
    public PostFeedPage clickBreadcrumbHome() {
        wait.waitForClickabilityBy(
                By.xpath("//span[contains(@class,'breadcrumb-clickable')"
                        + " and normalize-space(text())='Home']")).click();
        return new PostFeedPage(driver);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
