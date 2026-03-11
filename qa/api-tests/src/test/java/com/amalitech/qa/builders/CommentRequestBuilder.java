package com.amalitech.qa.builders;

import com.amalitech.qa.dto.CommentRequest;
import com.amalitech.qa.testdata.TestDataLoader;

/**
 * CommentRequestBuilder creates CommentRequest objects with defaults loaded from JSON
 * Supports overriding content for negative/edge case tests
 */
public class CommentRequestBuilder {
    private String content;
    private boolean loadedDefaults = false;

    /**
     * Initialize with defaults from valid-comment.json
     */
    private void ensureDefaults() {
        if (!loadedDefaults) {
            CommentRequest defaults = TestDataLoader.loadTestData("comments/valid-comment.json", CommentRequest.class);
            this.content = defaults.getContent();
            this.loadedDefaults = true;
        }
    }

    public CommentRequestBuilder withContent(String content) {
        ensureDefaults();
        this.content = content;
        return this;
    }

    public CommentRequest build() {
        ensureDefaults();
        return new CommentRequest(content);
    }
}
