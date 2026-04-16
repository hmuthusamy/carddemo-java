package com.carddemo.service;

/**
 * Thrown when a new-user-add request targets a userId that already exists.
 *
 * Maps to the COBOL WRITE-USER-SEC-FILE paragraph response handling in COUSR01C:
 *
 *   WHEN DFHRESP(DUPKEY)
 *   WHEN DFHRESP(DUPREC)
 *       MOVE 'Y'  TO WS-ERR-FLG
 *       MOVE 'User ID already exist...' TO WS-MESSAGE
 *
 * Source: COUSR01C.CBL CardDemo v1.0-15-g27d6c6f 2022-07-19
 */
public class UserAlreadyExistsException extends RuntimeException {

    private final String userId;

    /**
     * @param userId the duplicate SEC-USR-ID value
     */
    public UserAlreadyExistsException(String userId) {
        // COBOL message: "User ID already exist..."
        super("User ID already exist: " + userId);
        this.userId = userId;
    }

    /** The duplicate user identifier. */
    public String getUserId() {
        return userId;
    }
}
