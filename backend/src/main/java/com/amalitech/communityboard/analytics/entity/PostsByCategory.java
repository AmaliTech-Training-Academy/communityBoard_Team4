package com.amalitech.communityboard.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only JPA mapping for the {@code analytics_posts_by_category} materialized view.
 *
 * View columns:
 *   category_name — one of NEWS, EVENT, DISCUSSION, ALERT
 *   post_count    — number of posts in that category
 *
 * The view guarantees all 4 categories always appear (even with 0 count).
 * API must order by: post_count DESC
 */
@Entity
@Immutable
@Table(name = "analytics_posts_by_category")
@Getter
@NoArgsConstructor
public class PostsByCategory {

    /** category_name is uniquely indexed — safe to use as @Id. */
    @Id
    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "post_count")
    private Long postCount;
}
