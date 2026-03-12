package com.amalitech.qa.testdata;

import com.amalitech.qa.dto.PostRequest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public final class PostTestData {

    private PostTestData() {
    }

    public static PostRequest defaultPostRequest() {
        return new PostRequest("Community News Update", "Latest updates for the neighborhood.", "NEWS");
    }

    public static Stream<Arguments> validPostRequests() {
        return Stream.of(
                Arguments.of(defaultPostRequest()),
                Arguments.of(new PostRequest("Weekend Cleanup Event", "Join cleanup on Saturday morning.", "EVENT")),
                Arguments.of(new PostRequest("Noise Policy Discussion", "Share your thoughts on quiet hours.", "DISCUSSION")),
                Arguments.of(new PostRequest("Water Outage Alert", "Temporary outage from 9am to 1pm.", "ALERT"))
        );
    }

    public static Stream<Arguments> emptyTitlePostRequests() {
        return Stream.of(
                Arguments.of(new PostRequest("", "Body text is present.", "NEWS"))
        );
    }

    public static Stream<Arguments> emptyBodyPostRequests() {
        return Stream.of(
                Arguments.of(new PostRequest("Title is present", "", "EVENT"))
        );
    }

    public static Stream<Arguments> invalidCategoryPostRequests() {
        return Stream.of(
                Arguments.of(new PostRequest("Title", "Body", "INVALID_CATEGORY"))
        );
    }

    public static Stream<Arguments> maxLengthPostRequests() {
        return Stream.of(
                Arguments.of(new PostRequest(repeat("T", 255), repeat("B", 10000), "NEWS"))
        );
    }

    public static Stream<Arguments> overLimitPostRequests() {
        return Stream.of(
                Arguments.of(new PostRequest(repeat("T", 256), "Valid body", "NEWS")),
                Arguments.of(new PostRequest("Valid title", repeat("B", 10001), "NEWS"))
        );
    }

    public static Stream<Arguments> validSearchCategories() {
        return Stream.of(
                Arguments.of("NEWS"),
                Arguments.of("EVENT"),
                Arguments.of("DISCUSSION"),
                Arguments.of("ALERT")
        );
    }

    private static String repeat(String s, int times) {
        return s.repeat(times);
    }
}
