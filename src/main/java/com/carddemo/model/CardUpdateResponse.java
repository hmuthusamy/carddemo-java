package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST response for card update / retrieval operations.
 *
 * Maps to COBOL WS-INFO-MSG (PIC X(40)) + CARD-UPDATE-RECORD fields.
 * Status codes mirror the CCUP-CHANGE-ACTION 88-level values:
 *   SUCCESS  -> CCUP-CHANGES-OKAYED-AND-DONE  ('C')
 *   CONFLICT -> DATA-WAS-CHANGED-BEFORE-UPDATE
 *   ERROR    -> CCUP-CHANGES-OKAYED-BUT-FAILED ('F') / CCUP-CHANGES-OKAYED-LOCK-ERROR ('L')
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardUpdateResponse {

    public enum Status {
        /** CCUP-CHANGES-OKAYED-AND-DONE — "Changes committed to database" */
        SUCCESS,
        /** DATA-WAS-CHANGED-BEFORE-UPDATE — "Record changed by some one else. Please review" */
        CONFLICT,
        /** CCUP-CHANGES-OKAYED-LOCK-ERROR / CCUP-CHANGES-OKAYED-BUT-FAILED */
        ERROR,
        /** Card not found in CARDDAT */
        NOT_FOUND
    }

    private Status status;

    /** WS-INFO-MSG PIC X(40) — human-readable outcome message. */
    private String message;

    /** The persisted card data after update (null on error/not-found). */
    private CardData card;
}
