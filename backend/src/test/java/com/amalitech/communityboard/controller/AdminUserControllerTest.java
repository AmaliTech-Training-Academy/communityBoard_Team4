package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.dto.AdminUpdateUserRequest;
import com.amalitech.communityboard.dto.UserResponse;
import com.amalitech.communityboard.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminUserController — verifies delegation to AdminUserService
 * and correct HTTP response codes.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock AdminUserService adminUserService;
    @InjectMocks AdminUserController adminUserController;

    private UserResponse sampleUser(long id) {
        return UserResponse.builder()
                .id(id)
                .name("User" + id)
                .email("user" + id + "@test.com")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllUsers_returns200WithList() {
        List<UserResponse> users = List.of(sampleUser(1L), sampleUser(2L));
        when(adminUserService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<UserResponse>> response = adminUserController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getUserById_returns200WithUser() {
        UserResponse user = sampleUser(3L);
        when(adminUserService.getUserById(3L)).thenReturn(user);

        ResponseEntity<UserResponse> response = adminUserController.getUserById(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3L);
    }

    @Test
    void updateUser_returns200WithUpdatedUser() {
        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setName("Updated");
        UserResponse updated = sampleUser(4L);
        when(adminUserService.updateUser(4L, req)).thenReturn(updated);

        ResponseEntity<UserResponse> response = adminUserController.updateUser(4L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminUserService).updateUser(4L, req);
    }

    @Test
    void deleteUser_returns204NoContent() {
        doNothing().when(adminUserService).deleteUser(7L);

        ResponseEntity<Void> response = adminUserController.deleteUser(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminUserService).deleteUser(7L);
    }
}
