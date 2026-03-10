package com.amalitech.communityboard.exception;

/** Thrown when a resource already exists (e.g. duplicate email). Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
