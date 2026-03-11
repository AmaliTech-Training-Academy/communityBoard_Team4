package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.PostRequest;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.CommentRepository;
import com.amalitech.communityboard.repository.PostRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
 *
 * Performance (cold vs warm):
 *  - cold call: cache miss — hits the repository
 *  - warm call: cache hit — skips the repository
 *  Timings are written to target/cache-performance.log
 */
@SpringBootTest
class PostCacheTest {

    @MockBean PostRepository postRepository;
    @MockBean CommentRepository commentRepository;

    @Autowired PostService postService;
    @Autowired CacheManager cacheManager;

    private User author;
    private Post post;

    /** Accumulated performance log lines written by cold/warm tests. */
    private static final List<String> PERF_LOG = new ArrayList<>();
    private static final String LOG_FILE = "target/cache-performance.log";

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void logPerf(String label, long coldNs, long warmNs) {
        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;
        double improvement = coldNs > 0 ? ((coldNs - warmNs) * 100.0 / coldNs) : 0;
        String line = String.format(
            "[%s] %-55s  cold=%7.3f ms  warm=%7.3f ms  improvement=%.1f%%",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            label, coldMs, warmMs, improvement);
        PERF_LOG.add(line);
    }

    /** Flush all accumulated perf entries to target/cache-performance.log after the test class. */
    @AfterAll
    static void writePerformanceLog() throws IOException {
        Path dir = Paths.get("target");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, false))) {
            pw.println("=".repeat(110));
            pw.println("CB-211 Cache Performance Report  —  generated: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pw.println("=".repeat(110));
            pw.printf("%-63s  %20s  %20s  %s%n", "Test", "Cold (cache miss)", "Warm (cache hit)", "Improvement");
            pw.println("-".repeat(110));
            PERF_LOG.forEach(pw::println);
            pw.println("-".repeat(110));
            pw.println("NOTE: cold = first call (DB hit).  warm = second call (served from cache, no DB).");
            pw.println("      A positive improvement % means the warm call was faster — cache is working.");
            pw.println("=".repeat(110));
        }
        System.out.println("\n[CB-211] Cache performance log written to: " + LOG_FILE);
    }

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

    // ── CB-211 Cold vs Warm performance tests ────────────────────────────────

    @Test
    void getAllPosts_coldVsWarm_warmIsFasterAndLogged() {
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        // COLD — cache is empty, must hit repository
        long coldStart = System.nanoTime();
        postService.getAllPosts(0, 10);
        long coldNs = System.nanoTime() - coldStart;

        // WARM — result now in cache, repository bypassed
        long warmStart = System.nanoTime();
        postService.getAllPosts(0, 10);
        long warmNs = System.nanoTime() - warmStart;

        logPerf("getAllPosts (page=0, size=10)", coldNs, warmNs);

        // Repository called exactly once (cold only)
        verify(postRepository, times(1)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        // Warm must be faster than cold
        assertThat(warmNs).isLessThan(coldNs);
    }

    @Test
    void getPostById_coldVsWarm_warmIsFasterAndLogged() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        // COLD
        long coldStart = System.nanoTime();
        postService.getPostById(1L);
        long coldNs = System.nanoTime() - coldStart;

        // WARM
        long warmStart = System.nanoTime();
        postService.getPostById(1L);
        long warmNs = System.nanoTime() - warmStart;

        logPerf("getPostById (id=1)", coldNs, warmNs);

        verify(postRepository, times(1)).findById(1L);
        assertThat(warmNs).isLessThan(coldNs);
    }

    @Test
    void getAllPosts_multipleWarmCalls_allFasterThanColdAndLogged() {
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByPostId(anyLong())).thenReturn(0L);

        // COLD
        long coldStart = System.nanoTime();
        postService.getAllPosts(0, 20);
        long coldNs = System.nanoTime() - coldStart;

        // WARM × 5 — every subsequent call served from cache
        long totalWarmNs = 0;
        for (int i = 0; i < 5; i++) {
            long s = System.nanoTime();
            postService.getAllPosts(0, 20);
            totalWarmNs += System.nanoTime() - s;
        }
        long avgWarmNs = totalWarmNs / 5;

        logPerf("getAllPosts — avg of 5 warm calls (page=0, size=20)", coldNs, avgWarmNs);

        // Repository called exactly once (only the cold hit)
        verify(postRepository, times(1)).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        assertThat(avgWarmNs).isLessThan(coldNs);
    }
}
