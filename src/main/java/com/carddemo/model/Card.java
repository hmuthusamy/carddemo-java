package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the CARDS table.
 *
 * <p>Migrated from COBOL copybook CVACT02Y.cpy (CARD-RECORD, RECLN=150).
 *
 * <pre>
 * Column type mapping:
 *   card_number       VARCHAR(16)   ← PIC X(16)
 *   account_id        BIGINT        ← PIC 9(11)
 *   cvv_code          SMALLINT      ← PIC 9(03)
 *   embossed_name     VARCHAR(50)   ← PIC X(50)
 *   expiration_date   VARCHAR(10)   ← PIC X(10)
 *   active_status     CHAR(1)       ← PIC X(01)
 * </pre>
 */
@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    /** CARD-NUM PIC X(16) – primary key. Maps to DDL: VARCHAR(16) NOT NULL */
    @Id
    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber;

    /** CARD-ACCT-ID PIC 9(11) – FK to accounts. Maps to DDL: BIGINT NOT NULL */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** CARD-CVV-CD PIC 9(03). Maps to DDL: SMALLINT NOT NULL */
    @Column(name = "cvv_code", nullable = false)
    private Short cvvCode;

    /** CARD-EMBOSSED-NAME PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "embossed_name", nullable = false, length = 50)
    private String embossedName;

    /** CARD-EXPIRAION-DATE PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "expiration_date", nullable = false, length = 10)
    private String expirationDate;

    /** CARD-ACTIVE-STATUS PIC X(01). Maps to DDL: CHAR(1) NOT NULL DEFAULT 'Y' */
    @Column(name = "active_status", nullable = false, length = 1)
    private String activeStatus;

    /** Many-side navigation to parent Account (join on account_id). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;
}
