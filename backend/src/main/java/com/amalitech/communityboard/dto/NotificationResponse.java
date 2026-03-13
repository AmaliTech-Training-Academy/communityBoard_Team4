package com.amalitech.communityboard.dto;

import com.amalitech.communityboard.model.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {

    private Long id;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
