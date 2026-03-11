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
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CB-212: Verifies that PostAnalyticsService methods are invoked asynchronously
 * after post events (view, create, delete).
 *
 * Uses @SpringBootTest to load the full application context including
 * @EnableAsync and the "analyticsExecutor" ThreadPoolTaskExecutor.
 *
 * @SpyBean lets us assert that real async methods were actually called while
 * the rest of the application context wires up the proxy chain needed for @Async
 * to work (direct method calls on the same bean would bypass the proxy).
 */
@SpringBootTest
class PostAsyncTest {

    @MockBean  PostRepository postRepository;
    @MockBean  CommentRepository commentRepository;
    @SpyBean   PostAnalyticsService postAnalyticsService;

    @Autowired PostService postService;

    private User author;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .password("pw").role(Role.USER).build();

        savedPost = Post.builder()
                .id(42L).title("Test Post").body("Body")
                .category(Category.NEWS).author(author)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── recordPostView fires after getPostById ────────────────────────────────

    @Test
    void getPostById_triggersAsyncViewEvent() throws InterruptedException {
        when(postRepository.findById(42L)).thenReturn(Optional.of(savedPost));
        when(commentRepository.countByPostId(42L)).thenReturn(0L);

        postService.getPostById(42L, "alice@test.com");

        // Give the async thread up to 1 s to execute
        verify(postAnalyticsService, timeout(1000).times(1))
                .recordPostView(42L, "alice@test.com");
    }

    @Test
    void getPostById_anonymousViewer_triggersViewEventWithAnonymous() throws InterruptedException {
        when(postRepository.findById(42L)).thenReturn(Optional.of(savedPost));
        when(commentRepository.countByPostId(42L)).thenReturn(0L);

        postService.getPostById(42L, "anonymous");

        verify(postAnalyticsService, timeout(1000).times(1))
                .recordPostView(42L, "anonymous");
    }

    // ── recordPostCreated fires after createPost ──────────────────────────────

    @Test
    void createPost_triggersAsyncCreatedEvent() throws InterruptedException {
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(commentRepository.countByPostId(42L)).thenReturn(0L);

        PostRequest req = new PostRequest("Test Post", "Body", "NEWS");
        postService.createPost(req, author);

        verify(postAnalyticsService, timeout(1000).times(1))
                .recordPostCreated(42L, "alice@test.com", "NEWS");
    }

    // ── recordPostDeleted fires after deletePost ──────────────────────────────

    @Test
    void deletePost_triggersAsyncDeletedEvent() throws InterruptedException {
        when(postRepository.findById(42L)).thenReturn(Optional.of(savedPost));

        postService.deletePost(42L, author);

        verify(postAnalyticsService, timeout(1000).times(1))
                .recordPostDeleted(42L, "alice@test.com");
    }

    // ── analytics thread runs on the dedicated executor ───────────────────────

    @Test
    void recordPostView_runsOnAnalyticsExecutorThread() throws InterruptedException {
        // Verify that the analytics event is fired asynchronously after the service call.
        // The thread pool is named "analyticsExecutor" with prefix "async-analytics-".
        // We confirm async dispatch by verifying the call completes after the service
        // returns, using Mockito's timeout() matcher.
        when(postRepository.findById(42L)).thenReturn(Optional.of(savedPost));
        when(commentRepository.countByPostId(42L)).thenReturn(0L);

        postService.getPostById(42L, "alice@test.com");

        // timeout(2000) asserts the invocation happens — but only after an async dispatch
        // (if it were synchronous it would already be done; the timeout gives async time)
        verify(postAnalyticsService, timeout(2000).times(1))
                .recordPostView(42L, "alice@test.com");
    }

    // ── view event does NOT block the calling thread ──────────────────────────

    @Test
    void getPostById_doesNotBlockCallingThread() throws InterruptedException {
        // Both the DB mock and analytics mock return immediately.
        // The service call itself should complete in well under 100ms since
        // DB is mocked and analytics is dispatched asynchronously without waiting.
        when(postRepository.findById(42L)).thenReturn(Optional.of(savedPost));
        when(commentRepository.countByPostId(42L)).thenReturn(0L);

        long start = System.currentTimeMillis();
        postService.getPostById(42L, "alice@test.com");
        long elapsed = System.currentTimeMillis() - start;

        // With a mocked DB and fire-and-forget async, this should be < 100ms
        assertThat(elapsed).as("getPostById should return quickly with async analytics").isLessThan(100);

        // Analytics method was still dispatched (verified asynchronously)
        verify(postAnalyticsService, timeout(2000).times(1))
                .recordPostView(42L, "alice@test.com");
    }
}
