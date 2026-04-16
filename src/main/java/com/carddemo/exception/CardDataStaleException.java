package com.carddemo.exception;

/**
 * Thrown when the card record was modified by another transaction between the
 * READ and the REWRITE.  Mirrors COBOL paragraph 9300-CHECK-CHANGE-IN-REC:
 *   SET DATA-WAS-CHANGED-BEFORE-UPDATE TO TRUE
 *   WS-RETURN-MSG: 'Record changed by some one else. Please review'
 */
public class CardDataStaleException extends RuntimeException {

    public CardDataStaleException(String cardNumber) {
        super("Record changed by someone else. Please review card: " + cardNumber);
    }
}
