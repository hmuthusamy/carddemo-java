package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing CARD-XREF-RECORD from COBOL copybook CVACT03Y.
 *
 * COBOL mapping (RECLN = 50):
 *   XREF-CARD-NUM   PIC X(16)  → String  (primary key — RECORD KEY in VSAM)
 *   XREF-CUST-ID    PIC 9(09)  → Long
 *   XREF-ACCT-ID    PIC 9(11)  → Long    (alternate key — used for interest lookup)
 *   FILLER          PIC X(14)  → (ignored)
 *
 * CBACT03C reads this file sequentially and displays each record.
 * CBACT04C uses the alternate key (XREF-ACCT-ID) to look up the card number
 * that will be stamped on generated interest transactions.
 */
@Entity
@Table(name = "card_xref",
        indexes = @Index(name = "idx_card_xref_acct_id", columnList = "xref_acct_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardXref {

    /** XREF-CARD-NUM PIC X(16) — primary key (VSAM RECORD KEY) */
    @Id
    @Column(name = "xref_card_num", length = 16, nullable = false)
    private String xrefCardNum;

    /** XREF-CUST-ID PIC 9(09) */
    @Column(name = "xref_cust_id", precision = 9)
    private Long xrefCustId;

    /** XREF-ACCT-ID PIC 9(11) — alternate VSAM key; FK to account_data */
    @Column(name = "xref_acct_id", precision = 11)
    private Long xrefAcctId;
}
