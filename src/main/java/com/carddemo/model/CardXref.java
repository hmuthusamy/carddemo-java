package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CardXref entity – migrated from COBOL copybook CVACT03Y (CCXREF / CXACAIX).
 *
 * COBOL layout (CVACT03Y):
 *   01 CARD-XREF-RECORD.
 *     05 XREF-CARD-NUM    PIC X(16)
 *     05 XREF-CUST-ID     PIC 9(09)
 *     05 XREF-ACCT-ID     PIC 9(11)
 */
@Entity
@Table(name = "card_xref")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardXref {

    /** XREF-CARD-NUM  PIC X(16) – primary key */
    @Id
    @Column(name = "xref_card_num", length = 16, nullable = false)
    private String xrefCardNum;

    /** XREF-CUST-ID  PIC 9(09) */
    @Column(name = "xref_cust_id", length = 9, nullable = false)
    private String xrefCustId;

    /** XREF-ACCT-ID  PIC 9(11) */
    @Column(name = "xref_acct_id", length = 11, nullable = false)
    private String xrefAcctId;
}
