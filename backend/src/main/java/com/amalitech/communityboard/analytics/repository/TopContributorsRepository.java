package com.amalitech.communityboard.analytics.repository;

import com.amalitech.communityboard.analytics.entity.TopContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopContributorsRepository extends JpaRepository<TopContributor, Long> {

    @Query(value = "SELECT * FROM analytics_top_contributors ORDER BY etl_rank ASC", nativeQuery = true)
    List<TopContributor> getTopContributors();
}
