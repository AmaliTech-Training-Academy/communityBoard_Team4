package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.*;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PerformanceMetricsService metricsService;

    /**
     * Returns a paginated list of comments for the given post, oldest-first.
     * Page and size default to 0 and 20 respectively if not supplied by the caller.
     */
    public Page<CommentResponse> getCommentsByPost(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
                .map(this::toResponse);
    }

    /**
     * Creates a comment on the given post.
     * Throws ResourceNotFoundException (404) if the post does not exist.
     */
    public CommentResponse createComment(Long postId, CommentRequest request, User author) {
        return metricsService.timeCommentOperation(() -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
            Comment comment = Comment.builder()
                    .content(request.getContent())
                    .post(post)
                    .author(author)
                    .build();
            CommentResponse result = toResponse(commentRepository.save(comment));
            metricsService.incrementCommentsCreated();
            return result;
        });
    }

    /**
     * Deletes a comment.
     * ADMIN may delete any comment.
     * A USER may only delete their own comment.
     * Throws ResourceNotFoundException (404) if comment does not exist.
     * Throws UnauthorizedException (403) if user is not the author and not ADMIN.
     */
    public CommentResponse updateComment(Long commentId, CommentRequest request, User currentUser) {
        return metricsService.timeCommentOperation(() -> {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            if (!isAdmin && !comment.getAuthor().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("Not authorized to edit this comment");
            }
            comment.setContent(request.getContent());
            CommentResponse result = toResponse(commentRepository.save(comment));
            metricsService.incrementCommentsUpdated();
            return result;
        });
    }

    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isAdmin && !comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        metricsService.incrementCommentsDeleted();
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .authorEmail(comment.getAuthor().getEmail())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
