package com.amalitech.qa.testdata;

import com.amalitech.qa.dto.CommentRequest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public final class CommentTestData {

    private CommentTestData() {
    }

    public static CommentRequest defaultCommentRequest() {
        return new CommentRequest("Great post, thanks for sharing.");
    }

    public static Stream<Arguments> validCommentRequests() {
        return Stream.of(
                Arguments.of(defaultCommentRequest()),
                Arguments.of(new CommentRequest("I support this event.")),
                Arguments.of(new CommentRequest("Please share more details."))
        );
    }

    public static Stream<Arguments> emptyCommentRequests() {
        return Stream.of(
                Arguments.of(new CommentRequest("")),
                Arguments.of(new CommentRequest("   "))
        );
    }

    public static Stream<Arguments> shortCommentRequests() {
        return Stream.of(
                Arguments.of(new CommentRequest("ok")),
                Arguments.of(new CommentRequest("a"))
        );
    }

    public static Stream<Arguments> maxLengthCommentRequests() {
        return Stream.of(
                Arguments.of(new CommentRequest(repeat("C", 2000)))
        );
    }

    public static Stream<Arguments> overLimitCommentRequests() {
        return Stream.of(
                Arguments.of(new CommentRequest(repeat("C", 2001)))
        );
    }

    private static String repeat(String s, int times) {
        return s.repeat(times);
    }
}
