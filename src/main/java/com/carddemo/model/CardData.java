package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CardData – maps to the CARD-RECORD copybook (CVACT02Y).
 *
 * <p>COBOL FD: CARD-FILE, RECORD KEY IS CARD-NUM (PIC X(16))
 *
 * <pre>
 * 01  CARD-RECORD.
 *     05  CARD-NUM                PIC X(16)    -- primary key
 *     05  CARD-ACCT-ID            PIC 9(11)    -- foreign key → AccountData
 *     05  CARD-CVV-CD             PIC 9(03)
 *     05  CARD-EMBOSSED-NAME      PIC X(50)
 *     05  CARD-EXPIRAION-DATE     PIC X(10)
 *     05  CARD-ACTIVE-STATUS      PIC X(01)
 *     05  FILLER                  PIC X(59)
 * </pre>
 *
 * <p>Relationship to CBACT04C:
 * <ul>
 *   <li>{@link CardXref} (CVACT03Y) holds the cross-reference between card numbers
 *       and accounts — it is the file directly read by CBACT04C paragraph 1110-GET-XREF-DATA.</li>
 *   <li>{@code CardData} (CVACT02Y) is the full card master record, providing card-level
 *       details (CVV, embossed name, expiry, status) that are associated with an account
 *       via {@code CARD-ACCT-ID}.</li>
 * </ul>
 *
 * <p>Both entities are required by the CBACT04C migration: {@link CardXref} supports the
 * cross-reference lookup, while {@code CardData} represents the complete card entity
 * as specified in the "CardData and AccountData entities available" requirement.
 */
@Entity
@Table(name = "card_data",
       indexes = @Index(name = "idx_card_data_acct_id", columnList = "card_acct_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardData {

    /**
     * CARD-NUM  PIC X(16) — primary key.
     * The 16-digit card number uniquely identifies a card record.
     */
    @Id
    @Column(name = "card_num", nullable = false, length = 16)
    private String cardNum;

    /**
     * CARD-ACCT-ID  PIC 9(11) — foreign key to {@link AccountData#getAcctId()}.
     * Links the card to its parent account.
     */
    @Column(name = "card_acct_id", nullable = false)
    private Long cardAcctId;

    /**
     * CARD-CVV-CD  PIC 9(03) — card verification value.
     */
    @Column(name = "card_cvv_cd")
    private Integer cardCvvCd;

    /**
     * CARD-EMBOSSED-NAME  PIC X(50) — name printed/embossed on the card.
     */
    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    /**
     * CARD-EXPIRAION-DATE  PIC X(10) — card expiry date (note: COBOL spelling preserved).
     * Stored as a string to match the PIC X(10) COBOL definition (e.g. "2027-12-31").
     */
    @Column(name = "card_expiration_date", length = 10)
    private String cardExpirationDate;

    /**
     * CARD-ACTIVE-STATUS  PIC X(01) — 'Y' active, 'N' inactive.
     */
    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;
}
