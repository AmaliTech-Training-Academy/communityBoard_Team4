package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Returns comments for a post ordered oldest-first (pagination support). */
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /** Returns all comments for a post as a list (used for count checks). */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /** Returns the number of comments associated with a post. */
    long countByPostId(Long postId);
}
