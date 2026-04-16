package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction – maps to the TRAN-RECORD copybook (CVTRA05Y).
 *
 * COBOL FD:  TRANSACT-FILE  (OUTPUT — written by CBACT04C)
 *   TRAN-ID             PIC X(16)
 *   TRAN-TYPE-CD        PIC X(02)   -- '01' for interest
 *   TRAN-CAT-CD         PIC X(04)   -- '05' for interest category
 *   TRAN-SOURCE         PIC X(10)   -- 'System'
 *   TRAN-DESC           PIC X(100)  -- 'Int. for a/c <acct-id>'
 *   TRAN-AMT            PIC S9(9)V99
 *   TRAN-MERCHANT-ID    PIC 9(09)
 *   TRAN-MERCHANT-NAME  PIC X(50)
 *   TRAN-MERCHANT-CITY  PIC X(50)
 *   TRAN-MERCHANT-ZIP   PIC X(10)
 *   TRAN-CARD-NUM       PIC X(16)
 *   TRAN-ORIG-TS        PIC X(26)
 *   TRAN-PROC-TS        PIC X(26)
 */
@Entity
@Table(name = "transaction_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /** TRAN-ID  PIC X(16) */
    @Id
    @Column(name = "tran_id", nullable = false, length = 16)
    private String tranId;

    /** TRAN-TYPE-CD  PIC X(02) */
    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    /** TRAN-CAT-CD  PIC X(04) */
    @Column(name = "tran_cat_cd", length = 4)
    private String tranCatCd;

    /** TRAN-SOURCE  PIC X(10) */
    @Column(name = "tran_source", length = 10)
    private String tranSource;

    /** TRAN-DESC  PIC X(100) */
    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    /** TRAN-AMT  PIC S9(9)V99 */
    @Column(name = "tran_amt", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    /** TRAN-MERCHANT-ID  PIC 9(09) */
    @Column(name = "tran_merchant_id")
    private Long tranMerchantId;

    /** TRAN-MERCHANT-NAME  PIC X(50) */
    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    /** TRAN-MERCHANT-CITY  PIC X(50) */
    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    /** TRAN-MERCHANT-ZIP  PIC X(10) */
    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    /** TRAN-CARD-NUM  PIC X(16) */
    @Column(name = "tran_card_num", length = 16)
    private String tranCardNum;

    /** TRAN-ORIG-TS */
    @Column(name = "tran_orig_ts")
    private LocalDateTime tranOrigTs;

    /** TRAN-PROC-TS */
    @Column(name = "tran_proc_ts")
    private LocalDateTime tranProcTs;
}
