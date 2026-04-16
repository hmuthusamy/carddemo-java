package com.carddemo.dto;

import com.carddemo.model.UserStatus;

/**
 * Response DTO returned by the delete endpoint.
 *
 * <p>Mirrors the success message built by COUSR03C DELETE-USER-SEC-FILE paragraph:
 * <pre>
 *   STRING 'User ' DELIMITED BY SIZE
 *          SEC-USR-ID DELIMITED BY SPACE
 *          ' has been deleted ...' DELIMITED BY SIZE
 *     INTO WS-MESSAGE
 * </pre>
 *
 * @param userId  the deleted user's ID
 * @param message human-readable outcome message
 * @param status  final {@link UserStatus} of the record (INACTIVE after soft-delete)
 */
public record UserDeleteResponse(String userId, String message, UserStatus status) {

    /** Factory method replicating the COBOL success message format. */
    public static UserDeleteResponse success(String userId) {
        return new UserDeleteResponse(
                userId,
                "User " + userId.trim() + " has been deleted ...",
                UserStatus.INACTIVE
        );
    }
}
