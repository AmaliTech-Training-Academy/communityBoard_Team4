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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
public class AnalyticsService {

    private final AnalyticsSummaryRepository summaryRepository;
    private final PostsByCategoryRepository postsByCategoryRepository;
    private final PostsByDayRepository postsByDayRepository;
    private final TopContributorsRepository topContributorsRepository;

    /**
     * Returns total post and comment counts from the analytics_summary view.
     * Cached under "analyticsSummary".
     */
    @Cacheable("analyticsSummary")
    public AnalyticsSummary getSummary() {
        return summaryRepository.getSummary()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analytics summary is not available — ETL pipeline may not have run yet"));
    }

    /**
     * Returns post counts grouped by category, ordered by post_count DESC.
     * Cached under "analyticsPostsByCategory".
     */
    @Cacheable("analyticsPostsByCategory")
    @Async
    public CompletableFuture<List<PostsByCategory>> getPostsByCategory() {
        return CompletableFuture.completedFuture(postsByCategoryRepository.getPostsByCategory());
    }

    /**
     * Returns post counts grouped by day of week, ordered by day_order ASC (Sun→Sat).
     * Cached under "analyticsPostsByDay".
     */
    @Cacheable("analyticsPostsByDay")
    @Async
    public CompletableFuture<List<PostsByDay>> getPostsByDay() {
        return CompletableFuture.completedFuture(postsByDayRepository.getPostsByDay());
    }

    /**
     * Returns the top 10 contributors ranked by post count, ordered by etl_rank ASC.
     * Cached under "analyticsTopContributors".
     */
    @Cacheable("analyticsTopContributors")
    @Async
    public CompletableFuture<List<TopContributor>> getTopContributors() {
        return CompletableFuture.completedFuture(topContributorsRepository.getTopContributors());
    }
}
