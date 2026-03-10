package com.amalitech.communityboard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Post entity representing a community board post.
 * Cascade delete ensures all associated comments are removed when a post is deleted.
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_category",    columnList = "category"),
        @Index(name = "idx_posts_created_at",  columnList = "created_at"),
        @Index(name = "idx_posts_author_id",   columnList = "author_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    /**
     * The main body / content of the post.
     * Column name is `body` (aligned with CommunityBoard spec).
     * Application-layer max enforced in PostRequest DTO (@Size max = 10_000).
     */
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    /** Category stored as a string enum — constrained to NEWS, EVENT, DISCUSSION, ALERT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Cascade ALL + orphanRemoval ensures that deleting a Post automatically removes
     * all of its Comments, maintaining referential integrity.
     * This collection is JPA-managed and never set via the builder.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
