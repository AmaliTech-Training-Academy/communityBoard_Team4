package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.dto.CategorySubscriptionRequest;
import com.amalitech.communityboard.dto.CategorySubscriptionResponse;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.service.CategorySubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Category Subscriptions", description = "Manage category subscriptions for email notifications")
public class CategorySubscriptionController {

    private final CategorySubscriptionService categorySubscriptionService;

    @Operation(summary = "Get current user's category subscriptions", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription list returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/categories")
    public ResponseEntity<CategorySubscriptionResponse> getSubscriptions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categorySubscriptionService.getUserSubscriptions(user));
    }

    @Operation(summary = "Replace current user's category subscriptions", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscriptions updated"),
        @ApiResponse(responseCode = "400", description = "Invalid category payload"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/categories")
    public ResponseEntity<CategorySubscriptionResponse> updateSubscriptions(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategorySubscriptionRequest request) {
        return ResponseEntity.ok(categorySubscriptionService.updateUserSubscriptions(user, request));
    }
}
