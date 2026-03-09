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
 * Integration security test — scenario 1: no Bearer token → HTTP 401 JSON.
 *
 * Uses the full Spring context (@SpringBootTest) so the real SecurityConfig
 * and JwtAuthFilter are exercised. H2 in-memory DB is wired via
 * src/test/resources/application.properties (Flyway disabled, ddl-auto=create-drop).
 *
 * The security filter rejects the request before the controller is reached,
 * so no service/repository mocks are needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    // ── Scenario 1a: POST comment without token → 401 JSON ────────────────

    @Test
    void createComment_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(
                        "Authentication required. Provide a valid Bearer token."));
    }

    // ── Scenario 1b: DELETE comment without token → 401 JSON ─────────────

    @Test
    void deleteComment_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}

