package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity mapping for CARDDAT VSAM file (CVACT02Y copybook).
 *
 * COBOL mapping:
 *   CARD-RECORD               PIC X(150)
 *     CARD-NUM                PIC X(16)   -> cardNumber
 *     CARD-ACCT-ID            PIC 9(11)   -> accountId
 *     CARD-CVV-CD             PIC 9(03)   -> cvvCode
 *     CARD-EMBOSSED-NAME      PIC X(50)   -> embossedName
 *     CARD-EXPIRAION-DATE     PIC X(10)   -> expirationDate  (YYYY-MM-DD)
 *     CARD-ACTIVE-STATUS      PIC X(01)   -> activeStatus    ('Y'/'N')
 */
@Entity
@Table(name = "card_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardData {

    /** CARD-NUM  PIC X(16) – primary key (VSAM RIDFLD) */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    /** CARD-ACCT-ID  PIC 9(11) */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** CARD-CVV-CD  PIC 9(03) */
    @Column(name = "cvv_code", length = 3)
    private String cvvCode;

    /** CARD-EMBOSSED-NAME  PIC X(50) */
    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    /**
     * CARD-EXPIRAION-DATE  PIC X(10)
     * Stored as YYYY-MM-DD matching COBOL STRING CCUP-NEW-EXPYEAR '-' CCUP-NEW-EXPMON '-' CCUP-NEW-EXPDAY
     */
    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

    /** CARD-ACTIVE-STATUS  PIC X(01) – 'Y' active / 'N' inactive */
    @Column(name = "active_status", length = 1)
    private String activeStatus;

    /**
     * Optimistic-lock version column – replaces the COBOL 9300-CHECK-CHANGE-IN-REC
     * stale-data detection paragraph.  The JPA provider increments this on every
     * REWRITE (repository.save()), and throws OptimisticLockException if a
     * concurrent update is detected.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
