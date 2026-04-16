package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVTRA05Y.cpy
 * Data-structure for TRANsaction record (RECLN = 350)
 * VSAM KSDS - keyed by TRAN-ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction")
public class TransactionData {

    /** TRAN-ID PIC X(16) - Primary key */
    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    /** TRAN-TYPE-CD PIC X(02) */
    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    /** TRAN-CAT-CD PIC 9(04) */
    @Column(name = "tran_cat_cd", precision = 4)
    private Integer tranCatCd;

    /** TRAN-SOURCE PIC X(10) */
    @Column(name = "tran_source", length = 10)
    private String tranSource;

    /** TRAN-DESC PIC X(100) */
    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    /** TRAN-AMT PIC S9(09)V99 - signed 9-digit + 2 decimal (COMP-3 equivalent) */
    @Column(name = "tran_amt", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    /** TRAN-MERCHANT-ID PIC 9(09) */
    @Column(name = "tran_merchant_id", precision = 9)
    private Long tranMerchantId;

    /** TRAN-MERCHANT-NAME PIC X(50) */
    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    /** TRAN-MERCHANT-CITY PIC X(50) */
    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    /** TRAN-MERCHANT-ZIP PIC X(10) */
    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    /** TRAN-CARD-NUM PIC X(16) */
    @Column(name = "tran_card_num", length = 16)
    private String tranCardNum;

    /** TRAN-ORIG-TS PIC X(26) */
    @Column(name = "tran_orig_ts", length = 26)
    private String tranOrigTs;

    /** TRAN-PROC-TS PIC X(26) */
    @Column(name = "tran_proc_ts", length = 26)
    private String tranProcTs;
}
