package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVTRA06Y.cpy
 * Data-structure for DALYTRANsaction record (RECLN = 350)
 * VSAM KSDS - keyed by DALYTRAN-ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_transaction")
public class DailyTransaction {

    /** DALYTRAN-ID PIC X(16) - Primary key */
    @Id
    @Column(name = "dalytran_id", length = 16, nullable = false)
    private String dalytranId;

    /** DALYTRAN-TYPE-CD PIC X(02) */
    @Column(name = "dalytran_type_cd", length = 2)
    private String dalytranTypeCd;

    /** DALYTRAN-CAT-CD PIC 9(04) */
    @Column(name = "dalytran_cat_cd", precision = 4)
    private Integer dalytranCatCd;

    /** DALYTRAN-SOURCE PIC X(10) */
    @Column(name = "dalytran_source", length = 10)
    private String dalytranSource;

    /** DALYTRAN-DESC PIC X(100) */
    @Column(name = "dalytran_desc", length = 100)
    private String dalytranDesc;

    /** DALYTRAN-AMT PIC S9(09)V99 - signed decimal, COMP-3 equivalent */
    @Column(name = "dalytran_amt", precision = 11, scale = 2)
    private BigDecimal dalytranAmt;

    /** DALYTRAN-MERCHANT-ID PIC 9(09) */
    @Column(name = "dalytran_merchant_id", precision = 9)
    private Long dalytranMerchantId;

    /** DALYTRAN-MERCHANT-NAME PIC X(50) */
    @Column(name = "dalytran_merchant_name", length = 50)
    private String dalytranMerchantName;

    /** DALYTRAN-MERCHANT-CITY PIC X(50) */
    @Column(name = "dalytran_merchant_city", length = 50)
    private String dalytranMerchantCity;

    /** DALYTRAN-MERCHANT-ZIP PIC X(10) */
    @Column(name = "dalytran_merchant_zip", length = 10)
    private String dalytranMerchantZip;

    /** DALYTRAN-CARD-NUM PIC X(16) */
    @Column(name = "dalytran_card_num", length = 16)
    private String dalytranCardNum;

    /** DALYTRAN-ORIG-TS PIC X(26) */
    @Column(name = "dalytran_orig_ts", length = 26)
    private String dalytranOrigTs;

    /** DALYTRAN-PROC-TS PIC X(26) */
    @Column(name = "dalytran_proc_ts", length = 26)
    private String dalytranProcTs;
}
