package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.CategorySubscriptionRequest;
import com.amalitech.communityboard.dto.CategorySubscriptionResponse;
import com.amalitech.communityboard.exception.BadRequestException;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.UserCategorySubscription;
import com.amalitech.communityboard.repository.UserCategorySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategorySubscriptionService {

    private final UserCategorySubscriptionRepository subscriptionRepository;

    public CategorySubscriptionResponse getUserSubscriptions(User user) {
        List<String> categories = subscriptionRepository.findByUserId(user.getId())
                .stream()
                .map(sub -> sub.getCategory().name())
                .toList();
        return CategorySubscriptionResponse.builder().categories(categories).build();
    }

    @Transactional
    public CategorySubscriptionResponse updateUserSubscriptions(User user, CategorySubscriptionRequest request) {
        List<String> requested = request.getCategories() == null ? Collections.emptyList() : request.getCategories();

        Set<Category> parsedCategories = new LinkedHashSet<>();
        for (String raw : requested) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                parsedCategories.add(Category.valueOf(raw.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid category for subscription: " + raw);
            }
        }

        subscriptionRepository.deleteByUserId(user.getId());

        List<UserCategorySubscription> subscriptions = parsedCategories.stream()
                .map(category -> UserCategorySubscription.builder()
                        .user(user)
                        .category(category)
                        .build())
                .toList();

        subscriptionRepository.saveAll(subscriptions);

        return CategorySubscriptionResponse.builder()
                .categories(parsedCategories.stream().map(Enum::name).toList())
                .build();
    }
}
