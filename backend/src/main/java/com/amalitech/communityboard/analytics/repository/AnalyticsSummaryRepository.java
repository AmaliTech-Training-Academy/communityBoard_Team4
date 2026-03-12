package com.amalitech.communityboard.analytics.repository;

import com.amalitech.communityboard.analytics.entity.AnalyticsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsSummaryRepository extends JpaRepository<AnalyticsSummary, Long> {

    @Query(value = "SELECT * FROM analytics_summary LIMIT 1", nativeQuery = true)
    Optional<AnalyticsSummary> getSummary();
}
