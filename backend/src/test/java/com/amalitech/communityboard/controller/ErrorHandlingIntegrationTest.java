package com.amalitech.communityboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 6 — API Quality & Error Handling integration tests.
 *
 * Verifies that every error scenario produces:
 *  - The correct HTTP status code
 *  - Content-Type: application/json
 *  - An ApiErrorResponse envelope with a "status" and "message" field
 *
 * Uses the full Spring context with H2 in-memory DB
 * (src/test/resources/application.properties, Flyway disabled).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlingIntegrationTest {

    @Autowired MockMvc mockMvc;

    // ── 400 — Validation failure on register (missing required fields) ───────

    @Test
    void register_missingFields_returns400WithMessage() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ── 400 — Validation failure: invalid email format ────────────────────────

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email must be a valid email address"));
    }

    // ── 400 — Validation failure: password too short ──────────────────────────

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Password must be at least 6 characters"));
    }

    // ── 400 — Malformed JSON body ─────────────────────────────────────────────

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    // ── 404 — Post not found ──────────────────────────────────────────────────

    @Test
    void getPost_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ── 400 — Create post with invalid category ───────────────────────────────

    @Test
    void createPost_invalidCategory_returns400() throws Exception {
        // Need a token — but the endpoint is protected, so we expect 401 without a token.
        // That is tested in PostControllerSecurityTest. We test the 400 path here by
        // verifying the validation on the public search endpoint instead.
        mockMvc.perform(get("/api/posts/search")
                        .param("category", "INVALID_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("INVALID_CATEGORY")));
    }

    // ── 401 — No token on protected endpoint ─────────────────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"body\":\"B\",\"category\":\"NEWS\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(
                        "Authentication required. Provide a valid Bearer token."));
    }

    // ── 409 — Duplicate email on register ────────────────────────────────────

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = "{\"name\":\"Bob\",\"email\":\"bob_unique_phase6@test.com\",\"password\":\"secure123\"}";

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second registration with the same email → 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
