package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class PostApiTest {

    private String authToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";
        // Login to get token
        authToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@amalitech.com\",\"password\":\"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .extract().path("token");
    }

    @Test
    public void testGetAllPosts() {
        given()
        .when()
            .get("/api/posts")
        .then()
            .statusCode(200)
            .body("content", notNullValue());
    }

    @Test
    public void testCreatePost() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + authToken)
            .body("{\"title\":\"QA Test Post\",\"content\":\"Automated test content\",\"categoryId\":1}")
        .when()
            .post("/api/posts")
        .then()
            .statusCode(200)
            .body("title", equalTo("QA Test Post"));
    }

    // TODO: Add more post tests
    // - testUpdatePost
    // - testDeletePost
    // - testCreatePostUnauthorized
    // - testGetPostById
}
