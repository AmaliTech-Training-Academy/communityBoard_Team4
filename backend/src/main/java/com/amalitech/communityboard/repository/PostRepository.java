package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    // TODO: Add search methods - findByTitleContainingIgnoreCase, full-text search
}
