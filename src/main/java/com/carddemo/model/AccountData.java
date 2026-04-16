package com.carddemo.model;

import java.math.BigDecimal;

/**
 * Maps to FD-ACCTFILE-REC / CVACT01Y copybook.
 *
 * Key field: ACCT-ID  PIC 9(11)
 * Notable fields:
 *   ACCT-CURR-BAL    PIC S9(9)V99 COMP-3
 *   ACCT-CREDIT-LIMIT PIC S9(9)V99 COMP-3
 *   ACCT-CURR-CYC-CREDIT PIC S9(9)V99 COMP-3
 *   ACCT-CURR-CYC-DEBIT  PIC S9(9)V99 COMP-3
 */
public class AccountData {

    /** ACCT-ID PIC 9(11) */
    private String accountId;

    /** ACCT-CURR-BAL PIC S9(9)V99 COMP-3 – scale 2 */
    private BigDecimal currentBalance;

    /** ACCT-CREDIT-LIMIT PIC S9(9)V99 COMP-3 – scale 2 */
    private BigDecimal creditLimit;

    /** ACCT-CURR-CYC-CREDIT PIC S9(9)V99 COMP-3 – scale 2 */
    private BigDecimal currentCycleCredit;

    /** ACCT-CURR-CYC-DEBIT PIC S9(9)V99 COMP-3 – scale 2 */
    private BigDecimal currentCycleDebit;

    /** ACCT-OPEN-DATE PIC X(10) */
    private String openDate;

    /** ACCT-EXPIRATION-DATE PIC X(10) */
    private String expirationDate;

    /** ACCT-REISSUE-DATE PIC X(10) */
    private String reissueDate;

    public AccountData() {}

    public AccountData(String accountId, BigDecimal currentBalance, BigDecimal creditLimit) {
        this.accountId = accountId;
        this.currentBalance = currentBalance != null
            ? currentBalance.setScale(2, java.math.RoundingMode.HALF_UP) : null;
        this.creditLimit = creditLimit != null
            ? creditLimit.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = (currentBalance != null)
            ? currentBalance.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = (creditLimit != null)
            ? creditLimit.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getCurrentCycleCredit() { return currentCycleCredit; }
    public void setCurrentCycleCredit(BigDecimal currentCycleCredit) {
        this.currentCycleCredit = (currentCycleCredit != null)
            ? currentCycleCredit.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getCurrentCycleDebit() { return currentCycleDebit; }
    public void setCurrentCycleDebit(BigDecimal currentCycleDebit) {
        this.currentCycleDebit = (currentCycleDebit != null)
            ? currentCycleDebit.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public String getOpenDate() { return openDate; }
    public void setOpenDate(String openDate) { this.openDate = openDate; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public String getReissueDate() { return reissueDate; }
    public void setReissueDate(String reissueDate) { this.reissueDate = reissueDate; }

    @Override
    public String toString() {
        return "AccountData{accountId='" + accountId + "', balance=" + currentBalance + '}';
    }
}
