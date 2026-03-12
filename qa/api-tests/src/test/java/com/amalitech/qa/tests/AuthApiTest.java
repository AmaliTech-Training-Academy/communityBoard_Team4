package com.amalitech.qa.tests;

import com.amalitech.qa.base.SetUp;
import com.amalitech.qa.builders.RegisterRequestBuilder;
import com.amalitech.qa.dto.AuthRequest;
import com.amalitech.qa.dto.RegisterRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Feature("Authentication")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth API Tests")
public class AuthApiTest extends SetUp {

    @ParameterizedTest(name = "[{index}] valid registration succeeds")
    @Order(1)
    @MethodSource("com.amalitech.qa.testdata.AuthTestData#validRegisterRequests")
    @Story("Register")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Register with valid data returns 201")
    void register_validData_returns201(RegisterRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("role", equalTo("USER"));
    }

    @ParameterizedTest(name = "[{index}] invalid email rejected")
    @Order(2)
    @MethodSource("com.amalitech.qa.testdata.AuthTestData#invalidEmailRegisterRequests")
    @Story("Register")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Register with invalid email returns 400")
    void register_invalidEmail_returns400(RegisterRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest(name = "[{index}] short password rejected")
    @Order(3)
    @MethodSource("com.amalitech.qa.testdata.AuthTestData#shortPasswordRegisterRequests")
    @Story("Register")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Register with short password returns 400")
    void register_shortPassword_returns400(RegisterRequest request) {
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @Story("Register")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Register with duplicate email returns 409")
    void register_duplicateEmail_returns409() {
        String uniqueEmail = "duplicate-" + UUID.randomUUID() + "@test.com";
        RegisterRequest request = new RegisterRequestBuilder()
                .withEmail(uniqueEmail)
                .build();

        // First registration — should succeed
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        // Second registration with the same email — must conflict
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(409);
    }

    @ParameterizedTest(name = "[{index}] valid login succeeds after registration")
    @Order(5)
    @MethodSource("com.amalitech.qa.testdata.AuthTestData#validLoginRegistrationRequests")
    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Login with valid credentials returns 200")
    void login_validCredentials_returns200(RegisterRequest registerRequest) {
        RestAssured.given()
                .spec(requestSpec)
                .body(registerRequest)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("token", notNullValue());

        AuthRequest loginRequest = new AuthRequest(registerRequest.getEmail(), registerRequest.getPassword());

        RestAssured.given()
                .spec(requestSpec)
                .body(loginRequest)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("email", equalTo(loginRequest.getEmail()));
    }

    @ParameterizedTest(name = "[{index}] expects status {1}")
    @Order(6)
    @MethodSource("com.amalitech.qa.testdata.AuthTestData#invalidLoginRequests")
    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Login with invalid credentials returns expected error status")
    void login_invalidCredentials_returns4xx(AuthRequest request, int expectedStatus) {
        RestAssured.given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    @Order(7)
    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Login with blank fields returns 400")
    void login_blankFields_returns400() {
        RestAssured.given()
                .spec(requestSpec)
                .body(new AuthRequest("", ""))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(400);
    }
}
