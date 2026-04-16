package com.carddemo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA Entity representing a Credit Card record.
 *
 * Migrated from COBOL copybook CVACT02Y.cpy (CARD-RECORD layout):
 *   CARD-NUM            PIC X(16)  -> cardNumber
 *   CARD-ACCT-ID        PIC 9(11)  -> accountId
 *   CARD-CVV-CD         PIC 9(3)   -> cvvCode
 *   CARD-EMBOSSED-NAME  PIC X(50)  -> embossedName
 *   CARD-EXPIRAION-DATE PIC X(10)  -> expirationDate
 *   CARD-ACTIVE-STATUS  PIC X(1)   -> activeStatus
 *
 * Maps to COBOL file CARDDAT (CARDFILENAME literal in COCRDSLC).
 */
@Entity
@Table(name = "credit_cards")
public class CreditCard {

    /** 16-digit card number – primary key (CARD-NUM PIC X(16)) */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    /** 11-digit account identifier (CARD-ACCT-ID PIC 9(11)) */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** 3-digit CVV security code (CARD-CVV-CD PIC 9(3)) */
    @Column(name = "cvv_code", nullable = false)
    private Integer cvvCode;

    /** Cardholder name as embossed on card (CARD-EMBOSSED-NAME PIC X(50)) */
    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    /** Card expiration date (CARD-EXPIRAION-DATE PIC X(10), format YYYY-MM-DD) */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Card active status (CARD-ACTIVE-STATUS PIC X(1)).
     * COBOL 88-level values: 'Y' = active, 'N' = inactive.
     */
    @Column(name = "active_status", length = 1)
    private String activeStatus;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CreditCard() {}

    public CreditCard(String cardNumber, Long accountId, Integer cvvCode,
                      String embossedName, LocalDate expirationDate, String activeStatus) {
        this.cardNumber     = cardNumber;
        this.accountId      = accountId;
        this.cvvCode        = cvvCode;
        this.embossedName   = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus   = activeStatus;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getCardNumber()            { return cardNumber; }
    public void   setCardNumber(String v)    { this.cardNumber = v; }

    public Long   getAccountId()             { return accountId; }
    public void   setAccountId(Long v)       { this.accountId = v; }

    public Integer getCvvCode()              { return cvvCode; }
    public void    setCvvCode(Integer v)     { this.cvvCode = v; }

    public String getEmbossedName()          { return embossedName; }
    public void   setEmbossedName(String v)  { this.embossedName = v; }

    public LocalDate getExpirationDate()           { return expirationDate; }
    public void      setExpirationDate(LocalDate v){ this.expirationDate = v; }

    public String getActiveStatus()          { return activeStatus; }
    public void   setActiveStatus(String v)  { this.activeStatus = v; }
}
