package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.NotificationResponse;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.exception.UnauthorizedException;
import com.amalitech.communityboard.model.Notification;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Fired asynchronously from CommentService after a comment is persisted.
     * Skipped when the commenter is the post author (no self-notification).
     */
    @Async
    public void notifyPostAuthorOfComment(Post post, User commenter) {
        if (post.getAuthor().getId().equals(commenter.getId())) {
            return;
        }
        Notification notification = Notification.builder()
                .recipient(post.getAuthor())
                .message(commenter.getName() + " commented on your post \"" + post.getTitle() + "\"")
                .build();
        notificationRepository.save(notification);
    }

    public Page<NotificationResponse> getNotifications(User user, int page, int size) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not your notification");
        }
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllReadByRecipientId(user.getId());
    }
}
