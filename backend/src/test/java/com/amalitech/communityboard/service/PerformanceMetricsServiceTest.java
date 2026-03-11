package com.amalitech.communityboard.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CB-214: Unit tests for PerformanceMetricsService.
 *
 * Uses SimpleMeterRegistry (in-memory, no Spring context required) for speed.
 * Verifies that each counter increments correctly and timers record executions.
 */
class PerformanceMetricsServiceTest {

    private PerformanceMetricsService metricsService;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        metricsService = new PerformanceMetricsService(registry);
    }

    // ── Post counters ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementPostsCreated increments posts.created.total counter")
    void incrementPostsCreated_incrementsCounter() {
        assertThat(metricsService.getPostsCreatedCount()).isEqualTo(0.0);
        metricsService.incrementPostsCreated();
        assertThat(metricsService.getPostsCreatedCount()).isEqualTo(1.0);
        metricsService.incrementPostsCreated();
        assertThat(metricsService.getPostsCreatedCount()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("incrementPostsUpdated increments posts.updated.total counter")
    void incrementPostsUpdated_incrementsCounter() {
        metricsService.incrementPostsUpdated();
        assertThat(metricsService.getPostsUpdatedCount()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("incrementPostsDeleted increments posts.deleted.total counter")
    void incrementPostsDeleted_incrementsCounter() {
        metricsService.incrementPostsDeleted();
        assertThat(metricsService.getPostsDeletedCount()).isEqualTo(1.0);
    }

    // ── Comment counters ──────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementCommentsCreated increments comments.created.total counter")
    void incrementCommentsCreated_incrementsCounter() {
        metricsService.incrementCommentsCreated();
        metricsService.incrementCommentsCreated();
        assertThat(metricsService.getCommentsCreatedCount()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("incrementCommentsUpdated increments comments.updated.total counter")
    void incrementCommentsUpdated_incrementsCounter() {
        metricsService.incrementCommentsUpdated();
        assertThat(metricsService.getCommentsUpdatedCount()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("incrementCommentsDeleted increments comments.deleted.total counter")
    void incrementCommentsDeleted_incrementsCounter() {
        metricsService.incrementCommentsDeleted();
        assertThat(metricsService.getCommentsDeletedCount()).isEqualTo(1.0);
    }

    // ── Timers ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("recordPostLatency registers a sample in the post timer")
    void recordPostLatency_recordsSample() {
        metricsService.recordPostLatency(50);
        assertThat(metricsService.getPostTimerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recordCommentLatency registers a sample in the comment timer")
    void recordCommentLatency_recordsSample() {
        metricsService.recordCommentLatency(30);
        assertThat(metricsService.getCommentTimerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("timePostOperation records the timer and returns the supplier result")
    void timePostOperation_recordsAndReturnsResult() {
        String result = metricsService.timePostOperation(() -> "post-result");
        assertThat(result).isEqualTo("post-result");
        assertThat(metricsService.getPostTimerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("timeCommentOperation records the timer and returns the supplier result")
    void timeCommentOperation_recordsAndReturnsResult() {
        Integer result = metricsService.timeCommentOperation(() -> 42);
        assertThat(result).isEqualTo(42);
        assertThat(metricsService.getCommentTimerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("counters are independent — posting does not affect comment counters")
    void postAndCommentCounters_areIndependent() {
        metricsService.incrementPostsCreated();
        metricsService.incrementPostsCreated();
        metricsService.incrementCommentsDeleted();

        assertThat(metricsService.getPostsCreatedCount()).isEqualTo(2.0);
        assertThat(metricsService.getCommentsDeletedCount()).isEqualTo(1.0);
        assertThat(metricsService.getCommentsCreatedCount()).isEqualTo(0.0);
        assertThat(metricsService.getPostsDeletedCount()).isEqualTo(0.0);
    }
}
