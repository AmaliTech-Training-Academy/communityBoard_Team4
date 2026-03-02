package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.dto.CommentResponse;
import com.amalitech.communityboard.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    // TODO: Add POST endpoint for creating comments
    // TODO: Add DELETE endpoint for deleting comments
}
