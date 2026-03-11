package com.amalitech.communityboard.analytics.repository;

import com.amalitech.communityboard.analytics.entity.PostsByDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostsByDayRepository extends JpaRepository<PostsByDay, Integer> {

    @Query(value = "SELECT * FROM analytics_posts_by_day ORDER BY day_order ASC", nativeQuery = true)
    List<PostsByDay> getPostsByDay();
}
