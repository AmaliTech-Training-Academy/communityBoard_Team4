package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.builders.CommentRequestBuilder;
import com.amalitech.qa.builders.PostRequestBuilder;
import com.amalitech.qa.dto.CommentRequest;
import com.amalitech.qa.dto.PostRequest;
import com.amalitech.qa.utils.ConfigManager;
import com.amalitech.qa.utils.TokenManager;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Feature("Comments")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Comment API Tests")
public class CommentApiTest extends SetUp {

    private static Long postId;
    private static Long commentId;
    private static final List<Long> commentsToCleanUp = new ArrayList<>();
        private static String authorToken;
        private static String otherUserToken;
        private static String adminToken;

        private static String registerAndGetTokenWithRetry(String name, String email, String password) {
                String requestBody = String.format(
                                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                                name, email, password
                );

                RuntimeException lastException = null;
                int maxAttempts = 3;
                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        Response response = RestAssured.given()
                                        .contentType("application/json")
                                        .header("Accept", "application/json")
                                        .body(requestBody)
                                        .when()
                                        .post("/api/auth/register");

                        if (response.getStatusCode() == 201) {
                                return response.jsonPath().getString("token");
                        }

                        if (response.getStatusCode() != 502 || attempt == maxAttempts) {
                                lastException = new RuntimeException(
                                                "Failed to register user after " + attempt + " attempt(s). Status: "
                                                                + response.getStatusCode() + ", Response: " + response.getBody().asString()
                                );
                                break;
                        }

                        try {
                                Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Interrupted while retrying registration", ex);
                        }
                }

