package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests for AdminUserController.
 *
 * Verifies:
 *  - All four endpoints return HTTP 401 when no Bearer token is provided.
 *  - A non-admin user receives HTTP 403 on any admin endpoint.
 *  - An authenticated ADMIN can list users (200), get a missing user (404),
 *    delete a missing user (404), and is blocked from modifying their own
 *    account (400 business-rule enforcement).
 *
 * Uses the full Spring context with H2 in-memory DB.
 * The `authentication()` post-processor injects a real User entity as the
 * principal so that @AuthenticationPrincipal User currentUser resolves correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    /** Builds an Authentication whose principal is our custom User entity (id=99, ADMIN). */
    private static Authentication adminAuth() {
        User admin = User.builder()
                .id(99L).email("admin@test.com").name("Admin").password("pw")
                .role(Role.ADMIN).build();
        return new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // ── HTTP 401 — no Authorization header ───────────────────────────────────

    @Test
    void getAllUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getUserById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void updateUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deleteUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── HTTP 403 — authenticated but not ADMIN ────────────────────────────────

    @Test
    void getAllUsers_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user("u@test.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ── Authenticated ADMIN — functional tests ────────────────────────────────

    @Test
    void getAllUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getUserById_asAdmin_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/users/9999")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_asAdmin_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/users/9999")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_asAdmin_selfUpdate_returns400() throws Exception {
        // The service rejects attempts to modify the calling admin's own account.
        mockMvc.perform(put("/api/admin/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}")
                        .with(authentication(adminAuth())))
                .andExpect(status().isBadRequest());
    }
}
