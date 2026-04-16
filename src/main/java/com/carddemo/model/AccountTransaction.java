package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVACT03Y.cpy
 * Data-structure for card cross-reference (RECLN 50)
 * VSAM KSDS - keyed by XREF-CARD-NUM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_xref")
public class AccountTransaction {

    /** XREF-CARD-NUM PIC X(16) - Primary key */
    @Id
    @Column(name = "xref_card_num", length = 16, nullable = false)
    private String xrefCardNum;

    /** XREF-CUST-ID PIC 9(09) */
    @Column(name = "xref_cust_id", precision = 9)
    private Long xrefCustId;

    /** XREF-ACCT-ID PIC 9(11) */
    @Column(name = "xref_acct_id", precision = 11)
    private Long xrefAcctId;
}
