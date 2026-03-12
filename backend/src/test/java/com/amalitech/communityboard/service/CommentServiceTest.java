package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.CommentRequest;
import com.amalitech.communityboard.dto.CommentResponse;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.Comment;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @InjectMocks CommentService commentService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private User user(long id, Role role) {
        return User.builder()
                .id(id).name("User" + id).email("user" + id + "@test.com")
                .password("pw").role(role).build();
    }

    private Comment comment(long commentId, User author) {
        Post post = Post.builder().id(1L).build();
        return Comment.builder()
                .id(commentId).content("hello").post(post).author(author).build();
    }

    // ── Test 2: user trying to delete ANOTHER user's comment → 403 ────────

    @Test
    void deleteComment_byNonOwnerUser_throwsUnauthorized() {
        User owner   = user(1L, Role.USER);
        User stranger = user(2L, Role.USER);
        Comment c = comment(10L, owner);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteComment(10L, stranger))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized");

        verify(commentRepository, never()).delete(any());
    }

    // ── Test 3: admin deletes ANY comment → 204 (no exception) ────────────

    @Test
    void deleteComment_byAdmin_deletesAnyComment() {
        User owner = user(1L, Role.USER);
        User admin  = user(99L, Role.ADMIN);
        Comment c = comment(10L, owner);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThatNoException().isThrownBy(() -> commentService.deleteComment(10L, admin));

        verify(commentRepository).delete(c);
    }

    // ── Test 4: author deletes their own comment → 204 (no exception) ─────

    @Test
    void deleteComment_byAuthor_deletesOwnComment() {
        User owner = user(1L, Role.USER);
        Comment c = comment(10L, owner);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThatNoException().isThrownBy(() -> commentService.deleteComment(10L, owner));

        verify(commentRepository).delete(c);
    }

    // ── Test 5: comment not found → 404 ───────────────────────────────────

    @Test
    void deleteComment_commentNotFound_throwsResourceNotFound() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(999L, user(1L, Role.USER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(commentRepository, never()).delete(any());
    }

    // ── getCommentsByPost — pagination ────────────────────────────────────

    @Test
    void getCommentsByPost_returnsPaginatedComments() {
        User author = user(1L, Role.USER);
        Comment c = Comment.builder()
                .id(1L).content("hello").post(Post.builder().id(1L).build())
                .author(author).createdAt(LocalDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 20);

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(c), pageable, 1));

        Page<CommentResponse> result = commentService.getCommentsByPost(1L, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("hello");
        assertThat(result.getContent().get(0).getAuthorName()).isEqualTo("User1");
    }

    @Test
    void getCommentsByPost_emptyPost_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(42L, pageable))
                .thenReturn(Page.empty(pageable));

        Page<CommentResponse> result = commentService.getCommentsByPost(42L, 0, 20);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ── createComment ─────────────────────────────────────────────────────

    @Test
    void createComment_postExists_savesAndReturnsResponse() {
        User author = user(1L, Role.USER);
        Post post   = Post.builder().id(1L).author(author).build();
        Comment saved = Comment.builder()
                .id(5L).content("my comment").post(post)
                .author(author).createdAt(LocalDateTime.now()).build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse resp = commentService.createComment(1L, new CommentRequest("my comment"), author);

        assertThat(resp.getId()).isEqualTo(5L);
        assertThat(resp.getContent()).isEqualTo("my comment");
        assertThat(resp.getAuthorName()).isEqualTo("User1");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_postNotFound_throws404() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(99L, new CommentRequest("hi"), user(1L, Role.USER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(commentRepository, never()).save(any());
    }
}
