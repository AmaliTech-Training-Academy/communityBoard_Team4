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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock AnalyticsSummaryRepository summaryRepository;
    @Mock PostsByCategoryRepository postsByCategoryRepository;
    @Mock PostsByDayRepository postsByDayRepository;
    @Mock TopContributorsRepository topContributorsRepository;

    @InjectMocks AnalyticsService analyticsService;

    @Test
    void getSummary_returnsSummaryWhenPresent() {
        AnalyticsSummary summary = mock(AnalyticsSummary.class);
        when(summaryRepository.getSummary()).thenReturn(Optional.of(summary));

        AnalyticsSummary result = analyticsService.getSummary();

        assertThat(result).isSameAs(summary);
    }

    @Test
    void getSummary_throwsWhenNotPresent() {
        when(summaryRepository.getSummary()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getSummary())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPostsByCategory_returnsList() throws ExecutionException, InterruptedException {
        List<PostsByCategory> data = List.of(mock(PostsByCategory.class));
        when(postsByCategoryRepository.getPostsByCategory()).thenReturn(data);

        CompletableFuture<List<PostsByCategory>> result = analyticsService.getPostsByCategory();

        assertThat(result.get()).isEqualTo(data);
    }

    @Test
    void getPostsByDay_returnsList() throws ExecutionException, InterruptedException {
        List<PostsByDay> data = List.of(mock(PostsByDay.class));
        when(postsByDayRepository.getPostsByDay()).thenReturn(data);

        CompletableFuture<List<PostsByDay>> result = analyticsService.getPostsByDay();

        assertThat(result.get()).isEqualTo(data);
    }

    @Test
    void getTopContributors_returnsList() throws ExecutionException, InterruptedException {
        List<TopContributor> data = List.of(mock(TopContributor.class));
        when(topContributorsRepository.getTopContributors()).thenReturn(data);

        CompletableFuture<List<TopContributor>> result = analyticsService.getTopContributors();

        assertThat(result.get()).isEqualTo(data);
    }
}
