package com.amalitech.communityboard.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import com.amalitech.communityboard.dto.PostRequest;
import com.amalitech.communityboard.dto.PostResponse;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.repository.CommentRepository;
import com.amalitech.communityboard.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public Page<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    /**
     * Searches and filters posts by any combination of category, date range, and keyword.
     * All parameters are optional — passing null disables that filter.
     *
     * @param categoryStr  Category name (case-insensitive); null = no filter
     * @param startDate    Inclusive start of creation date range; null = no filter
     * @param endDate      Inclusive end of creation date range; null = no filter
     * @param keyword      Case-insensitive substring matched against title and body; null = no filter
     * @param page         0-based page index
     * @param size         Page size
     */
    public Page<PostResponse> searchPosts(String categoryStr, LocalDateTime startDate,
                                          LocalDateTime endDate, String keyword,
                                          int page, int size) {
        Category category = (categoryStr == null || categoryStr.isBlank()) ? null : parseCategory(categoryStr);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.searchPosts(category, startDate, endDate, kw, pageable)
                .map(this::toResponse);
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return toResponse(post);
    }

    public PostResponse createPost(PostRequest request, User author) {
        Category category = parseCategory(request.getCategory());
        Post post = Post.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .category(category)
                .author(author)
                .build();
        Post saved = postRepository.save(post);
        log.info("Post created: id={}, author={}", saved.getId(), author.getEmail());
        return toResponse(saved);
    }

    public PostResponse updatePost(Long id, PostRequest request, User author) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        boolean isAdmin = author.getRole().name().equals("ADMIN");
        if (!post.getAuthor().getId().equals(author.getId()) && !isAdmin) {
            throw new UnauthorizedException("Not authorized to update this post");
        }
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setCategory(parseCategory(request.getCategory()));
        log.info("Post updated: id={}, updatedBy={}", id, author.getEmail());
        return toResponse(postRepository.save(post));
    }

    public void deletePost(Long id, User author) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        boolean isAdmin = author.getRole().name().equals("ADMIN");
        if (!post.getAuthor().getId().equals(author.getId()) && !isAdmin) {
            throw new UnauthorizedException("Not authorized to delete this post");
        }
        // Comments are automatically deleted via CascadeType.ALL on Post.comments
        postRepository.delete(post);
        log.info("Post deleted: id={}, deletedBy={}", id, author.getEmail());
    }

    /**
     * Validates and parses a category string against the Category enum.
     * Accepts case-insensitive input (e.g. "news", "News", "NEWS" all map to Category.NEWS).
     */
    public Category parseCategory(String categoryStr) {
        try {
            return Category.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.amalitech.communityboard.exception.BadRequestException(
                "Invalid category '" + categoryStr + "'. Allowed values: NEWS, EVENT, DISCUSSION, ALERT");
        }
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .category(post.getCategory().name())
                .authorName(post.getAuthor().getName())
                .authorEmail(post.getAuthor().getEmail())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .commentCount(commentRepository.countByPostId(post.getId()))
                .build();
    }
}
