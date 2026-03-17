package com.amalitech.communityboard.exception;

/** CB-213: Thrown when an optimistic lock conflict is detected (concurrent update). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
