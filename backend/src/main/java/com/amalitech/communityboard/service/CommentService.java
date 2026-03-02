package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.model.*;
import com.amalitech.communityboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentResponse> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toResponse).toList();
    }

    // TODO: Implement createComment
    // public CommentResponse createComment(Long postId, CommentRequest request, User author) { ... }

    // TODO: Implement deleteComment
    // public void deleteComment(Long commentId, User author) { ... }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
