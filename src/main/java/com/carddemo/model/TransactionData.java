package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TransactionData entity – migrated from COBOL copybook CVTRA05Y.
 *
 * COBOL layout (CVTRA05Y):
 *   01 TRAN-RECORD.
 *     05 TRAN-ID             PIC X(16)
 *     05 TRAN-TYPE-CD        PIC X(02)
 *     05 TRAN-CAT-CD         PIC 9(04)
 *     05 TRAN-SOURCE         PIC X(10)
 *     05 TRAN-DESC           PIC X(48)
 *     05 TRAN-AMT            PIC S9(9)V99 COMP-3
 *     05 TRAN-CARD-NUM       PIC X(16)
 *     05 TRAN-MERCHANT-ID    PIC 9(09)
 *     05 TRAN-MERCHANT-NAME  PIC X(50)
 *     05 TRAN-MERCHANT-CITY  PIC X(50)
 *     05 TRAN-MERCHANT-ZIP   PIC X(10)
 *     05 TRAN-ORIG-TS        PIC X(26)
 *     05 TRAN-PROC-TS        PIC X(26)
 */
@Entity
@Table(name = "transaction_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {

    /** TRAN-ID  PIC X(16) – 16-digit sequential transaction identifier */
    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    /** TRAN-TYPE-CD  PIC X(02) – transaction type code (numeric string) */
    @Column(name = "tran_type_cd", length = 2, nullable = false)
    private String tranTypeCd;

    /** TRAN-CAT-CD  PIC 9(04) – category code */
    @Column(name = "tran_cat_cd", length = 4, nullable = false)
    private String tranCatCd;

    /** TRAN-SOURCE  PIC X(10) */
    @Column(name = "tran_source", length = 10, nullable = false)
    private String tranSource;

    /** TRAN-DESC  PIC X(48) */
    @Column(name = "tran_desc", length = 48, nullable = false)
    private String tranDesc;

    /** TRAN-AMT  PIC S9(9)V99  – signed amount, max 9 digits + 2 decimals */
    @Column(name = "tran_amt", precision = 11, scale = 2, nullable = false)
    private BigDecimal tranAmt;

    /** TRAN-CARD-NUM  PIC X(16) – 16-digit card number */
    @Column(name = "tran_card_num", length = 16, nullable = false)
    private String tranCardNum;

    /** TRAN-MERCHANT-ID  PIC 9(09) */
    @Column(name = "tran_merchant_id", length = 9, nullable = false)
    private String tranMerchantId;

    /** TRAN-MERCHANT-NAME  PIC X(50) */
    @Column(name = "tran_merchant_name", length = 50, nullable = false)
    private String tranMerchantName;

    /** TRAN-MERCHANT-CITY  PIC X(50) */
    @Column(name = "tran_merchant_city", length = 50, nullable = false)
    private String tranMerchantCity;

    /** TRAN-MERCHANT-ZIP  PIC X(10) */
    @Column(name = "tran_merchant_zip", length = 10, nullable = false)
    private String tranMerchantZip;

    /** TRAN-ORIG-TS  PIC X(26) – origination timestamp (YYYY-MM-DD) */
    @Column(name = "tran_orig_ts", length = 26, nullable = false)
    private String tranOrigTs;

    /** TRAN-PROC-TS  PIC X(26) – processing timestamp (YYYY-MM-DD) */
    @Column(name = "tran_proc_ts", length = 26, nullable = false)
    private String tranProcTs;
}
