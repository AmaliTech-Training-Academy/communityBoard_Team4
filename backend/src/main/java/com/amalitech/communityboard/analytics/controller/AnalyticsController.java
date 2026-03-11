package com.amalitech.communityboard.analytics.controller;

import com.amalitech.communityboard.analytics.entity.AnalyticsSummary;
import com.amalitech.communityboard.analytics.entity.PostsByCategory;
import com.amalitech.communityboard.analytics.entity.PostsByDay;
import com.amalitech.communityboard.analytics.entity.TopContributor;
import com.amalitech.communityboard.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only analytics endpoints backed by PostgreSQL materialized views.
 * All data is precomputed by the ETL pipeline — no computation happens here.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Read-only analytics endpoints backed by ETL-refreshed materialized views")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Platform summary — total posts and total comments")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Summary data returned"),
        @ApiResponse(responseCode = "404", description = "ETL pipeline has not yet populated the view")
    })
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummary> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    @Operation(summary = "Post counts grouped by category (NEWS, EVENT, DISCUSSION, ALERT), ordered by count descending")
    @ApiResponse(responseCode = "200", description = "All 4 categories returned, zero counts included")
    @GetMapping("/posts-by-category")
    public CompletableFuture<ResponseEntity<List<PostsByCategory>>> getPostsByCategory() {
        return analyticsService.getPostsByCategory()
                .thenApply(ResponseEntity::ok);
    }

    @Operation(summary = "Post counts grouped by day of week (Sun→Sat), ordered chronologically")
    @ApiResponse(responseCode = "200", description = "All 7 days returned, zero counts included")
    @GetMapping("/posts-by-day")
    public CompletableFuture<ResponseEntity<List<PostsByDay>>> getPostsByDay() {
        return analyticsService.getPostsByDay()
                .thenApply(ResponseEntity::ok);
    }

    @Operation(summary = "Top 10 contributors ranked by post count")
    @ApiResponse(responseCode = "200", description = "Up to 10 contributors returned")
    @GetMapping("/contributors/top")
    public CompletableFuture<ResponseEntity<List<TopContributor>>> getTopContributors() {
        return analyticsService.getTopContributors()
                .thenApply(ResponseEntity::ok);
    }

    @Operation(
        summary = "Refresh analytics views",
        description = "Re-runs all 4 materialized views and clears the analytics cache. Call this after seeding data or when the ETL pipeline is not running. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Views refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshViews() {
        analyticsService.refreshViews();
        return ResponseEntity.ok("Analytics views refreshed successfully");
    }
}
