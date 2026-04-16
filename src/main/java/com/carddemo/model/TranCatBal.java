package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entity representing a transaction category balance record.
 * Mirrors the COBOL file TCATBAL-FILE / copybook CVTRA01Y used in CBTRN02C paragraph 2700-UPDATE-TCATBAL.
 *
 * Composite key: (acct_id, type_cd, cat_cd)
 *
 * COBOL logic:
 *   - Read by key; if not found (status 23) → CREATE a new record
 *   - ADD DALYTRAN-AMT TO TRAN-CAT-BAL
 *   - WRITE (create) or REWRITE (update)
 */
@Entity
@Table(name = "tran_cat_bal",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_tran_cat_bal",
               columnNames = {"trancat_acct_id", "trancat_type_cd", "trancat_cd"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranCatBal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Account ID – part of composite key (FD-TRANCAT-ACCT-ID) */
    @Column(name = "trancat_acct_id", nullable = false)
    private Long trancatAcctId;

    /** Transaction type code (FD-TRANCAT-TYPE-CD) */
    @Column(name = "trancat_type_cd", length = 2, nullable = false)
    private String trancatTypeCd;

    /** Transaction category code (FD-TRANCAT-CD) */
    @Column(name = "trancat_cd", nullable = false)
    private Integer trancatCd;

    /**
     * Running balance for this category (TRAN-CAT-BAL).
     * COBOL PIC S9(09)V99 → BigDecimal(11,2).
     * ADD DALYTRAN-AMT TO TRAN-CAT-BAL
     */
    @Column(name = "tran_cat_bal", precision = 11, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal tranCatBal = BigDecimal.ZERO;
}
