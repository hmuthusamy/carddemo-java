package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TransactionCategoryBalance – maps to the TRAN-CAT-BAL-RECORD copybook (CVTRA01Y).
 *
 * COBOL FD:  TCATBAL-FILE
 *   FD-TRAN-CAT-KEY:
 *     FD-TRANCAT-ACCT-ID    PIC 9(11)
 *     FD-TRANCAT-TYPE-CD    PIC X(02)
 *     FD-TRANCAT-CD         PIC 9(04)
 *   FD-FD-TRAN-CAT-DATA     PIC X(33)
 *
 * CBACT04C reads this file sequentially, grouped by account.
 * For each record it looks up the interest rate and computes monthly interest.
 */
@Entity
@Table(name = "tran_cat_bal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryKey id;

    /** TRAN-CAT-BAL  — balance used in interest calculation */
    @Column(name = "tran_cat_bal", precision = 14, scale = 2)
    private BigDecimal tranCatBal;

    // ---------------------------------------------------------------
    // Composite key
    // ---------------------------------------------------------------
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionCategoryKey implements java.io.Serializable {

        /** TRANCAT-ACCT-ID  PIC 9(11) */
        @Column(name = "trancat_acct_id", nullable = false)
        private Long transcatAcctId;

        /** TRANCAT-TYPE-CD  PIC X(02) */
        @Column(name = "trancat_type_cd", nullable = false, length = 2)
        private String transcatTypeCd;

        /** TRANCAT-CD  PIC 9(04) */
        @Column(name = "trancat_cd", nullable = false)
        private Integer transcatCd;
    }
}
