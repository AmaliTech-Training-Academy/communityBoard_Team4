package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.AdminUpdateUserRequest;
import com.amalitech.communityboard.dto.UserResponse;
import com.amalitech.communityboard.exception.BadRequestException;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminUserService — admin CRUD operations on user accounts.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks AdminUserService adminUserService;

    // ── helpers ────────────────────────────────────────────────────────────

    private User user(long id, Role role) {
        return User.builder()
                .id(id)
                .name("User" + id)
                .email("user" + id + "@test.com")
                .password("hashed")
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── getAllUsers ─────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsMappedList() {
        User u1 = user(1L, Role.USER);
        User u2 = user(2L, Role.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = adminUserService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@test.com");
        assertThat(result.get(1).getRole()).isEqualTo("ADMIN");
    }

    // ── getUserById ─────────────────────────────────────────────────────────

    @Test
    void getUserById_found_returnsResponse() {
        User u = user(5L, Role.USER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        UserResponse result = adminUserService.getUserById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getEmail()).isEqualTo("user5@test.com");
    }

    @Test
    void getUserById_notFound_throws404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateUser ──────────────────────────────────────────────────────────

    @Test
    void updateUser_nameAndRole_updatesFields() {
        User u = user(3L, Role.USER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("NewName");
        req.setRole("ADMIN");

        UserResponse result = adminUserService.updateUser(3L, req);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        verify(userRepository).save(u);
    }

    @Test
    void updateUser_emailChange_success() {
        User u = user(4L, Role.USER);
        when(userRepository.findById(4L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setEmail("NEW@Test.com");

        UserResponse result = adminUserService.updateUser(4L, req);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUser_duplicateEmail_throwsBadRequest() {
        User u = user(6L, Role.USER);
        when(userRepository.findById(6L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setEmail("taken@test.com");

        assertThatThrownBy(() -> adminUserService.updateUser(6L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void updateUser_notFound_throws404() {
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateUser(77L, new AdminUpdateUserRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteUser ──────────────────────────────────────────────────────────

    @Test
    void deleteUser_exists_deletesById() {
        when(userRepository.existsById(10L)).thenReturn(true);

        adminUserService.deleteUser(10L);

        verify(userRepository).deleteById(10L);
    }

    @Test
    void deleteUser_notFound_throws404() {
        when(userRepository.existsById(55L)).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.deleteUser(55L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("55");
    }
}
