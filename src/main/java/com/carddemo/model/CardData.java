package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CardData – JPA entity migrated from the COBOL CVACT02Y copybook / CARDDAT VSAM file.
 *
 * <p>COBOL field mapping:
 * <pre>
 *   CARD-NUM            PIC X(16)   → cardNum         (PK)
 *   CARD-ACCT-ID        PIC 9(11)   → accountId
 *   CARD-CVV-CD         PIC 9(03)   → cvvCode
 *   CARD-EMBOSSED-NAME  PIC X(50)   → embossedName
 *   CARD-EXPIRAION-DATE PIC X(10)   → expirationDate
 *   CARD-ACTIVE-STATUS  PIC X(01)   → activeStatus
 * </pre>
 *
 * <p>Originates from COCRDLIC.CBL § 9500-FILTER-RECORDS which reads CARD-NUM,
 * CARD-ACCT-ID and CARD-ACTIVE-STATUS from the CARDDAT VSAM file.
 */
@Entity
@Table(name = "card_data")
public class CardData {

    /** 16-digit card number – primary key (replaces CARD-NUM PIC X(16)). */
    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    /** 11-digit account id – replaces CARD-ACCT-ID PIC 9(11). */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** 3-digit CVV code – replaces CARD-CVV-CD PIC 9(03). */
    @Column(name = "cvv_code")
    private Integer cvvCode;

    /** Embossed name on card – replaces CARD-EMBOSSED-NAME PIC X(50). */
    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    /** Expiration date – replaces CARD-EXPIRAION-DATE PIC X(10). */
    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

    /**
     * Single-char active status flag – replaces CARD-ACTIVE-STATUS PIC X(01).
     * Typical values: 'Y' = active, 'N' = inactive.
     */
    @Column(name = "active_status", length = 1)
    private String activeStatus;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public CardData() {}

    public CardData(String cardNum, Long accountId, String activeStatus) {
        this.cardNum      = cardNum;
        this.accountId    = accountId;
        this.activeStatus = activeStatus;
    }

    public CardData(String cardNum, Long accountId, Integer cvvCode,
                    String embossedName, String expirationDate, String activeStatus) {
        this.cardNum        = cardNum;
        this.accountId      = accountId;
        this.cvvCode        = cvvCode;
        this.embossedName   = embossedName;
        this.expirationDate = expirationDate;
        this.activeStatus   = activeStatus;
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getCvvCode() {
        return cvvCode;
    }

    public void setCvvCode(Integer cvvCode) {
        this.cvvCode = cvvCode;
    }

    public String getEmbossedName() {
        return embossedName;
    }

    public void setEmbossedName(String embossedName) {
        this.embossedName = embossedName;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    @Override
    public String toString() {
        return "CardData{cardNum='" + cardNum + "', accountId=" + accountId
               + ", activeStatus='" + activeStatus + "'}";
    }
}
