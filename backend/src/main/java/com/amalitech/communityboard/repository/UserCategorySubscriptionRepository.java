package com.amalitech.communityboard.repository;

import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.UserCategorySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCategorySubscriptionRepository extends JpaRepository<UserCategorySubscription, Long> {

    List<UserCategorySubscription> findByUserId(Long userId);

    List<UserCategorySubscription> findByCategory(Category category);

    void deleteByUserId(Long userId);
}
