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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 *
 * Repositories are mocked; the service logic — delegating to repositories,
 * wrapping results in CompletableFuture, and throwing ResourceNotFoundException
 * when the analytics_summary view is empty — is exercised in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock AnalyticsSummaryRepository summaryRepository;
    @Mock PostsByCategoryRepository postsByCategoryRepository;
    @Mock PostsByDayRepository postsByDayRepository;
    @Mock TopContributorsRepository topContributorsRepository;
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks AnalyticsService analyticsService;

    // ── getSummary ────────────────────────────────────────────────────────────

    @Test
    void getSummary_viewHasData_returnsSummary() {
        AnalyticsSummary summary = new AnalyticsSummary();
        when(summaryRepository.getSummary()).thenReturn(Optional.of(summary));

        AnalyticsSummary result = analyticsService.getSummary();

        assertThat(result).isSameAs(summary);
        verify(jdbcTemplate).execute(contains("analytics_summary"));
        verify(summaryRepository).getSummary();
    }

    @Test
    void getSummary_viewEmpty_throwsResourceNotFoundException() {
        when(summaryRepository.getSummary()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getSummary())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ETL pipeline");

        verify(jdbcTemplate).execute(contains("analytics_summary"));
    }

    // ── getPostsByCategory ────────────────────────────────────────────────────

    @Test
    void getPostsByCategory_returnsFutureWithList() throws ExecutionException, InterruptedException {
        List<PostsByCategory> data = List.of();
        when(postsByCategoryRepository.getPostsByCategory()).thenReturn(data);

        List<PostsByCategory> result = analyticsService.getPostsByCategory().get();

        assertThat(result).isSameAs(data);
        verify(jdbcTemplate).execute(contains("analytics_posts_by_category"));
        verify(postsByCategoryRepository).getPostsByCategory();
    }

    // ── getPostsByDay ─────────────────────────────────────────────────────────

    @Test
    void getPostsByDay_returnsFutureWithList() throws ExecutionException, InterruptedException {
        List<PostsByDay> data = List.of();
        when(postsByDayRepository.getPostsByDay()).thenReturn(data);

        List<PostsByDay> result = analyticsService.getPostsByDay().get();

        assertThat(result).isSameAs(data);
        verify(jdbcTemplate).execute(contains("analytics_posts_by_day"));
        verify(postsByDayRepository).getPostsByDay();
    }

    // ── getTopContributors ────────────────────────────────────────────────────

    @Test
    void getTopContributors_returnsFutureWithList() throws ExecutionException, InterruptedException {
        List<TopContributor> data = List.of();
        when(topContributorsRepository.getTopContributors()).thenReturn(data);

        List<TopContributor> result = analyticsService.getTopContributors().get();

        assertThat(result).isSameAs(data);
        verify(jdbcTemplate).execute(contains("analytics_top_contributors"));
        verify(topContributorsRepository).getTopContributors();
    }
}
