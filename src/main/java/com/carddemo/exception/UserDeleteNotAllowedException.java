package com.carddemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a delete operation is rejected because it would violate
 * an integrity rule derived from the mainframe environment.
 *
 * <p>In the original COBOL/CICS/RACF setup, deleting the last admin user
 * would have been prevented implicitly by RACF authorisation rules.
 * This exception makes that guard explicit in the Java migration.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserDeleteNotAllowedException extends RuntimeException {

    public UserDeleteNotAllowedException(String message) {
        super(message);
    }
}
