package com.amalitech.communityboard.analytics.repository;

import com.amalitech.communityboard.analytics.entity.PostsByCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostsByCategoryRepository extends JpaRepository<PostsByCategory, String> {

    @Query(value = "SELECT * FROM analytics_posts_by_category ORDER BY post_count DESC", nativeQuery = true)
    List<PostsByCategory> getPostsByCategory();
}
