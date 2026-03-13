package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategorySubscriptionRequest {

    @NotNull(message = "categories must not be null")
    private List<String> categories;
}
