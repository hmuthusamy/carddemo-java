package com.carddemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested user ID cannot be found in the USRSEC table.
 *
 * <p>COBOL equivalent: DFHRESP(NOTFND) response from CICS READ / DELETE in COUSR03C.
 * Original message: "User ID NOT found..."
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User ID NOT found: " + userId);
    }
}
