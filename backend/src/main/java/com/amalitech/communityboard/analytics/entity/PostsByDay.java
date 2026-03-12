package com.amalitech.communityboard.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only JPA mapping for the {@code analytics_posts_by_day} materialized view.
 *
 * View columns:
 *   day_name   — abbreviated day name: Sun, Mon, Tue, Wed, Thu, Fri, Sat
 *   day_order  — numeric order 0 (Sun) to 6 (Sat); uniquely indexed
 *   post_count — number of posts created on that day of the week
 *
 * The view guarantees all 7 days always appear (even with 0 count).
 * API must order by: day_order ASC
 */
@Entity
@Immutable
@Table(name = "analytics_posts_by_day")
@Getter
@NoArgsConstructor
public class PostsByDay {

    /** day_order is uniquely indexed — safe to use as @Id. */
    @Id
    @Column(name = "day_order")
    private Integer dayOrder;

    @Column(name = "day_name")
    private String dayName;

    @Column(name = "post_count")
    private Long postCount;
}
