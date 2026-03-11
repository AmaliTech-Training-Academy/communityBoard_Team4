package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /** Returns all posts ordered by newest first, with pagination. */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Returns all posts in a given category, ordered by newest first, with pagination. */
    Page<Post> findByCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);
}
