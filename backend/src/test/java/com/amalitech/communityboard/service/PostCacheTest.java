package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.PostRequest;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.CommentRepository;
import com.amalitech.communityboard.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * CB-211: Unit tests for Spring Cache behaviour on PostService.
 *
 * Verifies:
 *  - getAllPosts: second call with same params is served from cache (DB hit = 1)
 *  - getPostById: second call with same id is served from cache (DB hit = 1)
 *  - createPost:  evicts "posts" cache so next getAllPosts re-queries DB
 *  - updatePost:  evicts "post" + "posts" so next calls re-query DB
 *  - deletePost:  evicts "post" + "posts" so next calls re-query DB
 */
@SpringBootTest
class PostCacheTest {

    @MockBean PostRepository postRepository;
    @MockBean CommentRepository commentRepository;

    @Autowired PostService postService;
    @Autowired CacheManager cacheManager;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        // Clear caches before each test to ensure isolation
        cacheManager.getCache("posts").clear();
        cacheManager.getCache("post").clear();

        author = User.builder()
                .id(1L).name("Andy").email("andy@test.com")
                .password("encoded").role(Role.USER).build();

        post = Post.builder()
                .id(1L).title("Test Post").body("Body text")
                .category(Category.NEWS).author(author)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── CB-211 Task 3: @Cacheable on getAllPosts ──────────────────────────────

    @Test
    void getAllPosts_secondCallReturnsCachedResult_repositoryCalledOnce() {
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        postService.getAllPosts(0, 10);
        postService.getAllPosts(0, 10); // served from cache

        verify(postRepository, times(1)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void getAllPosts_differentPageParams_bothHitRepository() {
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        postService.getAllPosts(0, 10);
        postService.getAllPosts(1, 10); // different page → different cache key → DB hit

        verify(postRepository, times(2)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    // ── CB-211 Task 4: @Cacheable on getPostById ─────────────────────────────

    @Test
    void getPostById_secondCallReturnsCachedResult_repositoryCalledOnce() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        postService.getPostById(1L);
        postService.getPostById(1L); // served from cache

        verify(postRepository, times(1)).findById(1L);
    }

    // ── CB-211 Task 5: @CacheEvict on createPost ─────────────────────────────

    @Test
    void createPost_evictsPostsCache_nextGetAllPostsHitsDb() {
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        Post newPost = Post.builder()
                .id(2L).title("New Post").body("New Body")
                .category(Category.NEWS).author(author)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(postRepository.save(any(Post.class))).thenReturn(newPost);

        postService.getAllPosts(0, 10);                              // 1st DB hit — result cached
        postService.createPost(new PostRequest("New Post", "New Body", "NEWS"), author); // evicts
        postService.getAllPosts(0, 10);                              // cache evicted → 2nd DB hit

        verify(postRepository, times(2)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    // ── CB-211 Task 5: @Caching eviction on updatePost ───────────────────────

    @Test
    void updatePost_evictsPostAndPostsCache_nextGetByIdHitsDb() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        postService.getPostById(1L);                                                    // cached
        postService.updatePost(1L, new PostRequest("Updated", "Updated body", "NEWS"), author); // evicts (also calls findById internally)
        postService.getPostById(1L);                                                    // re-queries DB

        // 3 total: 1 from getPostById, 1 inside updatePost, 1 from getPostById after eviction
        verify(postRepository, times(3)).findById(1L);
    }

    // ── CB-211 Task 5: @Caching eviction on deletePost ───────────────────────

    @Test
    void deletePost_evictsPostAndPostsCache_nextGetByIdHitsDb() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);
        doNothing().when(postRepository).delete(any(Post.class));

        postService.getPostById(1L);        // cached
        postService.deletePost(1L, author); // evicts (also calls findById internally)
        postService.getPostById(1L);        // re-queries DB

        // 3 total: 1 from getPostById, 1 inside deletePost, 1 from getPostById after eviction
        verify(postRepository, times(3)).findById(1L);
    }
}
