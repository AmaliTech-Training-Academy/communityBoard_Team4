package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Page Object for the Post Feed page ({@code /}).
 *
 * <p>Locators sourced from {@code frontend/src/pages/PostFeed/PostFeed.tsx}
 * and {@code frontend/src/components/layout/Navbar.tsx}:
 * <pre>
 * data-testid="search-input"      → search text &lt;input&gt;
 * data-testid="search-submit-btn" → search submit &lt;button&gt;
 * data-testid="create-post-btn"   → create post &lt;button&gt;
 * data-testid="navbar-logo"       → navbar logo &lt;div&gt;
 * </pre>
 */
public class PostFeedPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    @FindBy(css = "[data-testid='search-input']")
    private WebElement searchInput;

    @FindBy(css = "[data-testid='search-submit-btn']")
    private WebElement searchSubmitButton;

    @FindBy(css = "[data-testid='create-post-btn']")
    private WebElement createPostButton;

    @FindBy(css = "[data-testid='navbar-logo']")
    private WebElement navbarLogo;

    public PostFeedPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------ actions

    public PostFeedPage searchFor(String query) {
        wait.waitForVisibility(searchInput).clear();
        searchInput.sendKeys(query);
        wait.waitForClickability(searchSubmitButton).click();
        return this;
    }

    /**
     * Clicks the "Create post" button and returns the Create Post page.
     * Waits for the title input to be visible so the page is fully loaded.
     */
    public CreatePostPage clickCreatePost() {
        wait.waitForClickability(createPostButton).click();
        CreatePostPage createPostPage = new CreatePostPage(driver);
        createPostPage.waitForTitleInput();
        return createPostPage;
    }

    // ------------------------------------------------------------------ state queries

    public boolean isSearchInputDisplayed() {
        return searchInput.isDisplayed();
    }

    public boolean isCreatePostButtonDisplayed() {
        return createPostButton.isDisplayed();
    }

    public boolean isNavbarLogoDisplayed() {
        return navbarLogo.isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
