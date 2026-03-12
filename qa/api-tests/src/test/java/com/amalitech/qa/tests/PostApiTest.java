package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.builders.PostRequestBuilder;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Feature("Posts")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Post API Tests")
public class PostApiTest extends SetUp {

    private static Long postId;
    private static final List<Long> postsToCleanUp = new ArrayList<>();
        private static String authorToken;
        private static String otherUserToken;
        private static String adminToken;

        private static String tryGetAdminToken() {
                try {
                        return TokenManager.getAdminToken();
                } catch (RuntimeException ex) {
                        return null;
                }
        }

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

    @BeforeAll
    static void createTestPost() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();

                String authorEmail = "post-author-" + UUID.randomUUID() + "@test.com";
                String otherEmail = "post-other-" + UUID.randomUUID() + "@test.com";
                authorToken = registerAndGetTokenWithRetry("Post Author", authorEmail, "password123");
                otherUserToken = registerAndGetTokenWithRetry("Post Other User", otherEmail, "password123");
                adminToken = tryGetAdminToken();

        PostRequest request = new PostRequestBuilder().build();
        postId = RestAssured.given()
                .contentType("application/json")
                .header("Accept", "application/json")
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
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
        for (Long id : postsToCleanUp) {
            RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                                        .header("Authorization", "Bearer " + cleanupToken)
                    .when()
                    .delete("/api/posts/" + id);
        }
        // postId itself is deleted by deletePost_asAuthor_returns204 (order 17);
        // @AfterAll does not need to re-delete it.
    }

    // -------------------------------------------------------------------------
    // Read Posts
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @Story("Read Posts")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Get all posts returns 200")
    void getAllPosts_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(2)
    @Story("Read Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Get all posts with pagination returns 200 and respects page size")
    void getAllPosts_withPagination_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("size", equalTo(5));
    }

    @Test
    @Order(3)
    @Story("Read Posts")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Get post by valid ID returns 200 with post data")
    void getPostById_validId_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("title", notNullValue())
                .body("body", notNullValue())
                .body("category", notNullValue());
    }

    @Test
    @Order(4)
    @Story("Read Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Get post by invalid ID returns 404")
    void getPostById_invalidId_returns404() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/posts/999999999")
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Create Post
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] valid post created")
    @Order(5)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#validPostRequests")
    @Story("Create Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Create post with valid data returns 201")
    void createPost_validData_returns201(PostRequest request) {
        Long id = RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo(request.getTitle()))
                .body("category", equalTo(request.getCategory()))
                .extract()
                .jsonPath()
                .getLong("id");
        postsToCleanUp.add(id);
    }

    @ParameterizedTest(name = "[{index}] empty title rejected")
    @Order(6)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#emptyTitlePostRequests")
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Create post with empty title returns 400")
    void createPost_emptyTitle_returns400(PostRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "[{index}] empty body rejected")
    @Order(7)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#emptyBodyPostRequests")
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Create post with empty body returns 400")
    void createPost_emptyBody_returns400(PostRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "[{index}] invalid category rejected")
    @Order(8)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#invalidCategoryPostRequests")
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Create post with invalid category returns 400")
    void createPost_invalidCategory_returns400(PostRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "[{index}] over-limit field rejected")
    @Order(9)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#overLimitPostRequests")
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Create post with over-limit fields returns 400")
    void createPost_overLimitFields_returns400(PostRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(10)
    @Story("Create Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Create post without authentication returns 401")
    void createPost_noAuth_returns401() {
        PostRequest request = new PostRequestBuilder().build();
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(401);
    }

    // -------------------------------------------------------------------------
    // Update Post
    // -------------------------------------------------------------------------

    @Test
    @Order(11)
    @Story("Update Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update post as author returns 200")
    void updatePost_asAuthor_returns200() {
        PostRequest update = new PostRequestBuilder()
                .withTitle("Updated by Author")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + authorToken)
                .body(update)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated by Author"));
    }

    @Test
    @Order(12)
    @Story("Update Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update post as admin returns 200")
    void updatePost_asAdmin_returns200() {
        Assumptions.assumeTrue(adminToken != null, "Admin credentials unavailable in this environment");
        PostRequest update = new PostRequestBuilder()
                .withTitle("Updated by Admin")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + adminToken)
                .body(update)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated by Admin"));
    }

    @Test
    @Order(13)
    @Story("Update Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update post as a different user returns 403")
    void updatePost_asOtherUser_returns403() {
        PostRequest update = new PostRequestBuilder()
                .withTitle("Unauthorized update attempt")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + otherUserToken)
                .body(update)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(14)
    @Story("Update Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Update post with invalid data returns 400")
    void updatePost_invalidData_returns400() {
        PostRequest invalidUpdate = new PostRequestBuilder()
                .withTitle("")
                .build();
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + authorToken)
                .body(invalidUpdate)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(15)
    @Story("Update Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Update post without authentication returns 401")
    void updatePost_noAuth_returns401() {
        PostRequest update = new PostRequestBuilder().build();
        RestAssured.given()
                .spec(requestSpec)
                .body(update)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(401);
    }

    // -------------------------------------------------------------------------
    // Delete Post  (must run last — ordering matters)
    // -------------------------------------------------------------------------

    @Test
    @Order(16)
    @Story("Delete Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Delete post as a different user returns 403")
    void deletePost_asOtherUser_returns403() {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + otherUserToken)
                .when()
                .delete("/api/posts/" + postId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(17)
    @Story("Delete Post")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Delete post as author returns 204")
    void deletePost_asAuthor_returns204() {
        RestAssured.given()
                .spec(requestSpec)
                                .header("Authorization", "Bearer " + authorToken)
                .when()
                .delete("/api/posts/" + postId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(18)
    @Story("Delete Post")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Delete already-deleted post returns 404")
    void deletePost_alreadyDeleted_returns404() {
        String tokenForDelete = adminToken != null ? adminToken : authorToken;
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + tokenForDelete)
                .when()
                .delete("/api/posts/" + postId)
                .then()
                .statusCode(404);
    }
}
