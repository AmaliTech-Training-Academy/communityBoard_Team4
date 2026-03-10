package com.amalitech.communityboard.exception;

/** Thrown when an authenticated user attempts an action they are not permitted to perform. Maps to HTTP 403. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
