package com.amalitech.qa.tests.search;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.builders.PostRequestBuilder;
import com.amalitech.qa.constants.ApiPaths;
import com.amalitech.qa.constants.TestConstants;
import com.amalitech.qa.tests.post.PostRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@Feature("Search")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Search API Tests")
public class SearchApiTest extends SetUp {

    private static final List<Long> searchPostIds = new ArrayList<>();
    private static final String[] CATEGORIES = {
            TestConstants.CATEGORY_NEWS,
            TestConstants.CATEGORY_EVENT,
            TestConstants.CATEGORY_DISCUSSION,
            TestConstants.CATEGORY_ALERT
    };
    private static String fixtureOwnerToken;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static String startOfYearIsoDateTime() {
        return LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 0, 0, 0).format(ISO_DATE_TIME);
    }

    private static String endOfYearIsoDateTime() {
        return LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59).format(ISO_DATE_TIME);
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
                    .post(ApiPaths.AUTH_REGISTER);

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
    static void createSearchFixtures() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();
        String ownerEmail = "search-owner-" + UUID.randomUUID() + "@test.com";
        fixtureOwnerToken = registerAndGetTokenWithRetry(TestConstants.SEARCH_FIXTURE_OWNER_NAME, ownerEmail, TestConstants.DEFAULT_PASSWORD);
        for (String category : CATEGORIES) {
            PostRequest request = new PostRequestBuilder()
                    .withTitle(TestConstants.SEARCH_POST_TITLE_PREFIX + category)
                    .withBody(TestConstants.SEARCH_POST_BODY_PREFIX + category)
                    .withCategory(category)
                    .build();
            Long id = RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + fixtureOwnerToken)
                    .body(request)
                    .when()
                    .post(ApiPaths.POSTS)
                    .then()
                    .statusCode(201)
                    .extract()
                    .jsonPath()
                    .getLong("id");
            searchPostIds.add(id);
        }
    }

    @AfterAll
    static void cleanUpSearchFixtures() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();
        for (Long id : searchPostIds) {
            RestAssured.given()
                    .contentType("application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + fixtureOwnerToken)
                    .when()
                    .delete(ApiPaths.postById(id));
        }
    }

    @Test
    @Order(1)
    @Story("Search Posts")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Search with no parameters returns 200")
    void search_noParams_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @ParameterizedTest(name = "[{index}] category={0}")
    @Order(2)
    @MethodSource("com.amalitech.qa.testdata.PostTestData#validSearchCategories")
    @Story("Search Posts")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Search by valid category returns 200 with matching posts")
    void search_byCategory_returns200(String category) {
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("category", category)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("totalElements", greaterThan(0))
                .body("content.category", everyItem(equalTo(category)));
    }

    @Test
    @Order(3)
    @Story("Search Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Search by invalid category returns 400")
    void search_byInvalidCategory_returns400() {
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("category", TestConstants.INVALID_CATEGORY)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @Story("Search Posts")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Search by keyword returns 200 with matching posts")
    void search_byKeyword_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("keyword", TestConstants.SEARCH_KEYWORD)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("totalElements", greaterThan(0));
    }

    @Test
    @Order(5)
    @Story("Search Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Search by date range returns 200")
    void search_byDateRange_returns200() {
        String startDate = startOfYearIsoDateTime();
        String endDate = endOfYearIsoDateTime();
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @Order(6)
    @Story("Search Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Search with category, keyword and date range combined returns 200")
    void search_allFilters_returns200() {
        String startDate = startOfYearIsoDateTime();
        String endDate = endOfYearIsoDateTime();
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("category", TestConstants.CATEGORY_NEWS)
                .queryParam("keyword", TestConstants.SEARCH_KEYWORD_NEWS)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @Order(7)
    @Story("Search Posts")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Search with non-matching keyword returns 200 with empty page")
    void search_noMatch_returnsEmptyPage() {
        RestAssured.given()
                .spec(requestSpec)
                .queryParam("keyword", TestConstants.SEARCH_NO_MATCH_KEYWORD)
                .when()
                .get(ApiPaths.POSTS_SEARCH)
                .then()
                .statusCode(200)
                .body("empty", equalTo(true));
    }
}
