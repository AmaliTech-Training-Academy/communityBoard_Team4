package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.PostResponse;
import com.amalitech.communityboard.exception.BadRequestException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostService.searchPosts() — Phase 5 Search & Filtering.
 *
 * Covers:
 *  - All filters null (pass-through)
 *  - Category filter (valid + invalid string)
 *  - Keyword filter (trims whitespace, null treated as "no filter")
 *  - Blank keyword treated as null (no filter)
 *  - Date range filter passed through to repository
 *  - Empty results returned as empty Page (not exception)
 */
@ExtendWith(MockitoExtension.class)
class PostSearchServiceTest {

    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @InjectMocks PostService postService;

    private User author() {
        return User.builder().id(1L).name("Alice").email("alice@test.com")
                .password("pw").role(Role.USER).build();
    }

    private Post post(long id, Category category) {
        return Post.builder()
                .id(id).title("Title " + id).body("Body " + id)
                .category(category).author(author())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── no filters → all posts returned ────────────────────────────────────

    @Test
    void searchPosts_noFilters_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Post p = post(1L, Category.NEWS);
        when(postRepository.searchPosts(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));
        when(commentRepository.countByPostId(1L)).thenReturn(0L);

        Page<PostResponse> result = postService.searchPosts(null, null, null, null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Title 1");
    }

    // ── valid category filter ───────────────────────────────────────────────

    @Test
    void searchPosts_validCategory_parsedAndPassedToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Post p = post(2L, Category.EVENT);
        when(postRepository.searchPosts(eq(Category.EVENT), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));
        when(commentRepository.countByPostId(2L)).thenReturn(1L);

        Page<PostResponse> result = postService.searchPosts("event", null, null, null, 0, 10);

        assertThat(result.getContent().get(0).getCategory()).isEqualTo("EVENT");
        verify(postRepository).searchPosts(Category.EVENT, null, null, null, pageable);
    }

    // ── invalid category string → BadRequestException ──────────────────────

    @Test
    void searchPosts_invalidCategory_throwsBadRequest() {
        assertThatThrownBy(() -> postService.searchPosts("BOGUS", null, null, null, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("BOGUS");

        verify(postRepository, never()).searchPosts(any(), any(), any(), any(), any());
    }

    // ── keyword filter ─────────────────────────────────────────────────────

    @Test
    void searchPosts_keywordFilter_passedToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.searchPosts(null, null, null, "community", pageable))
                .thenReturn(Page.empty(pageable));

        Page<PostResponse> result = postService.searchPosts(null, null, null, "community", 0, 10);

        assertThat(result.getTotalElements()).isZero();
        verify(postRepository).searchPosts(null, null, null, "community", pageable);
    }

    @Test
    void searchPosts_blankKeyword_treatedAsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.searchPosts(null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        postService.searchPosts(null, null, null, "   ", 0, 10);

        // blank keyword normalised to null — repository receives null, not whitespace
        verify(postRepository).searchPosts(null, null, null, null, pageable);
    }

    // ── date range filter ───────────────────────────────────────────────────

    @Test
    void searchPosts_dateRangeFilter_passedToRepository() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2026, 3, 1, 23, 59);
        Pageable pageable = PageRequest.of(0, 10);

        when(postRepository.searchPosts(null, start, end, null, pageable))
                .thenReturn(Page.empty(pageable));

        postService.searchPosts(null, start, end, null, 0, 10);

        verify(postRepository).searchPosts(null, start, end, null, pageable);
    }

    // ── empty results ───────────────────────────────────────────────────────

    @Test
    void searchPosts_noMatches_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(postRepository.searchPosts(any(), any(), any(), any(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<PostResponse> result = postService.searchPosts(null, null, null, "no-match", 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
