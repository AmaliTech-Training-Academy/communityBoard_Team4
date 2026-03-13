package com.amalitech.communityboard.service;

import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.UserCategorySubscription;
import com.amalitech.communityboard.repository.UserCategorySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewPostEmailNotificationService {

    private final UserCategorySubscriptionRepository subscriptionRepository;
    private final EmailDeliveryService emailDeliveryService;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailNotificationsEnabled;

    @Value("${app.notifications.email.from:no-reply@communityboard.local}")
    private String fromAddress;

    @Value("${app.notifications.email.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Async
    public void notifySubscribers(Post post) {
        if (!emailNotificationsEnabled) {
            return;
        }

        List<UserCategorySubscription> subscriptions = subscriptionRepository.findByCategory(post.getCategory());

        subscriptions.stream()
                .map(UserCategorySubscription::getUser)
                .filter(user -> !user.getId().equals(post.getAuthor().getId()))
                .forEach(user -> sendEmail(user.getEmail(), post));
    }

    private void sendEmail(String recipientEmail, Post post) {
        try {
            emailDeliveryService.send(
                    fromAddress,
                    recipientEmail,
                    "New " + post.getCategory().name() + " post on Community Board",
                    buildBody(post)
            );
        } catch (Exception ex) {
            log.warn("Failed to send new-post email notification to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String buildBody(Post post) {
        return "A new post was published in your subscribed category (" + post.getCategory().name() + ").\n\n"
                + "Title: " + post.getTitle() + "\n"
                + "Author: " + post.getAuthor().getName() + "\n\n"
                + "Open the board to read it: " + frontendBaseUrl + "/post/" + post.getId() + "\n\n"
                + "You are receiving this because you subscribed to this category in your profile settings.";
    }
}
