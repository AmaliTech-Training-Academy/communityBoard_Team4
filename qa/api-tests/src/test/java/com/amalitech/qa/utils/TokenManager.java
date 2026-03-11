package com.amalitech.qa.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * TokenManager handles JWT token generation and caching for test authentication
 * Provides convenient static methods to obtain tokens for different user roles
 */
public class TokenManager {
    private static String adminToken;
    private static String userToken;
    private static String otherUserToken;

    /**
     * Get a cached admin token, or generate a new one via login
     * Token is cached to avoid repeated login calls
     *
     * @return JWT token for admin user
     */
    public static String getAdminToken() {
        if (adminToken == null || adminToken.isEmpty()) {
            adminToken = login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
        }
        return adminToken;
    }

    /**
     * Get a cached regular user token, or generate a new one via login
     * Token is cached to avoid repeated login calls
     *
     * @return JWT token for regular user
     */
    public static String getUserToken() {
        if (userToken == null || userToken.isEmpty()) {
            userToken = login(ConfigManager.getUserEmail(), ConfigManager.getUserPassword());
        }
        return userToken;
    }

    /**
     * Get a cached other user token, or generate a new one via login
     * Token is cached to avoid repeated login calls
     *
     * @return JWT token for other user
     */
    public static String getOtherUserToken() {
        if (otherUserToken == null || otherUserToken.isEmpty()) {
            otherUserToken = login(ConfigManager.getOtherUserEmail(), ConfigManager.getOtherUserPassword());
        }
        return otherUserToken;
    }

    /**
     * Register a new user and return the JWT token from the response
     *
     * @param name User's full name
     * @param email User's email
     * @param password User's password
     * @return JWT token from registration response
     */
    public static String registerAndGetToken(String name, String email, String password) {
        String requestBody = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                name, email, password
        );

        Response response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/register");

        if (response.getStatusCode() != 201) {
            throw new RuntimeException(
                    "Failed to register user. Status: " + response.getStatusCode() +
                            ", Response: " + response.getBody().asString()
            );
        }

        return response.jsonPath().getString("token");
    }

    /**
     * Clear all cached tokens
     * Call this method if tokens are invalidated (e.g., after user logout or deletion)
     */
    public static void clearTokens() {
        adminToken = null;
        userToken = null;
        otherUserToken = null;
    }

    /**
     * Private method: Login with email and password, return JWT token
     *
     * @param email User email
     * @param password User password
     * @return JWT token
     */
    private static String login(String email, String password) {
        String requestBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email, password
        );

        Response response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/login");

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Failed to login. Status: " + response.getStatusCode() +
                            ", Response: " + response.getBody().asString()
            );
        }

        return response.jsonPath().getString("token");
    }
}
