package com.carddemo.exception;

/**
 * Thrown when a userId lookup in the USRSEC repository returns empty.
 * Equivalent to DFHRESP(NOTFND) in COUSR02C READ-USER-SEC-FILE paragraph.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User ID NOT found: " + userId);
    }
}
