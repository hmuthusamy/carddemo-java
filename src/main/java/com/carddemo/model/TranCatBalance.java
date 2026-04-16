package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVTRA01Y.cpy
 * Data-structure for transaction category balance (RECLN = 50)
 * VSAM KSDS - composite key (TRANCAT-ACCT-ID + TRANCAT-TYPE-CD + TRANCAT-CD)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tran_cat_bal")
public class TranCatBalance {

    /** TRANCAT-ACCT-ID PIC 9(11) - part of composite key */
    @Id
    @Column(name = "trancat_acct_id", precision = 11, nullable = false)
    private Long trancatAcctId;

    /** TRANCAT-TYPE-CD PIC X(02) - part of composite key */
    @Column(name = "trancat_type_cd", length = 2, nullable = false)
    private String trancatTypeCd;

    /** TRANCAT-CD PIC 9(04) - part of composite key */
    @Column(name = "trancat_cd", precision = 4, nullable = false)
    private Integer trancatCd;

    /** TRAN-CAT-BAL PIC S9(09)V99 - COMP-3 packed decimal */
    @Column(name = "tran_cat_bal", precision = 11, scale = 2)
    private BigDecimal tranCatBal;
}
