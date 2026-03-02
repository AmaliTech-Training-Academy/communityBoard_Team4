package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CommentRequest {
    @NotBlank
    private String content;
}
