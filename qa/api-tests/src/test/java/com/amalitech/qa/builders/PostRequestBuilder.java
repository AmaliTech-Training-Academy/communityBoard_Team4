package com.amalitech.qa.builders;

import com.amalitech.qa.testdata.PostTestData;
import com.amalitech.qa.tests.post.PostRequest;

public class PostRequestBuilder {
    private String title;
    private String body;
    private String category;

    public PostRequestBuilder() {
        PostRequest defaults = PostTestData.defaultPostRequest();
        this.title = defaults.getTitle();
        this.body = defaults.getBody();
        this.category = defaults.getCategory();
    }

    public PostRequestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public PostRequestBuilder withBody(String body) {
        this.body = body;
        return this;
    }

    public PostRequestBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public PostRequest build() {
        return new PostRequest(title, body, category);
    }
}
