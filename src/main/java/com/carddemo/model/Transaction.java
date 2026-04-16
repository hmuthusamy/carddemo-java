package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA entity for the TRANSACTIONS table.
 *
 * <p>Migrated from COBOL copybook CVTRA05Y.cpy (TRAN-RECORD, RECLN=350).
 *
 * <pre>
 * Column type mapping:
 *   transaction_id        VARCHAR(16)    ← PIC X(16)
 *   transaction_type_code CHAR(2)        ← PIC X(02)
 *   transaction_cat_code  SMALLINT       ← PIC 9(04)
 *   transaction_source    VARCHAR(10)    ← PIC X(10)
 *   transaction_desc      VARCHAR(100)   ← PIC X(100)
 *   transaction_amount    NUMERIC(11,2)  ← PIC S9(09)V99
 *   merchant_id           INTEGER        ← PIC 9(09)
 *   merchant_name         VARCHAR(50)    ← PIC X(50)
 *   merchant_city         VARCHAR(50)    ← PIC X(50)
 *   merchant_zip          VARCHAR(10)    ← PIC X(10)
 *   card_number           VARCHAR(16)    ← PIC X(16)
 *   orig_timestamp        VARCHAR(26)    ← PIC X(26)
 *   proc_timestamp        VARCHAR(26)    ← PIC X(26)
 * </pre>
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /** TRAN-ID PIC X(16) – primary key. Maps to DDL: VARCHAR(16) NOT NULL */
    @Id
    @Column(name = "transaction_id", nullable = false, length = 16)
    private String transactionId;

    /** TRAN-TYPE-CD PIC X(02). Maps to DDL: CHAR(2) NOT NULL */
    @Column(name = "transaction_type_code", nullable = false, length = 2)
    private String transactionTypeCode;

    /** TRAN-CAT-CD PIC 9(04). Maps to DDL: SMALLINT NOT NULL */
    @Column(name = "transaction_cat_code", nullable = false)
    private Short transactionCatCode;

    /** TRAN-SOURCE PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "transaction_source", nullable = false, length = 10)
    private String transactionSource;

    /** TRAN-DESC PIC X(100). Maps to DDL: VARCHAR(100) NOT NULL */
    @Column(name = "transaction_desc", nullable = false, length = 100)
    private String transactionDesc;

    /** TRAN-AMT PIC S9(09)V99. Maps to DDL: NUMERIC(11,2) NOT NULL */
    @Column(name = "transaction_amount", nullable = false, precision = 11, scale = 2)
    private BigDecimal transactionAmount;

    /** TRAN-MERCHANT-ID PIC 9(09). Maps to DDL: INTEGER NOT NULL */
    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    /** TRAN-MERCHANT-NAME PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "merchant_name", nullable = false, length = 50)
    private String merchantName;

    /** TRAN-MERCHANT-CITY PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "merchant_city", nullable = false, length = 50)
    private String merchantCity;

    /** TRAN-MERCHANT-ZIP PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "merchant_zip", nullable = false, length = 10)
    private String merchantZip;

    /** TRAN-CARD-NUM PIC X(16) – FK to cards. Maps to DDL: VARCHAR(16) NOT NULL */
    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber;

    /** TRAN-ORIG-TS PIC X(26). Maps to DDL: VARCHAR(26) NOT NULL */
    @Column(name = "orig_timestamp", nullable = false, length = 26)
    private String origTimestamp;

    /** TRAN-PROC-TS PIC X(26). Maps to DDL: VARCHAR(26) NOT NULL */
    @Column(name = "proc_timestamp", nullable = false, length = 26)
    private String procTimestamp;

    /** Many-side navigation to parent Card (join on card_number). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", insertable = false, updatable = false)
    private Card card;
}
