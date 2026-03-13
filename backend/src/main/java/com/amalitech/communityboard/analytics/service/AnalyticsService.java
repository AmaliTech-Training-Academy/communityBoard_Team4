package com.amalitech.communityboard.analytics.service;

import com.amalitech.communityboard.analytics.entity.AnalyticsSummary;
import com.amalitech.communityboard.analytics.entity.PostsByCategory;
import com.amalitech.communityboard.analytics.entity.PostsByDay;
import com.amalitech.communityboard.analytics.entity.TopContributor;
import com.amalitech.communityboard.analytics.repository.AnalyticsSummaryRepository;
import com.amalitech.communityboard.analytics.repository.PostsByCategoryRepository;
import com.amalitech.communityboard.analytics.repository.PostsByDayRepository;
import com.amalitech.communityboard.analytics.repository.TopContributorsRepository;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service layer for analytics endpoints.
 *
 * All methods read from PostgreSQL materialized views refreshed by the ETL pipeline.
 * Results are cached to avoid redundant view scans between ETL refresh cycles.
 *
 * Caches are intentionally simple (ConcurrentMapCacheManager) — the ETL pipeline
 * is responsible for data freshness; the backend cache protects against burst reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsSummaryRepository summaryRepository;
    private final PostsByCategoryRepository postsByCategoryRepository;
    private final PostsByDayRepository postsByDayRepository;
    private final TopContributorsRepository topContributorsRepository;
    private final JdbcTemplate jdbcTemplate;

    // Avoid refreshing on every single request burst.
    private static final long REFRESH_DEBOUNCE_MS = 5000;
    private final AtomicLong lastRefreshEpochMs = new AtomicLong(0);
    private final Object refreshLock = new Object();

    private void refreshAnalyticsViewsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshEpochMs.get() < REFRESH_DEBOUNCE_MS) {
            return;
        }

        synchronized (refreshLock) {
            now = System.currentTimeMillis();
            if (now - lastRefreshEpochMs.get() < REFRESH_DEBOUNCE_MS) {
                return;
            }

            refreshView("analytics_summary");
            refreshView("analytics_posts_by_category");
            refreshView("analytics_posts_by_day");
            refreshView("analytics_top_contributors");
            lastRefreshEpochMs.set(now);
        }
    }

    private void refreshView(String viewName) {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName);
        } catch (Exception concurrentEx) {
            // Fallback for startup/single-session edge cases where CONCURRENTLY is not possible.
            log.debug("Concurrent refresh failed for {}. Falling back to blocking refresh.", viewName, concurrentEx);
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + viewName);
        }
    }

    /**
     * Returns total post and comment counts from the analytics_summary view.
     * Cached under "analyticsSummary".
     */
    public AnalyticsSummary getSummary() {
        refreshAnalyticsViewsIfNeeded();
        return summaryRepository.getSummary()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analytics summary is not available — ETL pipeline may not have run yet"));
    }

    /**
     * Returns post counts grouped by category, ordered by post_count DESC.
     * Cached under "analyticsPostsByCategory".
     */
    @Async
    public CompletableFuture<List<PostsByCategory>> getPostsByCategory() {
        refreshAnalyticsViewsIfNeeded();
        return CompletableFuture.completedFuture(postsByCategoryRepository.getPostsByCategory());
    }

    /**
     * Returns post counts grouped by day of week, ordered by day_order ASC (Sun→Sat).
     * Cached under "analyticsPostsByDay".
     */
    @Async
    public CompletableFuture<List<PostsByDay>> getPostsByDay() {
        refreshAnalyticsViewsIfNeeded();
        return CompletableFuture.completedFuture(postsByDayRepository.getPostsByDay());
    }

    /**
     * Returns the top 10 contributors ranked by post count, ordered by etl_rank ASC.
     * Cached under "analyticsTopContributors".
     */
    @Async
    public CompletableFuture<List<TopContributor>> getTopContributors() {
        refreshAnalyticsViewsIfNeeded();
        return CompletableFuture.completedFuture(topContributorsRepository.getTopContributors());
    }
}
