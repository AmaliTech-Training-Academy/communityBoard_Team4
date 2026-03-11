package com.amalitech.qa.builders;

import com.amalitech.qa.dto.PostRequest;
import com.amalitech.qa.testdata.TestDataLoader;

/**
 * PostRequestBuilder creates PostRequest objects with defaults loaded from JSON
 * Supports loading from predefined categories (NEWS, EVENT, DISCUSSION, ALERT)
 * Allows overriding fields for negative/edge case tests
 */
public class PostRequestBuilder {
    private String title;
    private String body;
    private String category;
    private boolean loadedDefaults = false;

    /**
     * Initialize with defaults from valid-news-post.json (NEWS category)
     */
    private void ensureDefaults() {
        if (!loadedDefaults) {
            PostRequest defaults = TestDataLoader.loadTestData("posts/valid-news-post.json", PostRequest.class);
            this.title = defaults.getTitle();
            this.body = defaults.getBody();
            this.category = defaults.getCategory();
            this.loadedDefaults = true;
        }
    }

    /**
     * Load defaults from a specific post category fixture
     *
     * @param category One of: NEWS, EVENT, DISCUSSION, ALERT
     * @return This builder (for chaining)
     */
    public PostRequestBuilder loadFromCategory(String category) {
        String fixture = switch (category.toUpperCase()) {
            case "NEWS" -> "posts/valid-news-post.json";
            case "EVENT" -> "posts/valid-event-post.json";
            case "DISCUSSION" -> "posts/valid-discussion-post.json";
            case "ALERT" -> "posts/valid-alert-post.json";
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };

        PostRequest data = TestDataLoader.loadTestData(fixture, PostRequest.class);
        this.title = data.getTitle();
        this.body = data.getBody();
        this.category = data.getCategory();
        this.loadedDefaults = true;
        return this;
    }

    public PostRequestBuilder withTitle(String title) {
        ensureDefaults();
        this.title = title;
        return this;
    }

    public PostRequestBuilder withBody(String body) {
        ensureDefaults();
        this.body = body;
        return this;
    }

    public PostRequestBuilder withCategory(String category) {
        ensureDefaults();
        this.category = category;
        return this;
    }

    public PostRequest build() {
        ensureDefaults();
        return new PostRequest(title, body, category);
    }
}
