package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    int countByPostId(Long postId);
}
