package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.config.ConfigReader;
import com.amalitech.qa.constants.Routes;
import com.amalitech.qa.pages.CreatePostPage;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.pages.PostFeedPage;
import com.amalitech.qa.testdata.PostTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI test suite for the Create Post feature.
 *
 * <p>Each test logs in first, then navigates to the create post form.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Create post form renders all required fields</li>
 *   <li>Successfully creating a post returns to the feed</li>
 *   <li>Cancelling post creation returns to the feed</li>
 *   <li>Closing the form via the (×) button returns to the feed</li>
 *   <li>Submitting an empty form stays on the create page</li>
 * </ul>
 */
@DisplayName("Create Post Tests")
class CreatePostTest extends SetUp {

    private CreatePostPage createPostPage;

    @BeforeEach
    void loginAndOpenCreateForm() {
        navigateTo(Routes.LOGIN);
        createPostPage = new LoginPage(driver)
                .loginAs(ConfigReader.getValidEmail(), ConfigReader.getValidPassword())
                .clickCreatePost();
    }

    // ------------------------------------------------------------------ rendering

    @Test
    @DisplayName("Create post form renders title, category, body, and submit fields")
    void createPostFormRendersRequiredFields() {
        // Assert
        assertTrue(createPostPage.isPostTitleInputDisplayed(),   "Title input should be visible");
        assertTrue(createPostPage.isCategorySelectDisplayed(),   "Category select should be visible");
        assertTrue(createPostPage.isPostBodyTextareaDisplayed(), "Body textarea should be visible");
        assertTrue(createPostPage.isSubmitButtonDisplayed(),     "Submit button should be visible");
    }

    // ------------------------------------------------------------------ happy path

    @Test
    @DisplayName("Filling the form and submitting creates a post and returns to the feed")
    void successfulPostCreationReturnsToFeed() {
        // Arrange
        String uniqueTitle = PostTestData.POST_TITLE_PREFIX + System.currentTimeMillis();

        // Act
        PostFeedPage feedPage = createPostPage
                .enterTitle(uniqueTitle)
                .selectCategory(PostTestData.POST_CATEGORY)
                .enterBody(PostTestData.POST_BODY)
                .submitPost();

        // Assert
        assertTrue(feedPage.isCreatePostButtonDisplayed(),
                "Create post button should be visible on the feed after posting");
    }

    // ------------------------------------------------------------------ cancellation flows

    @Test
    @DisplayName("Clicking 'Cancel' discards the post and returns to the feed")
    void cancellingPostCreationReturnsToFeed() {
        // Arrange
        createPostPage.enterTitle(PostTestData.CANCELLED_DRAFT_TITLE);

        // Act
        PostFeedPage feedPage = createPostPage.cancelPost();

        // Assert
        assertTrue(feedPage.isCreatePostButtonDisplayed(),
                "Create post button should be visible after cancelling");
    }

    @Test
    @DisplayName("Clicking the close (×) button discards the post and returns to the feed")
    void closingFormReturnsToFeed() {
        // Arrange
        createPostPage.enterTitle(PostTestData.CLOSED_DRAFT_TITLE);

        // Act
        PostFeedPage feedPage = createPostPage.closeForm();

        // Assert
        assertTrue(feedPage.isCreatePostButtonDisplayed(),
                "Create post button should be visible after closing the form");
    }

    // ------------------------------------------------------------------ validation

    @Test
    @DisplayName("Submitting an empty form stays on the create post page")
    void emptyFormSubmissionStaysOnCreatePage() {
        // Act
        createPostPage.submitPost();

        // Assert
        assertTrue(driver.getCurrentUrl().contains(Routes.CREATE),
                "User should stay on /create when the form is submitted empty");
    }
}
