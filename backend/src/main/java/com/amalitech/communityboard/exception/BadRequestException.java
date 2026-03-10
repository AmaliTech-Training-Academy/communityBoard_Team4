package com.amalitech.communityboard.exception;

/** Thrown for invalid client input that doesn't fit validation annotations. Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
