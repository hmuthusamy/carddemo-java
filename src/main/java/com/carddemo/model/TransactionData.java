package com.carddemo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TransactionData – Java model migrated from COBOL copybooks CVTRA05Y / CVTRA07Y.
 *
 * <p>Maps to the transaction record read by CBTRN03C.CBL:
 * <ul>
 *   <li>FD-TRANS-DATA  PIC X(304)  – core transaction fields</li>
 *   <li>FD-TRAN-PROC-TS PIC X(26)  – processing timestamp</li>
 * </ul>
 *
 * Rejection processing adds:
 * <ul>
 *   <li>{@link #rejectionReasonCode} – rejection reason (populated by Cbtrn03cService)</li>
 *   <li>{@link #rejected}             – flag set when transaction is rejected</li>
 * </ul>
 */
public class TransactionData {

    // -----------------------------------------------------------------------
    // Core fields from CVTRA05Y copybook
    // -----------------------------------------------------------------------

    /** Unique transaction identifier (TRAN-ID). */
    private String transactionId;

    /** Card number associated with the transaction (TRAN-CARD-NUM). */
    private String cardNumber;

    /** Account ID resolved via XREF lookup (XREF-ACCT-ID). */
    private String accountId;

    /** Transaction type code, e.g. "PR", "DB" (TRAN-TYPE-CD). */
    private String transactionTypeCode;

    /** Human-readable description of the transaction type (TRAN-TYPE-DESC). */
    private String transactionTypeDescription;

    /** Category code associated with the transaction type (TRAN-CAT-CD). */
    private Integer transactionCategoryCode;

    /** Human-readable category description (TRAN-CAT-TYPE-DESC). */
    private String transactionCategoryDescription;

    /** Originating source of the transaction (TRAN-SOURCE). */
    private String transactionSource;

    /** Transaction amount (TRAN-AMT). */
    private BigDecimal transactionAmount;

    /** Original transaction amount before adjustments. */
    private BigDecimal originalAmount;

    /** Merchant name (if present). */
    private String merchantName;

    /** Merchant city. */
    private String merchantCity;

    /** Merchant country. */
    private String merchantCountry;

    /** Processing timestamp (FD-TRAN-PROC-TS). */
    private LocalDateTime processingTimestamp;

    /** Date portion of the transaction (derived from processingTimestamp or input). */
    private LocalDate transactionDate;

    // -----------------------------------------------------------------------
    // Rejection-specific fields  (populated by Cbtrn03cService)
    // -----------------------------------------------------------------------

    /** Whether this transaction has been flagged as rejected. */
    private boolean rejected;

    /**
     * Rejection reason code set during CBTRN03C processing.
     * <ul>
     *   <li>R001 – Invalid card number (XREF lookup failure)</li>
     *   <li>R002 – Unknown transaction type</li>
     *   <li>R003 – Unknown transaction category</li>
     *   <li>R004 – Transaction outside date range</li>
     *   <li>R005 – Zero or negative amount</li>
     *   <li>R999 – General / unclassified rejection</li>
     * </ul>
     */
    private String rejectionReasonCode;

    /** Free-text description of the rejection reason. */
    private String rejectionReasonDescription;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public TransactionData() {
    }

    // -----------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }

    public String getTransactionTypeDescription() {
        return transactionTypeDescription;
    }

    public void setTransactionTypeDescription(String transactionTypeDescription) {
        this.transactionTypeDescription = transactionTypeDescription;
    }

    public Integer getTransactionCategoryCode() {
        return transactionCategoryCode;
    }

    public void setTransactionCategoryCode(Integer transactionCategoryCode) {
        this.transactionCategoryCode = transactionCategoryCode;
    }

    public String getTransactionCategoryDescription() {
        return transactionCategoryDescription;
    }

    public void setTransactionCategoryDescription(String transactionCategoryDescription) {
        this.transactionCategoryDescription = transactionCategoryDescription;
    }

    public String getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    public String getMerchantCountry() {
        return merchantCountry;
    }

    public void setMerchantCountry(String merchantCountry) {
        this.merchantCountry = merchantCountry;
    }

    public LocalDateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    public void setProcessingTimestamp(LocalDateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public String getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public void setRejectionReasonCode(String rejectionReasonCode) {
        this.rejectionReasonCode = rejectionReasonCode;
    }

    public String getRejectionReasonDescription() {
        return rejectionReasonDescription;
    }

    public void setRejectionReasonDescription(String rejectionReasonDescription) {
        this.rejectionReasonDescription = rejectionReasonDescription;
    }

    @Override
    public String toString() {
        return "TransactionData{"
                + "transactionId='" + transactionId + '\''
                + ", cardNumber='" + cardNumber + '\''
                + ", accountId='" + accountId + '\''
                + ", transactionTypeCode='" + transactionTypeCode + '\''
                + ", transactionCategoryCode=" + transactionCategoryCode
                + ", transactionAmount=" + transactionAmount
                + ", rejected=" + rejected
                + ", rejectionReasonCode='" + rejectionReasonCode + '\''
                + '}';
    }
}
