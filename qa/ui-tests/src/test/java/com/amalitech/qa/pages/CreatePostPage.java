package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

/**
 * Page Object for the Create Post form ({@code /create}).
 *
 * <p>Locators sourced from {@code frontend/src/pages/CreatePost/CreatePost.tsx}:
 * <pre>
 * data-testid="breadcrumb-home"         → home breadcrumb link
 * data-testid="close-create-post-btn"   → close (×) &lt;button&gt;
 * data-testid="post-title-input"        → title &lt;input&gt;
 * data-testid="category-select"         → category &lt;select&gt;
 * data-testid="post-body-textarea"      → body &lt;textarea&gt;
 * data-testid="cancel-create-post-btn"  → cancel &lt;button&gt;
 * data-testid="submit-create-post-btn"  → submit &lt;button&gt;
 * </pre>
 */
public class CreatePostPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    @FindBy(css = "[data-testid='breadcrumb-home']")
    private WebElement breadcrumbHome;

    @FindBy(css = "[data-testid='close-create-post-btn']")
    private WebElement closeButton;

    @FindBy(css = "[data-testid='post-title-input']")
    private WebElement postTitleInput;

    @FindBy(css = "[data-testid='category-select']")
    private WebElement categorySelect;

    @FindBy(css = "[data-testid='post-body-textarea']")
    private WebElement postBodyTextarea;

    @FindBy(css = "[data-testid='cancel-create-post-btn']")
    private WebElement cancelButton;

    @FindBy(css = "[data-testid='submit-create-post-btn']")
    private WebElement submitButton;

    public CreatePostPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    /** Waits until the title input is visible – confirms the create-post page has fully loaded. */
    public CreatePostPage waitForTitleInput() {
        wait.waitForVisibility(postTitleInput);
        return this;
    }

    // ------------------------------------------------------------------ actions

    public CreatePostPage enterTitle(String title) {
        wait.waitForVisibility(postTitleInput).clear();
        postTitleInput.sendKeys(title);
        return this;
    }

    public CreatePostPage selectCategory(String visibleText) {
        wait.waitForVisibility(categorySelect);
        new Select(categorySelect).selectByVisibleText(visibleText);
        return this;
    }

    public CreatePostPage enterBody(String body) {
        wait.waitForVisibility(postBodyTextarea).clear();
        postBodyTextarea.sendKeys(body);
        return this;
    }

    /** Submits the form and returns the Post Feed page. */
    public PostFeedPage submitPost() {
        wait.waitForClickability(submitButton).click();
        return new PostFeedPage(driver);
    }

    /** Cancels form submission and returns the Post Feed page. */
    public PostFeedPage cancelPost() {
        wait.waitForClickability(cancelButton).click();
        return new PostFeedPage(driver);
    }

    /** Closes the form via the (×) button and returns the Post Feed page. */
    public PostFeedPage closeForm() {
        wait.waitForClickability(closeButton).click();
        return new PostFeedPage(driver);
    }

    // ------------------------------------------------------------------ state queries

    public boolean isPostTitleInputDisplayed() {
        return postTitleInput.isDisplayed();
    }

    public boolean isCategorySelectDisplayed() {
        return categorySelect.isDisplayed();
    }

    public boolean isPostBodyTextareaDisplayed() {
        return postBodyTextarea.isDisplayed();
    }

    public boolean isSubmitButtonDisplayed() {
        return submitButton.isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
