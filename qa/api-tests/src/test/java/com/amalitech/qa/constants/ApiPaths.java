package com.amalitech.qa.constants;

/**
 * Central repository of all API endpoint paths used across the test suite.
 * Use the static constants for fixed paths and the helper methods for paths
 * that require dynamic segments (IDs).
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------
    public static final String AUTH_REGISTER = "/api/auth/register";
    public static final String AUTH_LOGIN    = "/api/auth/login";

    // -------------------------------------------------------------------------
    // Posts
    // -------------------------------------------------------------------------
    public static final String POSTS        = "/api/posts";
    public static final String POSTS_SEARCH = "/api/posts/search";

    public static String postById(long id) {
        return "/api/posts/" + id;
    }

    public static String postComments(long postId) {
        return "/api/posts/" + postId + "/comments";
    }

    // -------------------------------------------------------------------------
    // Comments
    // -------------------------------------------------------------------------
    public static String commentById(long id) {
        return "/api/comments/" + id;
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------
    public static final String CATEGORIES = "/api/categories";
}
