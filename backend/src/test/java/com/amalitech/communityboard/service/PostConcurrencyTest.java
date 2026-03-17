package com.amalitech.communityboard.service;

import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.PostRepository;
import com.amalitech.communityboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CB-213: Concurrency safety tests.
 *
 * Verifies that:
 * 1. The @Version column on Post prevents lost updates (optimistic locking).
 * 2. A concurrent second write throws ObjectOptimisticLockingFailureException.
 * 3. Successful single-threaded updates increment the version counter.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostConcurrencyTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .email("author@test.com")
                .name("Test Author")
                .password("encoded-password")
                .role(Role.USER)
                .build());

        savedPost = postRepository.save(Post.builder()
                .title("Original Title")
                .body("Original body content")
                .category(Category.DISCUSSION)
                .author(author)
                .build());
    }

    @Test
    @DisplayName("@Version field starts at 0 on new post")
    void newPost_hasVersionZero() {
        assertThat(savedPost.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Version increments by 1 on each successful update")
    void successfulUpdate_incrementsVersion() {
        // Re-fetch to get a fully managed instance (avoids cascade/orphan issues with detached builder-created entities)
        Post v0 = postRepository.findById(savedPost.getId()).orElseThrow();
        v0.setTitle("Updated Title v1");
        Post afterFirstUpdate = postRepository.saveAndFlush(v0);
        assertThat(afterFirstUpdate.getVersion()).isEqualTo(1L);

        Post v1 = postRepository.findById(afterFirstUpdate.getId()).orElseThrow();
        v1.setTitle("Updated Title v2");
        Post afterSecondUpdate = postRepository.saveAndFlush(v1);
        assertThat(afterSecondUpdate.getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Concurrent update on stale entity throws ObjectOptimisticLockingFailureException")
    void concurrentUpdate_throwsOptimisticLockException() {
        // Simulate two transactions both reading the same post at version 0
        Post staleSnapshot = postRepository.findById(savedPost.getId())
                .orElseThrow();

        // First update succeeds and bumps version to 1
        Post freshCopy = postRepository.findById(savedPost.getId()).orElseThrow();
        freshCopy.setTitle("Winner update");
        postRepository.saveAndFlush(freshCopy);

        // The stale snapshot still has version 0 — saving it must fail
        staleSnapshot.setTitle("Losing concurrent update");
        assertThatThrownBy(() -> postRepository.saveAndFlush(staleSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Only one thread wins when two threads update the same post concurrently")
    void twoThreadsConcurrentUpdate_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch readyLatch  = new CountDownLatch(threadCount);
        CountDownLatch startLatch  = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // Both threads load the post before either saves
                    Post localCopy = postRepository.findById(savedPost.getId()).orElseThrow();
                    readyLatch.countDown();
                    startLatch.await(); // wait for both threads to be ready

                    localCopy.setTitle("Thread-" + threadIndex + " update");
                    postRepository.saveAndFlush(localCopy);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // Hibernate may wrap the exception — count it as a locking failure
                    if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                        failCount.incrementAndGet();
                    }
                }
            });
        }

        readyLatch.await();  // wait until both threads have read the entity
        startLatch.countDown(); // fire both updates simultaneously
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Exactly one winner, exactly one failure
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Saved post is persisted and retrievable by ID")
    void savedPost_isRetrievableById() {
        Post found = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Original Title");
        assertThat(found.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Post delete removes the record from the repository")
    void deletePost_removesFromRepository() {
        Long id = savedPost.getId();
        postRepository.deleteById(id);
        assertThat(postRepository.findById(id)).isEmpty();
    }
}
