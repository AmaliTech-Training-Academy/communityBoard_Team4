package com.amalitech.communityboard.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * CB-214: Performance Monitoring.
 *
 * Centralises all custom Micrometer metrics for the CommunityBoard backend.
 * Metrics are exposed via /actuator/prometheus for Prometheus scraping.
 *
 * Counters (cumulative counts since app start):
 *   posts.created.total      — every new post persisted
 *   posts.updated.total      — every post update
 *   posts.deleted.total      — every post deletion
 *   comments.created.total   — every new comment persisted
 *   comments.updated.total   — every comment update
 *   comments.deleted.total   — every comment deletion
 *
 * Timers (latency histograms):
 *   posts.service.latency    — end-to-end duration of write operations on posts
 *   comments.service.latency — end-to-end duration of write operations on comments
 */
@Service
public class PerformanceMetricsService {

    // ── Counters ──────────────────────────────────────────────────────────────

    private final Counter postsCreated;
    private final Counter postsUpdated;
    private final Counter postsDeleted;
    private final Counter commentsCreated;
    private final Counter commentsUpdated;
    private final Counter commentsDeleted;

    // ── Timers ────────────────────────────────────────────────────────────────

    private final Timer postServiceTimer;
    private final Timer commentServiceTimer;

    public PerformanceMetricsService(MeterRegistry registry) {
        postsCreated    = Counter.builder("posts.created.total")
                .description("Total number of posts created")
                .tag("service", "post")
                .register(registry);

        postsUpdated    = Counter.builder("posts.updated.total")
                .description("Total number of posts updated")
                .tag("service", "post")
                .register(registry);

        postsDeleted    = Counter.builder("posts.deleted.total")
                .description("Total number of posts deleted")
                .tag("service", "post")
                .register(registry);

        commentsCreated = Counter.builder("comments.created.total")
                .description("Total number of comments created")
                .tag("service", "comment")
                .register(registry);

        commentsUpdated = Counter.builder("comments.updated.total")
                .description("Total number of comments updated")
                .tag("service", "comment")
                .register(registry);

        commentsDeleted = Counter.builder("comments.deleted.total")
                .description("Total number of comments deleted")
                .tag("service", "comment")
                .register(registry);

        postServiceTimer = Timer.builder("posts.service.latency")
                .description("Latency of post write operations")
                .tag("service", "post")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        commentServiceTimer = Timer.builder("comments.service.latency")
                .description("Latency of comment write operations")
                .tag("service", "comment")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    // ── Counter increment helpers ─────────────────────────────────────────────

    public void incrementPostsCreated()    { postsCreated.increment(); }
    public void incrementPostsUpdated()    { postsUpdated.increment(); }
    public void incrementPostsDeleted()    { postsDeleted.increment(); }
    public void incrementCommentsCreated() { commentsCreated.increment(); }
    public void incrementCommentsUpdated() { commentsUpdated.increment(); }
    public void incrementCommentsDeleted() { commentsDeleted.increment(); }

    // ── Timer helpers ─────────────────────────────────────────────────────────

    /**
     * Records the execution time of a post write operation.
     *
     * @param durationMs elapsed time in milliseconds
     */
    public void recordPostLatency(long durationMs) {
        postServiceTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records the execution time of a comment write operation.
     *
     * @param durationMs elapsed time in milliseconds
     */
    public void recordCommentLatency(long durationMs) {
        commentServiceTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Convenience: times a post write operation and returns its result.
     */
    public <T> T timePostOperation(Supplier<T> operation) {
        return postServiceTimer.record(operation);
    }

    /**
     * Convenience: times a comment write operation and returns its result.
     */
    public <T> T timeCommentOperation(Supplier<T> operation) {
        return commentServiceTimer.record(operation);
    }

    // ── Snapshot accessors (used by tests) ────────────────────────────────────

    public double getPostsCreatedCount()    { return postsCreated.count(); }
    public double getPostsUpdatedCount()    { return postsUpdated.count(); }
    public double getPostsDeletedCount()    { return postsDeleted.count(); }
    public double getCommentsCreatedCount() { return commentsCreated.count(); }
    public double getCommentsUpdatedCount() { return commentsUpdated.count(); }
    public double getCommentsDeletedCount() { return commentsDeleted.count(); }
    public long   getPostTimerCount()       { return postServiceTimer.count(); }
    public long   getCommentTimerCount()    { return commentServiceTimer.count(); }
}
