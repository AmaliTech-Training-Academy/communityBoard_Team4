package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.PostRequest;
import com.amalitech.communityboard.dto.PostResponse;
import com.amalitech.communityboard.exception.BadRequestException;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.CommentRepository;
import com.amalitech.communityboard.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostService — Phase 3 Post Management.
 *
 * Covers:
 *  - getAllPosts: returns paginated responses
 *  - getPostById: returns response; throws 404 when missing
 *  - createPost: persists and returns PostResponse
 *  - updatePost: author can update; admin can update any; non-owner/non-admin → 403; missing post → 404
 *  - deletePost: author can delete; admin can delete any; non-owner/non-admin → 403; missing post → 404
 *  - parseCategory: valid + invalid input
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock NewPostEmailNotificationService newPostEmailNotificationService;
    @InjectMocks PostService postService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private User user(long id, Role role) {
        return User.builder()
                .id(id).name("User" + id).email("u" + id + "@test.com")
                .password("pw").role(role).build();
    }

    private Post post(long postId, User author) {
        return Post.builder()
                .id(postId)
                .title("Title " + postId)
                .body("Body " + postId)
                .category(Category.NEWS)
                .author(author)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PostRequest request(String category) {
        return new PostRequest("My Title", "My body text", category);
    }

    // ── getAllPosts ───────────────────────────────────────────────────────────

    @Test
    void getAllPosts_returnsPagedResponses() {
        User author = user(1L, Role.USER);
        Post p = post(1L, author);
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));
        when(commentRepository.countByPostId(1L)).thenReturn(2L);

        var result = postService.getAllPosts(0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Title 1");
        assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(2);
    }

    // ── getPostById ───────────────────────────────────────────────────────────

    @Test
    void getPostById_found_returnsResponse() {
        User author = user(1L, Role.USER);
        Post p = post(5L, author);
        when(postRepository.findById(5L)).thenReturn(Optional.of(p));
        when(commentRepository.countByPostId(5L)).thenReturn(0L);

        PostResponse resp = postService.getPostById(5L);

        assertThat(resp.getId()).isEqualTo(5L);
        assertThat(resp.getAuthorName()).isEqualTo("User1");
        assertThat(resp.getCategory()).isEqualTo("NEWS");
    }

    @Test
    void getPostById_notFound_throws404() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createPost ────────────────────────────────────────────────────────────

    @Test
    void createPost_validRequest_savesAndReturnsResponse() {
        User author = user(1L, Role.USER);
        Post saved = post(10L, author);

        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(commentRepository.countByPostId(10L)).thenReturn(0L);

        PostResponse resp = postService.createPost(request("NEWS"), author);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getCategory()).isEqualTo("NEWS");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_invalidCategory_throwsBadRequest() {
        User author = user(1L, Role.USER);

        assertThatThrownBy(() -> postService.createPost(request("INVALID"), author))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("INVALID");

        verify(postRepository, never()).save(any());
    }

    // ── updatePost ────────────────────────────────────────────────────────────

    @Test
    void updatePost_byAuthor_succeeds() {
        User author = user(1L, Role.USER);
        Post existing = post(1L, author);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenReturn(existing);
        when(commentRepository.countByPostId(1L)).thenReturn(0L);

        assertThatNoException().isThrownBy(
                () -> postService.updatePost(1L, request("EVENT"), author));

        verify(postRepository).save(existing);
    }

    @Test
    void updatePost_byAdmin_canUpdateAnyPost() {
        User owner = user(1L, Role.USER);
        User admin = user(2L, Role.ADMIN);
        Post existing = post(1L, owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenReturn(existing);
        when(commentRepository.countByPostId(1L)).thenReturn(0L);

        assertThatNoException().isThrownBy(
                () -> postService.updatePost(1L, request("DISCUSSION"), admin));
    }

    @Test
    void updatePost_byNonOwnerUser_throwsUnauthorized() {
        User owner   = user(1L, Role.USER);
        User stranger = user(2L, Role.USER);
        Post existing = post(1L, owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> postService.updatePost(1L, request("NEWS"), stranger))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized");

        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_postNotFound_throws404() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(99L, request("NEWS"), user(1L, Role.USER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── deletePost ────────────────────────────────────────────────────────────

    @Test
    void deletePost_byAuthor_succeeds() {
        User author = user(1L, Role.USER);
        Post existing = post(1L, author);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatNoException().isThrownBy(() -> postService.deletePost(1L, author));
        verify(postRepository).delete(existing);
    }

    @Test
    void deletePost_byAdmin_canDeleteAnyPost() {
        User owner = user(1L, Role.USER);
        User admin = user(2L, Role.ADMIN);
        Post existing = post(1L, owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatNoException().isThrownBy(() -> postService.deletePost(1L, admin));
        verify(postRepository).delete(existing);
    }

    @Test
    void deletePost_byNonOwnerUser_throwsUnauthorized() {
        User owner    = user(1L, Role.USER);
        User stranger = user(2L, Role.USER);
        Post existing = post(1L, owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> postService.deletePost(1L, stranger))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized");

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    void deletePost_postNotFound_throws404() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(99L, user(1L, Role.USER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── parseCategory ─────────────────────────────────────────────────────────

    @Test
    void parseCategory_validLowercase_returnsEnum() {
        assertThat(postService.parseCategory("alert")).isEqualTo(Category.ALERT);
        assertThat(postService.parseCategory("Event")).isEqualTo(Category.EVENT);
    }

    @Test
    void parseCategory_invalid_throwsBadRequest() {
        assertThatThrownBy(() -> postService.parseCategory("UNKNOWN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
