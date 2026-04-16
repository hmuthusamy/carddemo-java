package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DisclosureGroup – maps to the DIS-GROUP-RECORD copybook (CVTRA02Y).
 *
 * COBOL FD:  DISCGRP-FILE
 *   FD-DISCGRP-KEY:
 *     FD-DIS-ACCT-GROUP-ID  PIC X(10)
 *     FD-DIS-TRAN-TYPE-CD   PIC X(02)
 *     FD-DIS-TRAN-CAT-CD    PIC 9(04)
 *   FD-DISCGRP-DATA         PIC X(34)   — contains DIS-INT-RATE
 *
 * CBACT04C uses this for interest rate lookup (paragraph 1200-GET-INTEREST-RATE).
 * When the specific group is not found (file status 23), it falls back to
 * account group id = 'DEFAULT'.
 */
@Entity
@Table(name = "disclosure_group")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisclosureGroup {

    @EmbeddedId
    private DisclosureGroupKey id;

    /** DIS-INT-RATE — annual interest rate (%) stored in DISCGRP-DATA */
    @Column(name = "dis_int_rate", precision = 6, scale = 4)
    private BigDecimal disIntRate;

    // ---------------------------------------------------------------
    // Composite key
    // ---------------------------------------------------------------
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisclosureGroupKey implements java.io.Serializable {

        /** FD-DIS-ACCT-GROUP-ID  PIC X(10) */
        @Column(name = "dis_acct_group_id", nullable = false, length = 10)
        private String disAcctGroupId;

        /** FD-DIS-TRAN-TYPE-CD  PIC X(02) */
        @Column(name = "dis_tran_type_cd", nullable = false, length = 2)
        private String disTranTypeCd;

        /** FD-DIS-TRAN-CAT-CD  PIC 9(04) */
        @Column(name = "dis_tran_cat_cd", nullable = false)
        private Integer disTranCatCd;
    }
}
