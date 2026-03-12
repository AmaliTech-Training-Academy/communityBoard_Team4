package com.amalitech.qa.builders;

import com.amalitech.qa.dto.CommentRequest;
import com.amalitech.qa.testdata.CommentTestData;

public class CommentRequestBuilder {
    private String content;

    public CommentRequestBuilder() {
        CommentRequest defaults = CommentTestData.defaultCommentRequest();
        this.content = defaults.getContent();
    }

    public CommentRequestBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public CommentRequest build() {
        return new CommentRequest(content);
    }
}
