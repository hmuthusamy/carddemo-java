package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVTRA02Y.cpy
 * Data-structure for disclosure group (RECLN = 50)
 * VSAM KSDS - composite key (DIS-ACCT-GROUP-ID + DIS-TRAN-TYPE-CD + DIS-TRAN-CAT-CD)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "disclosure_group")
public class DisclosureGroup {

    /** DIS-ACCT-GROUP-ID PIC X(10) - part of composite key */
    @Id
    @Column(name = "dis_acct_group_id", length = 10, nullable = false)
    private String disAcctGroupId;

    /** DIS-TRAN-TYPE-CD PIC X(02) - part of composite key */
    @Column(name = "dis_tran_type_cd", length = 2, nullable = false)
    private String disTranTypeCd;

    /** DIS-TRAN-CAT-CD PIC 9(04) - part of composite key */
    @Column(name = "dis_tran_cat_cd", precision = 4, nullable = false)
    private Integer disTranCatCd;

    /** DIS-INT-RATE PIC S9(04)V99 - COMP-3 signed decimal interest rate */
    @Column(name = "dis_int_rate", precision = 6, scale = 2)
    private BigDecimal disIntRate;
}
