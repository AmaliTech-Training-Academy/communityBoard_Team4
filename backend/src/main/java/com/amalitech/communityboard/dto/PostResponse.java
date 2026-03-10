package com.amalitech.communityboard.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostResponse {
    private Long id;
    private String title;
    /** Main post body (maps to the `body` column). */
    private String body;
    /** Category name (NEWS / EVENT / DISCUSSION / ALERT). */
    private String category;
    private String authorName;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long commentCount;
}
