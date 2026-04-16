package com.carddemo.model;

/**
 * DTO representing validated search criteria for credit card lookup.
 *
 * Migrated from COBOL copybook CVCRD01Y.cpy (CC-WORK-AREA):
 *   CC-ACCT-ID    PIC 9(11)  -> accountId   (CDEMO-ACCT-ID)
 *   CC-CARD-NUM   PIC X(16)  -> cardNumber  (CDEMO-CARD-NUM)
 *
 * Validation rules preserved from COCRDSLC paragraphs
 *   2210-EDIT-ACCOUNT  – account must be 11-digit numeric, non-zero
 *   2220-EDIT-CARD     – card number must be 16-digit numeric, non-zero
 *   Cross-field edit   – at least one criterion must be supplied
 */
public class CardSearchRequest {

    /**
     * 11-digit account identifier filter.
     * Maps to CC-ACCT-ID / CDEMO-ACCT-ID in COBOL commarea.
     * Validation: must be numeric, non-zero, at most 11 digits.
     */
    private Long accountId;

    /**
     * 16-digit card number filter.
     * Maps to CC-CARD-NUM / CDEMO-CARD-NUM in COBOL commarea.
     * Validation: must be numeric, non-zero, exactly 16 digits.
     */
    private String cardNumber;

    /**
     * Active-status filter (Y / N).
     * Maps to CARD-ACTIVE-STATUS in CVACT02Y.
     */
    private String status;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CardSearchRequest() {}

    public CardSearchRequest(Long accountId, String cardNumber, String status) {
        this.accountId  = accountId;
        this.cardNumber = cardNumber;
        this.status     = status;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long   getAccountId()           { return accountId; }
    public void   setAccountId(Long v)     { this.accountId = v; }

    public String getCardNumber()          { return cardNumber; }
    public void   setCardNumber(String v)  { this.cardNumber = v; }

    public String getStatus()              { return status; }
    public void   setStatus(String v)      { this.status = v; }
}
