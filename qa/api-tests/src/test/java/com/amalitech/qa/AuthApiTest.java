package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class AuthApiTest {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test(priority = 1)
    public void testRegisterNewUser() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"QA Tester\",\"email\":\"qa@test.com\",\"password\":\"test1234\"}")
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("email", equalTo("qa@test.com"));
    }

    @Test(priority = 2)
    public void testLoginExistingUser() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@amalitech.com\",\"password\":\"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("role", equalTo("ADMIN"));
    }

    @Test(priority = 3)
    public void testLoginInvalidCredentials() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"wrong@test.com\",\"password\":\"wrong\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(anyOf(is(401), is(500)));
    }

    // TODO: Add more auth tests
    // - testRegisterDuplicateEmail
    // - testRegisterInvalidEmail
    // - testAccessProtectedEndpoint
}
