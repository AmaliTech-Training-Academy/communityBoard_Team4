package com.amalitech.communityboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * CB-212: Asynchronous analytics tracking for post events.
 *
 * All methods are annotated with {@code @Async("analyticsExecutor")} so they run
 * on the dedicated thread pool defined in AppConfig — the calling thread (HTTP request
 * thread) returns immediately without waiting for the analytics work to complete.
 *
 * Current implementation uses structured logging as the analytics sink.
 * Future upgrade path: replace log statements with writes to an analytics DB table,
 * a message queue (Kafka/SQS), or an APM service without changing the call sites.
 */
@Slf4j
@Service
public class PostAnalyticsService {

    /**
     * Records a post view event asynchronously.
     * Called from {@code PostService#getPostById} — fires and returns immediately.
     *
     * @param postId  the ID of the post that was viewed
     * @param viewerEmail  the email of the authenticated viewer, or "anonymous"
     */
    @Async("analyticsExecutor")
    public void recordPostView(Long postId, String viewerEmail) {
        log.info("[ANALYTICS] post_view | postId={} viewer={} thread={}",
                postId, viewerEmail, Thread.currentThread().getName());
    }

    /**
     * Records a post creation event asynchronously.
     * Called from {@code PostService#createPost} after the post is saved.
     *
     * @param postId       the ID of the newly created post
     * @param authorEmail  the email of the author
     * @param category     the category of the post
     */
    @Async("analyticsExecutor")
    public void recordPostCreated(Long postId, String authorEmail, String category) {
        log.info("[ANALYTICS] post_created | postId={} author={} category={} thread={}",
                postId, authorEmail, category, Thread.currentThread().getName());
    }

    /**
     * Records a post deletion event asynchronously.
     * Called from {@code PostService#deletePost} after the post is removed.
     *
     * @param postId       the ID of the deleted post
     * @param deletedBy    the email of the user who performed the deletion
     */
    @Async("analyticsExecutor")
    public void recordPostDeleted(Long postId, String deletedBy) {
        log.info("[ANALYTICS] post_deleted | postId={} deletedBy={} thread={}",
                postId, deletedBy, Thread.currentThread().getName());
    }
}
