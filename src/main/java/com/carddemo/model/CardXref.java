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
 * CardXref – JPA entity mapped from COBOL copybook CVACT03Y.
 *
 * COBOL layout (CVACT03Y):
 *   01 CARD-XREF-RECORD.
 *      05 XREF-CARD-NUM     PIC X(16).
 *      05 XREF-CUST-ID      PIC 9(09).
 *      05 XREF-ACCT-ID      PIC 9(11).
 *
 * Replaces:  EXEC CICS READ DATASET(CXACAIX) RIDFLD(acct-id) ...
 */
@Entity
@Table(name = "card_xref")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardXref {

    /** Maps to XREF-CARD-NUM PIC X(16) – primary key */
    @Id
    @Column(name = "xref_card_num", nullable = false, length = 16)
    private String xrefCardNum;

    /** Maps to XREF-CUST-ID PIC 9(09) */
    @Column(name = "xref_cust_id", nullable = false)
    private Long xrefCustId;

    /** Maps to XREF-ACCT-ID PIC 9(11) */
    @Column(name = "xref_acct_id", nullable = false)
    private Long xrefAcctId;
}
