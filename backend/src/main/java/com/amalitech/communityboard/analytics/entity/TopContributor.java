package com.amalitech.communityboard.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only JPA mapping for the {@code analytics_top_contributors} materialized view.
 *
 * View columns:
 *   etl_rank           — unique sequential rank (ROW_NUMBER); used as @Id and for ordering
 *   true_rank          — leaderboard rank with ties (RANK); tied users share the same value
 *   user_id            — FK to users table
 *   contributor_name   — user's display name
 *   contributor_email  — user's email
 *   post_count         — number of posts authored
 *
 * API must order by: etl_rank ASC
 * View is limited to top 10 contributors by the ETL pipeline.
 */
@Entity
@Immutable
@Table(name = "analytics_top_contributors")
@Getter
@NoArgsConstructor
public class TopContributor {

    /** etl_rank is a unique ROW_NUMBER — safe to use as @Id. */
    @Id
    @Column(name = "etl_rank")
    private Long etlRank;

    @Column(name = "true_rank")
    private Long trueRank;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "contributor_name")
    private String contributorName;

    @Column(name = "contributor_email")
    private String contributorEmail;

    @Column(name = "post_count")
    private Long postCount;
}
