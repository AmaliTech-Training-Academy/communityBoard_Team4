package com.amalitech.communityboard.exception;

/** Thrown when a requested resource (Post, Comment, User) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
