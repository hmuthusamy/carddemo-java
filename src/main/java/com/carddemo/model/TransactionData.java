package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a credit card transaction record.
 *
 * <p>Migrated from COBOL copybook CVTRA05Y / CVTRA06Y (CardDemo).
 * The DALYTRAN-FILE layout was: 16-char transaction ID + 334-char data block,
 * covering card number, category code, type code, source, description,
 * amount, merchant details, and timestamps.
 *
 * <p>All monetary fields use {@link BigDecimal} to preserve precision and
 * avoid floating-point rounding errors that COBOL PIC 9(n)V9(m) fields
 * would expose in a binary-decimal environment.
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {

    /** 16-character transaction ID (DALYTRAN-ID / FD-TRAN-ID). */
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    private String transactionId;

    /**
     * Credit-card number (DALYTRAN-CARD-NUM).
     * Used to cross-reference the XREF file → account.
     */
    @Column(name = "card_number", length = 16)
    private String cardNumber;

    /** Category code (DALYTRAN-CAT-CD), e.g. "01" = purchase. */
    @Column(name = "category_code", length = 4)
    private String categoryCode;

    /** Transaction type code (DALYTRAN-TYPE-CD). */
    @Column(name = "type_code", length = 2)
    private String typeCode;

    /** Transaction source (DALYTRAN-SOURCE). */
    @Column(name = "source", length = 10)
    private String source;

    /** Free-text description (DALYTRAN-DESC). */
    @Column(name = "description", length = 100)
    private String description;

    /**
     * Transaction amount (DALYTRAN-AMT). COBOL PIC S9(9)V99 COMP-3.
     * Stored as BigDecimal(11,2) to match the original 9-digit integer + 2 decimals.
     */
    @Column(name = "amount", precision = 11, scale = 2)
    private BigDecimal amount;

    /** Merchant ID (DALYTRAN-MERCHANT-ID). */
    @Column(name = "merchant_id", length = 9)
    private String merchantId;

    /** Merchant name (DALYTRAN-MERCHANT-NAME). */
    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    /** Merchant city (DALYTRAN-MERCHANT-CITY). */
    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    /** Merchant zip code (DALYTRAN-MERCHANT-ZIP). */
    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    /**
     * Account ID resolved via XREF lookup (XREF-ACCT-ID / ACCT-ID).
     * Null when the card number cannot be verified.
     */
    @Column(name = "account_id")
    private Long accountId;

    /**
     * Transaction date derived from the timestamp in the record.
     * Corresponds to DALYTRAN-ORIG-TS date portion.
     */
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    /**
     * Full origination timestamp (DALYTRAN-ORIG-TS).
     * Format in COBOL: PIC X(26) "YYYY-MM-DD HH:MM:SS.ssssss".
     */
    @Column(name = "orig_timestamp")
    private LocalDateTime origTimestamp;

    /**
     * Processing timestamp set when the batch job posts the record.
     */
    @Column(name = "proc_timestamp")
    private LocalDateTime procTimestamp;

    /**
     * Processing status set by the Spring Batch processor step.
     *
     * <ul>
     *   <li>{@code VALID}    – passed all validation checks; posted to account.</li>
     *   <li>{@code REJECTED_CARD}    – card number not found in XREF file.</li>
     *   <li>{@code REJECTED_ACCOUNT} – account not found for resolved card.</li>
     *   <li>{@code REJECTED_AMOUNT}  – amount is null, zero, or negative.</li>
     * </ul>
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING";
}
