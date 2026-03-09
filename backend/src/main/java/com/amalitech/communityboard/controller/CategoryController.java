package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.model.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

/**
 * Provides publicly accessible endpoints for retrieving valid post categories.
 * Categories are defined as the Category enum and are not stored in the database.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /**
     * Returns all valid post categories.
     * This endpoint is public — no authentication required.
     *
     * @return List of category names: ["NEWS", "EVENT", "DISCUSSION", "ALERT"]
     */
    @GetMapping
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = Arrays.stream(Category.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(categories);
    }
}
