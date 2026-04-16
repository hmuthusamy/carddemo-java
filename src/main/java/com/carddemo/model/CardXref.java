package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CardXref – maps to the CARD-XREF-RECORD copybook (CVACT03Y).
 *
 * COBOL FD:  XREF-FILE
 *   FD-XREF-CARD-NUM   PIC X(16)   -- primary key
 *   FD-XREF-CUST-NUM   PIC 9(09)
 *   FD-XREF-ACCT-ID    PIC 9(11)   -- alternate key used in CBACT04C
 *   FD-XREF-FILLER     PIC X(14)
 *
 * CBACT04C uses the alternate key FD-XREF-ACCT-ID to look up the card
 * number associated with an account (paragraph 1110-GET-XREF-DATA).
 */
@Entity
@Table(name = "card_xref",
       indexes = @Index(name = "idx_card_xref_acct_id", columnList = "xref_acct_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardXref {

    /** XREF-CARD-NUM  PIC X(16) */
    @Id
    @Column(name = "xref_card_num", nullable = false, length = 16)
    private String xrefCardNum;

    /** XREF-CUST-NUM  PIC 9(09) */
    @Column(name = "xref_cust_num")
    private Long xrefCustNum;

    /** XREF-ACCT-ID  PIC 9(11) — alternate key */
    @Column(name = "xref_acct_id", nullable = false)
    private Long xrefAcctId;
}
