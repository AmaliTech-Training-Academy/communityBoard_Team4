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
 * UI test suite for the Post Feed page.
 *
 * <p>Each test authenticates first, then interacts with the protected feed.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Feed renders its key elements after login</li>
 *   <li>Clicking "Create post" navigates to the create form</li>
 *   <li>Search accepts input and stays on the feed</li>
 *   <li>Unauthenticated access is redirected to /login</li>
 * </ul>
 */
@DisplayName("Post Feed Tests")
class PostFeedTest extends SetUp {

    private PostFeedPage postFeedPage;

    @BeforeEach
    void loginAndLandOnFeed() {
        navigateTo(Routes.LOGIN);
        postFeedPage = new LoginPage(driver)
                .loginAs(ConfigReader.getValidEmail(), ConfigReader.getValidPassword());
    }

    // ------------------------------------------------------------------ rendering

    @Test
    @DisplayName("Post feed renders search bar, create post button, and navbar logo")
    void postFeedRendersKeyElements() {
        // Assert
        assertTrue(postFeedPage.isSearchInputDisplayed(),    "Search input should be visible");
        assertTrue(postFeedPage.isCreatePostButtonDisplayed(), "Create post button should be visible");
        assertTrue(postFeedPage.isNavbarLogoDisplayed(),     "Navbar logo should be visible");
    }

    // ------------------------------------------------------------------ navigation

    @Test
    @DisplayName("Clicking 'Create post' navigates to the create post form")
    void clickingCreatePostOpensCreateForm() {
        // Act
        CreatePostPage createPostPage = postFeedPage.clickCreatePost();

        // Assert
        assertTrue(driver.getCurrentUrl().contains(Routes.CREATE),
                "URL should contain /create after clicking the button");
        assertTrue(createPostPage.isPostTitleInputDisplayed(),
                "Post title input should be visible on the create form");
    }

    // ------------------------------------------------------------------ search

    @Test
    @DisplayName("Submitting a search query keeps the user on the feed")
    void searchQueryKeepsUserOnFeed() {
        // Act
        postFeedPage.searchFor(PostTestData.SEARCH_QUERY);

        // Assert – result is rendered inline; user stays on the same page
        assertFalse(postFeedPage.getCurrentUrl().contains(Routes.LOGIN),
                "User should not be redirected to login after a search");
    }

    // ------------------------------------------------------------------ auth guard

    @Test
    @DisplayName("Accessing the feed directly without a session redirects to /login")
    void unauthenticatedAccessRedirectsToLogin() {
        // Arrange – clear cookies to simulate a logged-out state
        driver.manage().deleteAllCookies();

        // Act
        navigateTo(Routes.FEED);

        // Assert
        assertTrue(driver.getCurrentUrl().contains(Routes.LOGIN),
                "Unauthenticated users must be redirected to /login");
    }
}
