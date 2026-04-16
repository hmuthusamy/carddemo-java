package com.carddemo.model;

/**
 * Maps to FD-XREFFILE-REC / CVACT03Y copybook.
 *
 * Layout (50 bytes):
 *   FD-XREF-CARD-NUM  PIC X(16)  – the indexed key
 *   FD-XREF-DATA      PIC X(34)
 *     XREF-CUST-ID    PIC X(09)  at offset 0 of FD-XREF-DATA
 *     XREF-ACCT-ID    PIC 9(11)  at offset 9 of FD-XREF-DATA
 *     (padding)
 *
 * CBSTM03A uses XREF-CARD-NUM, XREF-CUST-ID, XREF-ACCT-ID.
 */
public class CardXref {

    /** FD-XREF-CARD-NUM / XREF-CARD-NUM PIC X(16) */
    private String cardNumber;

    /** XREF-CUST-ID PIC X(09) */
    private String customerId;

    /** XREF-ACCT-ID PIC 9(11) */
    private String accountId;

    public CardXref() {}

    public CardXref(String cardNumber, String customerId, String accountId) {
        this.cardNumber = cardNumber;
        this.customerId = customerId;
        this.accountId = accountId;
    }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    @Override
    public String toString() {
        return "CardXref{card='" + cardNumber + "', custId='" + customerId
            + "', acctId='" + accountId + "'}";
    }
}
