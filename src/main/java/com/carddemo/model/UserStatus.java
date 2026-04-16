package com.carddemo.model;

/**
 * Represents the status of a user account.
 *
 * <p>Migration note (COUSR03C → Java):
 * The original COBOL program performed a physical CICS DELETE on the USRSEC VSAM file.
 * Per the migration task specification, this has been intentionally changed to a
 * soft-delete (INACTIVE) approach to preserve audit history and support potential
 * account recovery.  The behaviour change is documented here and in
 * {@link com.carddemo.service.UserDeleteService}.
 */
public enum UserStatus {
    /** User may log in and perform transactions. */
    ACTIVE,
    /**
     * User has been soft-deleted (COUSR03C migration equivalent of CICS DELETE).
     * The record is retained for audit purposes but the user cannot authenticate.
     */
    INACTIVE
}
