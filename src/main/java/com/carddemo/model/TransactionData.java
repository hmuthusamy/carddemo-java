package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a credit-card transaction.
 * Mirrors the COBOL copybooks CVTRA05Y / CVTRA06Y used in CBTRN02C.
 *
 * Status lifecycle:
 *   PENDING  → validated by CBTRN02C → APPROVED or REJECTED
 *   APPROVED → posted to account by cbtrn02cJob
 *   POSTED   → balance already reflected on AccountData
 */
@Entity
@Table(name = "transaction_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {

    /** 16-char transaction identifier (FD-TRAN-ID / DALYTRAN-ID) */
    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    /** Card number (DALYTRAN-CARD-NUM) – foreign key into XREF */
    @Column(name = "tran_card_num", length = 16, nullable = false)
    private String tranCardNum;

    /** Account ID resolved via XREF (XREF-ACCT-ID) */
    @Column(name = "tran_acct_id", nullable = false)
    private Long tranAcctId;

    /**
     * Transaction type code (DALYTRAN-TYPE-CD).
     * Common values: "SA" sale, "CR" credit/refund.
     */
    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    /** Transaction category code (DALYTRAN-CAT-CD) */
    @Column(name = "tran_cat_cd")
    private Integer tranCatCd;

    /** Source channel (DALYTRAN-SOURCE) */
    @Column(name = "tran_source", length = 10)
    private String tranSource;

    /** Description (DALYTRAN-DESC) */
    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    /**
     * Transaction amount (DALYTRAN-AMT / TRAN-AMT).
     * Positive = debit/charge; negative = credit/refund.
     * COBOL PIC S9(09)V99 → BigDecimal(11,2).
     */
    @Column(name = "tran_amt", precision = 11, scale = 2, nullable = false)
    private BigDecimal tranAmt;

    /** Merchant identifier (DALYTRAN-MERCHANT-ID) */
    @Column(name = "tran_merchant_id")
    private Long tranMerchantId;

    /** Merchant name (DALYTRAN-MERCHANT-NAME) */
    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    /** Merchant city (DALYTRAN-MERCHANT-CITY) */
    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    /** Merchant ZIP (DALYTRAN-MERCHANT-ZIP) */
    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    /** Original timestamp from the daily file (DALYTRAN-ORIG-TS) */
    @Column(name = "tran_orig_ts")
    private LocalDateTime tranOrigTs;

    /** Processing timestamp set when posted (TRAN-PROC-TS / DB2-FORMAT-TS) */
    @Column(name = "tran_proc_ts")
    private LocalDateTime tranProcTs;

    /**
     * Processing status.
     * Values: PENDING, APPROVED, REJECTED, POSTED.
     * CBTRN02C reads APPROVED records and sets them to POSTED after balance update.
     */
    @Column(name = "tran_status", length = 10, nullable = false)
    @Builder.Default
    private String tranStatus = "PENDING";

    /** Rejection reason code (WS-VALIDATION-FAIL-REASON) – 0 means approved */
    @Column(name = "tran_reject_reason")
    private Integer tranRejectReason;

    /** Human-readable rejection description (WS-VALIDATION-FAIL-REASON-DESC) */
    @Column(name = "tran_reject_desc", length = 100)
    private String tranRejectDesc;
}
