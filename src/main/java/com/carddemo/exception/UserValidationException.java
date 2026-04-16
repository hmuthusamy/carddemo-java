package com.carddemo.exception;

/**
 * Thrown when UPDATE-USER-INFO field-level validation fails.
 * Mirrors each WHEN branch inside the EVALUATE TRUE block of
 * COUSR02C UPDATE-USER-INFO paragraph.
 */
public class UserValidationException extends RuntimeException {

    public UserValidationException(String message) {
        super(message);
    }
}
