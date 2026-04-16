package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVACT02Y.cpy
 * Data-structure for card entity (RECLN 150)
 * VSAM KSDS - keyed by CARD-NUM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card")
public class AccountView {

    /** CARD-NUM PIC X(16) - Primary key */
    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    /** CARD-ACCT-ID PIC 9(11) */
    @Column(name = "card_acct_id", precision = 11)
    private Long cardAcctId;

    /** CARD-CVV-CD PIC 9(03) */
    @Column(name = "card_cvv_cd", precision = 3)
    private Integer cardCvvCd;

    /** CARD-EMBOSSED-NAME PIC X(50) */
    @Column(name = "card_embossed_name", length = 50)
    private String cardEmbossedName;

    /** CARD-EXPIRAION-DATE PIC X(10) */
    @Column(name = "card_expiration_date", length = 10)
    private String cardExpirationDate;

    /** CARD-ACTIVE-STATUS PIC X(01) */
    @Column(name = "card_active_status", length = 1)
    private String cardActiveStatus;
}