                throw lastException;
        }

        private static String tryGetAdminToken() {
                try {
                        return TokenManager.getAdminToken();
                } catch (RuntimeException ex) {
                        return null;
                }
        }

    @BeforeAll
    static void createTestFixtures() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();

        String authorEmail = "comment-author-" + UUID.randomUUID() + "@test.com";
        String otherEmail = "comment-other-" + UUID.randomUUID() + "@test.com";
        authorToken = registerAndGetTokenWithRetry("Comment Author", authorEmail, "password123");
        otherUserToken = registerAndGetTokenWithRetry("Comment Other User", otherEmail, "password123");
        adminToken = tryGetAdminToken();

        // Create a post using the author token (self-seeded test user)
        PostRequest post = new PostRequestBuilder()
                .withTitle("Comment Test Post")
                .build();
        postId = RestAssured.given()
                .contentType("application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authorToken)
                .body(post)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        // Create an initial comment using the same author token
        CommentRequest comment = new CommentRequestBuilder().build();
        commentId = RestAssured.given()
                .contentType("application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authorToken)
                .body(comment)
                .when()
                .post("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    @AfterAll
    static void cleanUp() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();
                String cleanupToken = adminToken != null ? adminToken : authorToken;

        // Delete any comments created during parameterized tests
        for (Long id : commentsToCleanUp) {
            RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                                        .header("Authorization", "Bearer " + cleanupToken)
                    .when()
                    .delete("/api/comments/" + id);
        }

        // Delete the test post (cascades to any remaining comments, including commentId)
        if (postId != null) {
            RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                                        .header("Authorization", "Bearer " + cleanupToken)
                    .when()
                    .delete("/api/posts/" + postId);
        }
    }

    // -------------------------------------------------------------------------
    // Read Comments
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @Story("Read Comments")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Get comments for valid post ID returns 200")
    void getComments_validPostId_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", equalTo(1));
    }

    @Test
    @Order(2)
    @Story("Read Comments")
    @Severity(SeverityLevel.NORMAL)
        @DisplayName("Get comments for invalid post ID returns 200 with empty result")
    void getComments_invalidPostId_returns404() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/posts/999999999/comments")
                .then()
                                .statusCode(200)
                                .body("empty", equalTo(true));
    }

    // -------------------------------------------------------------------------
    // Add Comment
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] valid comment added")
    @Order(3)
    @MethodSource("com.amalitech.qa.testdata.CommentTestData#validCommentRequests")
    @Story("Add Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Add comment with valid content returns 201")
    void addComment_validContent_returns201(CommentRequest request) {
        Long id = RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", equalTo(request.getContent()))
                .extract()
                .jsonPath()
                .getLong("id");
        commentsToCleanUp.add(id);
    }

    @ParameterizedTest(name = "[{index}] empty content rejected")
    @Order(4)
    @MethodSource("com.amalitech.qa.testdata.CommentTestData#emptyCommentRequests")
    @Story("Add Comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Add comment with empty content returns 400")
    void addComment_emptyContent_returns400(CommentRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "[{index}] over-limit content rejected")
    @Order(5)
    @MethodSource("com.amalitech.qa.testdata.CommentTestData#overLimitCommentRequests")
    @Story("Add Comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Add comment with over-limit content returns 400")
    void addComment_overLimitContent_returns400(CommentRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @Story("Add Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Add comment without authentication returns 401")
    void addComment_noAuth_returns401() {
        CommentRequest request = new CommentRequestBuilder().build();
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/posts/" + postId + "/comments")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    @Story("Add Comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Add comment to non-existent post returns 404")
    void addComment_invalidPostId_returns404() {
        CommentRequest request = new CommentRequestBuilder().build();
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts/999999999/comments")
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Update Comment
    // -------------------------------------------------------------------------

    @Test
    @Order(8)
    @Story("Update Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update comment as author returns 200")
    void updateComment_asAuthor_returns200() {
        CommentRequest update = new CommentRequestBuilder()
                .withContent("Updated by the author.")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + authorToken)
                .body(update)
                .when()
                .put("/api/comments/" + commentId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Updated by the author."));
    }

    @Test
    @Order(9)
    @Story("Update Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update comment as admin returns 200")
    void updateComment_asAdmin_returns200() {
        Assumptions.assumeTrue(adminToken != null, "Admin credentials unavailable in this environment");
        CommentRequest update = new CommentRequestBuilder()
                .withContent("Updated by admin.")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(update)
                .when()
                .put("/api/comments/" + commentId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Updated by admin."));
    }

    @Test
    @Order(10)
    @Story("Update Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update comment as a different user returns 403")
    void updateComment_asOtherUser_returns403() {
        CommentRequest update = new CommentRequestBuilder()
                .withContent("Unauthorized update attempt.")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + otherUserToken)
                .body(update)
                .when()
                .put("/api/comments/" + commentId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(11)
    @Story("Update Comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Update comment with blank content returns 400")
    void updateComment_blankContent_returns400() {
        CommentRequest blank = new CommentRequestBuilder()
                .withContent("")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + authorToken)
                .body(blank)
                .when()
                .put("/api/comments/" + commentId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    @Story("Update Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update comment without authentication returns 401")
    void updateComment_noAuth_returns401() {
        CommentRequest update = new CommentRequestBuilder().build();
        RestAssured.given()
                .spec(requestSpec)
                .body(update)
                .when()
                .put("/api/comments/" + commentId)
                .then()
                .statusCode(401);
    }

    // -------------------------------------------------------------------------
    // Delete Comment  (must run last — ordering matters)
    // -------------------------------------------------------------------------

    @Test
    @Order(13)
    @Story("Delete Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Delete comment as a different user returns 403")
    void deleteComment_asOtherUser_returns403() {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + otherUserToken)
                .when()
                .delete("/api/comments/" + commentId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(14)
    @Story("Delete Comment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Delete comment as author returns 204")
    void deleteComment_asAuthor_returns204() {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .when()
                .delete("/api/comments/" + commentId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(15)
    @Story("Delete Comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Delete already-deleted comment returns 404")
    void deleteComment_alreadyDeleted_returns404() {
        String tokenForDelete = adminToken != null ? adminToken : authorToken;
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + tokenForDelete)
                .when()
                .delete("/api/comments/" + commentId)
                .then()
                .statusCode(404);
    }
}
