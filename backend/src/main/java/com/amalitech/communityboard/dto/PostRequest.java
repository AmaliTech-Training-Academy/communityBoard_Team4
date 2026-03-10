package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    /**
     * The main post body. Max 10 000 characters enforced at the application layer;
     * the DB column is TEXT (unlimited) but we guard against excessive payloads here.
     */
    @NotBlank(message = "Body is required")
    @Size(max = 10_000, message = "Body must not exceed 10 000 characters")
    private String body;

    /**
     * Category must be one of: NEWS, EVENT, DISCUSSION, ALERT (case-insensitive).
     * Validated in PostService against the Category enum.
     */
    @NotBlank(message = "Category is required – allowed values: NEWS, EVENT, DISCUSSION, ALERT")
    private String category;
}
