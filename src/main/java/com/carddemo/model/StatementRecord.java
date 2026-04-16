package com.carddemo.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated statement record holding header data (from CBSTM03A step 1)
 * and line-item transactions (from CBSTM03B step 2).
 *
 * Corresponds to the in-memory WS-TRNX-TABLE accumulation in CBSTM03A.
 */
public class StatementRecord {

    // ── Header fields (CBSTM03A: 5000-CREATE-STATEMENT) ──────────────

    /** Card number (key for grouping) */
    private String cardNumber;

    /** Account ID – ST-ACCT-ID PIC X(20) */
    private String accountId;

    /** Customer full name – ST-NAME PIC X(75) */
    private String customerName;

    /** Address line 1 – ST-ADD1 PIC X(50) */
    private String addressLine1;

    /** Address line 2 – ST-ADD2 PIC X(50) */
    private String addressLine2;

    /** Address line 3 (city/state/country/zip) – ST-ADD3 PIC X(80) */
    private String addressLine3;

    /** Current balance – ST-CURR-BAL PIC 9(9).99-, scale 2 */
    private BigDecimal currentBalance;

    /** FICO score – ST-FICO-SCORE PIC X(20) */
    private String ficoScore;

    // ── Line-item transactions (CBSTM03A: 6000-WRITE-TRANS) ──────────

    /** Individual transaction line items */
    private List<TransactionData> transactions = new ArrayList<>();

    /**
     * Running subtotal – WS-TOTAL-AMT PIC S9(9)V99 COMP-3, scale 2.
     * Corresponds to ST-TOTAL-TRAMT and grand total logic in 4000-TRNXFILE-GET.
     */
    private BigDecimal totalTransactionAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

    public StatementRecord() {}

    public StatementRecord(String cardNumber, String accountId, String customerName) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.customerName = customerName;
    }

    // ── Subtotal helper (mirrors: ADD TRNX-AMT TO WS-TOTAL-AMT) ─────

    public void addTransaction(TransactionData txn) {
        transactions.add(txn);
        if (txn.getTransactionAmount() != null) {
            totalTransactionAmount = totalTransactionAmount
                .add(txn.getTransactionAmount())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getAddressLine3() { return addressLine3; }
    public void setAddressLine3(String addressLine3) { this.addressLine3 = addressLine3; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null
            ? currentBalance.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public String getFicoScore() { return ficoScore; }
    public void setFicoScore(String ficoScore) { this.ficoScore = ficoScore; }

    public List<TransactionData> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionData> transactions) {
        this.transactions = transactions;
    }

    public BigDecimal getTotalTransactionAmount() { return totalTransactionAmount; }
    public void setTotalTransactionAmount(BigDecimal totalTransactionAmount) {
        this.totalTransactionAmount = totalTransactionAmount != null
            ? totalTransactionAmount.setScale(2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "StatementRecord{accountId='" + accountId + "', card='" + cardNumber
            + "', txns=" + transactions.size() + ", total=" + totalTransactionAmount + '}';
    }
}
