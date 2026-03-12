package com.amalitech.qa.testdata;

import com.amalitech.qa.tests.auth.AuthRequest;
import com.amalitech.qa.tests.auth.RegisterRequest;
import com.amalitech.qa.tests.comments.CommentRequest;
import com.amalitech.qa.tests.post.PostRequest;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Utility examples for future parameterized tests.
 *
 * Example:
 *
 * @ParameterizedTest
 * @MethodSource("com.amalitech.qa.testdata.AuthTestData#validLoginRequests")
 * void login_success(AuthRequest request) { ... }
 *
 * @ParameterizedTest
 * @MethodSource("com.amalitech.qa.testdata.PostTestData#overLimitPostRequests")
 * void create_post_validation(PostRequest request) { ... }
 */
public final class ParameterizedDataNotes {

    private ParameterizedDataNotes() {
    }

    // Kept only as compile-time reference to common argument signatures.
    public static Stream<Arguments> sampleSignatures(
            RegisterRequest registerRequest,
            AuthRequest authRequest,
            PostRequest postRequest,
            CommentRequest commentRequest,
            int expectedStatus
    ) {
        return Stream.empty();
    }
}
