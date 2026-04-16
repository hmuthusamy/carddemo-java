package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.io.Serializable;

/**
 * JPA entity representing the DIS-GROUP-RECORD structure from COBOL copybook CVTRA02Y.
 *
 * COBOL mapping (RECLN = 50):
 *   DIS-ACCT-GROUP-ID   PIC X(10)       → String  (part of composite key)
 *   DIS-TRAN-TYPE-CD    PIC X(02)       → String  (part of composite key)
 *   DIS-TRAN-CAT-CD     PIC 9(04)       → Integer (part of composite key)
 *   DIS-INT-RATE        PIC S9(04)V99   → BigDecimal (COMP-3 equivalent, annual %)
 *
 * Interest rate semantics:
 *   The COBOL program divides DIS-INT-RATE by 1200 to convert annual % → monthly fraction.
 *   Example: DIS-INT-RATE = 18.00 → monthly rate = 18.00 / 1200 = 0.015 (1.5 %)
 *
 * Default group: 'DEFAULT   ' (padded to 10 chars) is used when no specific group is found
 * (DISCGRP-STATUS = '23' → key not found, fall back to 1200-A-GET-DEFAULT-INT-RATE).
 */
@Entity
@Table(name = "disclosure_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupKey id;

    /**
     * DIS-INT-RATE PIC S9(04)V99 — annual interest rate percentage.
     * COMP-3 (packed decimal) in COBOL → BigDecimal(6,2) here.
     * Zero value means: no interest charged (guarded by IF DIS-INT-RATE NOT = 0 in COBOL).
     */
    @Column(name = "dis_int_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal disIntRate;

    // ---------------------------------------------------------
    // Composite primary key
    // ---------------------------------------------------------
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DisclosureGroupKey implements Serializable {

        /** DIS-ACCT-GROUP-ID PIC X(10) */
        @Column(name = "dis_acct_group_id", length = 10, nullable = false)
        private String disAcctGroupId;

        /** DIS-TRAN-TYPE-CD PIC X(02) */
        @Column(name = "dis_tran_type_cd", length = 2, nullable = false)
        private String disTranTypeCd;

        /** DIS-TRAN-CAT-CD PIC 9(04) */
        @Column(name = "dis_tran_cat_cd", nullable = false)
        private Integer disTranCatCd;
    }
}
