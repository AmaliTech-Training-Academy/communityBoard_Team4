package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@Feature("Categories")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Category API Tests")
public class CategoryApiTest extends SetUp {

    @Test
    @Order(1)
    @Story("Get Categories")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Get categories returns 200 with non-empty list")
    void getCategories_returns200() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(equalTo(4)));
    }

    @Test
    @Order(2)
    @Story("Get Categories")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Get categories contains all expected values")
    void getCategories_containsAllExpected() {
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasItems("NEWS", "EVENT", "DISCUSSION", "ALERT"));
    }

    @Test
    @Order(3)
    @Story("Get Categories")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Get categories returns exactly four items")
    void getCategories_countIsFour() {
        int count = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$")
                .size();

        org.junit.jupiter.api.Assertions.assertEquals(4, count,
                "Expected exactly 4 categories but got " + count);
    }
}
