package com.carddemo.exception;

/**
 * Thrown when a requested card number is not found in the CARDDAT store.
 * Mirrors COBOL WHEN DFHRESP(NOTFND) branch in 9100-GETCARD-BYACCTCARD:
 *   SET DID-NOT-FIND-ACCTCARD-COMBO TO TRUE
 */
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String cardNumber) {
        super("Did not find cards for this search condition: " + cardNumber);
    }
}
