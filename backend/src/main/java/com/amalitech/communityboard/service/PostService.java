package com.amalitech.communityboard.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final NewPostEmailNotificationService newPostEmailNotificationService;

    public Page<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    /**
     * Searches and filters posts by any combination of category, date range, and keyword.
     * All parameters are optional — passing null disables that filter.
     */
    public Page<PostResponse> searchPosts(String categoryStr, LocalDateTime startDate,
                                          LocalDateTime endDate, String keyword,
                                          int page, int size) {
        Category category = (categoryStr == null || categoryStr.isBlank()) ? null : parseCategory(categoryStr);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Post> spec = Specification.where(null);

        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
        if (kw != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + kw.toLowerCase() + "%"));
        }

        return postRepository.findAll(spec, pageable).map(this::toResponse);
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
        newPostEmailNotificationService.notifySubscribers(saved);
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
