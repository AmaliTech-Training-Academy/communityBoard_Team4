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
 * Phase 3 Post Management — security integration tests.
 *
 * Verifies that write endpoints (POST / PUT / DELETE) are protected:
 * requests without a Bearer token must receive HTTP 401 with a JSON body.
 *
 * Uses the full Spring context with H2 in-memory DB
 * (wired via src/test/resources/application.properties, Flyway disabled).
 * No mocks needed — the security filter rejects the request before any service is invoked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    private static final String POST_BODY =
            "{\"title\":\"Test\",\"body\":\"Body text\",\"category\":\"NEWS\"}";

    // ── POST /api/posts without token → 401 ──────────────────────────────────

    @Test
    void createPost_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── PUT /api/posts/{id} without token → 401 ──────────────────────────────

    @Test
    void updatePost_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── DELETE /api/posts/{id} without token → 401 ───────────────────────────

    @Test
    void deletePost_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── GET /api/posts is PUBLIC — no auth needed → 200 ─────────────────────

    @Test
    void getAllPosts_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    // ── GET /api/posts/{id} is PUBLIC — no auth needed → 404 (no data in H2) ──

    @Test
    void getPostById_withoutToken_returns404NotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound());
    }
}
