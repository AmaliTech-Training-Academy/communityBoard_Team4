package com.amalitech.communityboard.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only JPA mapping for the {@code analytics_summary} materialized view.
 *
 * View columns:
 *   total_posts    — total number of posts in the platform
 *   total_comments — total number of comments in the platform
 *
 * The view has a unique index on {@code total_posts} which we use as the @Id.
 * This entity is immutable — the backend never writes to this view.
 */
@Entity
@Immutable
@Table(name = "analytics_summary")
@Getter
@NoArgsConstructor
public class AnalyticsSummary {

    /** Used as JPA @Id; uniquely indexed by the ETL pipeline. */
    @Id
    @Column(name = "total_posts")
    private Long totalPosts;

    @Column(name = "total_comments")
    private Long totalComments;
}
