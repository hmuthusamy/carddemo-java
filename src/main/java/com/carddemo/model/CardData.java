package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a card record from the CARDFILE VSAM dataset.
 *
 * <p>Derived from COBOL copybook CVACT02Y (record length 150):
 * <pre>
 *  01  CARD-RECORD.
 *      05  CARD-NUM                PIC X(16)
 *      05  CARD-ACCT-ID            PIC 9(11)
 *      05  CARD-CVV-CD             PIC 9(03)
 *      05  CARD-EMBOSSED-NAME      PIC X(50)
 *      05  CARD-EXPIRAION-DATE     PIC X(10)
 *      05  CARD-ACTIVE-STATUS      PIC X(01)
 *      05  FILLER                  PIC X(59)
 * </pre>
 */
@Entity
@Table(name = "card_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardData {

    /** CARD-NUM PIC X(16) – primary key / card number */
    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    /** CARD-ACCT-ID PIC 9(11) – associated account identifier */
    @Column(name = "card_acct_id", nullable = false)
    private Long cardAcctId;

    /** CARD-CVV-CD PIC 9(03) – card verification value */
    @Column(name = "card_cvv_cd")
    private Integer cardCvvCd;

    /** CARD-EMBOSSED-NAME PIC X(50) – name printed on card */
    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    /** CARD-EXPIRAION-DATE PIC X(10) – expiration date (COBOL spelling preserved) */
    @Column(name = "card_expiration_date", length = 10)
    private String cardExpirationDate;

    /** CARD-ACTIVE-STATUS PIC X(01) – 'Y' = active, 'N' = inactive */
    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;
}
