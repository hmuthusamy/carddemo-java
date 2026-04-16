package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a transaction record.
 *
 * <p>Migrated from COBOL copybook CVTRA05Y (TRAN-RECORD, RECLN=350):
 * <pre>
 *   05 TRAN-ID              PIC X(16)
 *   05 TRAN-TYPE-CD         PIC X(02)
 *   05 TRAN-CAT-CD          PIC 9(04)
 *   05 TRAN-SOURCE          PIC X(10)
 *   05 TRAN-DESC            PIC X(100)
 *   05 TRAN-AMT             PIC S9(09)V99
 *   05 TRAN-MERCHANT-ID     PIC 9(09)
 *   05 TRAN-MERCHANT-NAME   PIC X(50)
 *   05 TRAN-MERCHANT-CITY   PIC X(50)
 *   05 TRAN-MERCHANT-ZIP    PIC X(10)
 *   05 TRAN-CARD-NUM        PIC X(16)
 *   05 TRAN-ORIG-TS         PIC X(26)
 *   05 TRAN-PROC-TS         PIC X(26)
 * </pre>
 */
@Entity
@Table(name = "transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {

    /** Maps to TRAN-ID PIC X(16) – primary key. */
    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    /** Maps to TRAN-TYPE-CD PIC X(02). */
    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    /** Maps to TRAN-CAT-CD PIC 9(04). */
    @Column(name = "tran_cat_cd")
    private Integer tranCatCd;

    /** Maps to TRAN-SOURCE PIC X(10). */
    @Column(name = "tran_source", length = 10)
    private String tranSource;

    /** Maps to TRAN-DESC PIC X(100). */
    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    /**
     * Maps to TRAN-AMT PIC S9(09)V99.
     * Stored as DECIMAL(11,2) to preserve signed 9-digit integer + 2 decimal places.
     */
    @Column(name = "tran_amt", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    /** Maps to TRAN-MERCHANT-ID PIC 9(09). */
    @Column(name = "tran_merchant_id")
    private Long tranMerchantId;

    /** Maps to TRAN-MERCHANT-NAME PIC X(50). */
    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    /** Maps to TRAN-MERCHANT-CITY PIC X(50). */
    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    /** Maps to TRAN-MERCHANT-ZIP PIC X(10). */
    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    /** Maps to TRAN-CARD-NUM PIC X(16). Account identifier used for filtering. */
    @Column(name = "tran_card_num", length = 16)
    private String tranCardNum;

    /**
     * Maps to TRAN-ORIG-TS PIC X(26).
     * Original transaction origination timestamp. Used for date-range filtering
     * and DESC ordering (replaces CICS READNEXT sequential scan order).
     */
    @Column(name = "tran_orig_ts")
    private LocalDateTime tranOrigTs;

    /**
     * Maps to TRAN-PROC-TS PIC X(26).
     * Transaction processing timestamp.
     */
    @Column(name = "tran_proc_ts")
    private LocalDateTime tranProcTs;
}
