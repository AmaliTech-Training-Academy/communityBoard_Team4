package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** Returns all posts ordered by newest first, with pagination. */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Returns all posts in a given category, ordered by newest first, with pagination. */
    Page<Post> findByCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);

    /**
     * Full search/filter query supporting optional filters:
     * - category (enum)
     * - date range (inclusive start/end)
     * - keyword (case-insensitive match against title OR body)
     *
     * Passing null for any parameter disables that filter.
     */
    @Query("SELECT p FROM Post p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate) AND " +
           "(:keyword IS NULL OR " +
           "   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "   LOWER(p.body) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchPosts(
            @Param("category")  Category category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            @Param("keyword")   String keyword,
            Pageable pageable);
}
