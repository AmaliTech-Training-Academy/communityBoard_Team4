package com.amalitech.qa.pages;

import com.amalitech.qa.utilities.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

/**
 * Page Object for the Post Details page ({@code /post/:id}).
 *
 * <p>Static locators sourced from {@code frontend/src/pages/PostDetails/PostDetails.tsx}.
 * Dynamic per-comment elements (edit/delete/save) are resolved at runtime using
 * the comment ID, e.g. {@code data-testid="edit-comment-{id}"}.
 *
 * <pre>
 * Static:
 *   data-testid="edit-post-btn"             → edit post &lt;button&gt;
 *   data-testid="delete-post-btn"           → delete post &lt;button&gt;
 *   data-testid="add-comment-textarea"      → new comment &lt;textarea&gt;
 *   data-testid="submit-comment-button"     → submit comment &lt;button&gt;
 *   data-testid="close-edit-post-btn"       → close edit modal &lt;button&gt;
 *   data-testid="edit-post-title-input"     → edit title &lt;input&gt;
 *   data-testid="edit-category-select"      → edit category &lt;select&gt;
 *   data-testid="edit-post-body-textarea"   → edit body &lt;textarea&gt;
 *   data-testid="cancel-edit-post-btn"      → cancel edit &lt;button&gt;
 *   data-testid="save-edit-post-btn"        → save edit &lt;button&gt;
 *
 * Dynamic (replace {id} with the comment ID string):
 *   data-testid="comment-content-{id}"      → comment text &lt;div&gt;
 *   data-testid="edit-comment-{id}"         → edit comment &lt;button&gt;
 *   data-testid="delete-comment-{id}"       → delete comment &lt;button&gt;
 *   data-testid="edit-input-{id}"           → edit comment &lt;input&gt;
 *   data-testid="save-edit-comment-{id}"    → save edited comment &lt;button&gt;
 * </pre>
 */
public class PostDetailsPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    // Post-level actions
    @FindBy(css = "[data-testid='edit-post-btn']")
    private WebElement editPostButton;

    @FindBy(css = "[data-testid='delete-post-btn']")
    private WebElement deletePostButton;

    // Comment creation
    @FindBy(css = "[data-testid='add-comment-textarea']")
    private WebElement addCommentTextarea;

    @FindBy(css = "[data-testid='submit-comment-button']")
    private WebElement submitCommentButton;

    // Edit post modal
    @FindBy(css = "[data-testid='close-edit-post-btn']")
    private WebElement closeEditPostButton;

    @FindBy(css = "[data-testid='edit-post-title-input']")
    private WebElement editPostTitleInput;

    @FindBy(css = "[data-testid='edit-category-select']")
    private WebElement editCategorySelect;

    @FindBy(css = "[data-testid='edit-post-body-textarea']")
    private WebElement editPostBodyTextarea;

    @FindBy(css = "[data-testid='cancel-edit-post-btn']")
    private WebElement cancelEditPostButton;

    @FindBy(css = "[data-testid='save-edit-post-btn']")
    private WebElement saveEditPostButton;

    public PostDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------ comment actions

    public PostDetailsPage addComment(String commentText) {
        wait.waitForVisibility(addCommentTextarea).clear();
        addCommentTextarea.sendKeys(commentText);
        wait.waitForClickability(submitCommentButton).click();
        return this;
    }

    /** Returns the visible text content of a specific comment. */
    public String getCommentText(String commentId) {
        WebElement content = wait.waitForVisibilityBy(
                By.cssSelector("[data-testid='comment-content-" + commentId + "']"));
        return content.getText();
    }

    public PostDetailsPage editComment(String commentId, String newText) {
        driver.findElement(
                By.cssSelector("[data-testid='edit-comment-" + commentId + "']")).click();
        WebElement editInput = wait.waitForVisibilityBy(
                By.cssSelector("[data-testid='edit-input-" + commentId + "']"));
        editInput.clear();
        editInput.sendKeys(newText);
        driver.findElement(
                By.cssSelector("[data-testid='save-edit-comment-" + commentId + "']")).click();
        return this;
    }

    public PostDetailsPage deleteComment(String commentId) {
        driver.findElement(
                By.cssSelector("[data-testid='delete-comment-" + commentId + "']")).click();
        return this;
    }

    // ------------------------------------------------------------------ post edit actions

    public PostDetailsPage clickEditPost() {
        wait.waitForClickability(editPostButton).click();
        return this;
    }

    public PostDetailsPage editPostTitle(String newTitle) {
        wait.waitForVisibility(editPostTitleInput).clear();
        editPostTitleInput.sendKeys(newTitle);
        return this;
    }

    public PostDetailsPage editPostCategory(String visibleText) {
        wait.waitForVisibility(editCategorySelect);
        new Select(editCategorySelect).selectByVisibleText(visibleText);
        return this;
    }

    public PostDetailsPage editPostBody(String newBody) {
        wait.waitForVisibility(editPostBodyTextarea).clear();
        editPostBodyTextarea.sendKeys(newBody);
        return this;
    }

    /** Saves the edited post and returns the Post Feed page. */
    public PostFeedPage saveEditedPost() {
        wait.waitForClickability(saveEditPostButton).click();
        return new PostFeedPage(driver);
    }

    public PostDetailsPage cancelEditPost() {
        wait.waitForClickability(cancelEditPostButton).click();
        return this;
    }

    /** Deletes the post and returns the Post Feed page. */
    public PostFeedPage deletePost() {
        wait.waitForClickability(deletePostButton).click();
        return new PostFeedPage(driver);
    }

    // ------------------------------------------------------------------ state queries

    public boolean isAddCommentTextareaDisplayed() {
        return addCommentTextarea.isDisplayed();
    }

    public boolean isEditPostButtonDisplayed() {
        return editPostButton.isDisplayed();
    }

    public boolean isDeletePostButtonDisplayed() {
        return deletePostButton.isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
