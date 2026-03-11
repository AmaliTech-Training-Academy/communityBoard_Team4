package com.amalitech.communityboard.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private String content;
    private String authorName;
    private String authorEmail;
    private LocalDateTime createdAt;
}
