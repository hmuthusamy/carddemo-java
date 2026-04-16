package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing an account record.
 * Mirrors the COBOL copybook CVACT01Y used in CBTRN02C.
 *
 * Balance fields updated by cbtrn02cJob (CBTRN02C.CBL paragraph 2800-UPDATE-ACCOUNT-REC):
 *   ADD DALYTRAN-AMT TO ACCT-CURR-BAL
 *   IF DALYTRAN-AMT >= 0 → ADD to ACCT-CURR-CYC-CREDIT
 *   ELSE                 → ADD to ACCT-CURR-CYC-DEBIT
 */
@Entity
@Table(name = "account_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {

    /** Account identifier (FD-ACCT-ID / XREF-ACCT-ID) – 11 digits */
    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    /** Active/Inactive flag */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /**
     * Current balance (ACCT-CURR-BAL).
     * Updated by: ADD DALYTRAN-AMT TO ACCT-CURR-BAL
     * COBOL PIC S9(10)V99 → BigDecimal(12,2).
     */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /**
     * Credit limit (ACCT-CREDIT-LIMIT).
     * Used to validate: ACCT-CREDIT-LIMIT >= (CURR-CREDIT - CURR-DEBIT + TRAN-AMT).
     */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /**
     * Cash credit limit (ACCT-CASH-CREDIT-LIMIT).
     */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /** Account open date */
    @Column(name = "acct_open_date")
    private LocalDate acctOpenDate;

    /**
     * Account expiration date (ACCT-EXPIRAION-DATE in COBOL – note intentional typo preserved).
     * Validation: ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS(1:10)
     */
    @Column(name = "acct_expiration_date")
    private LocalDate acctExpirationDate;

    /** Reissue date */
    @Column(name = "acct_reissue_date")
    private LocalDate acctReissueDate;

    /**
     * Current cycle credit total (ACCT-CURR-CYC-CREDIT).
     * Updated when DALYTRAN-AMT >= 0:  ADD DALYTRAN-AMT TO ACCT-CURR-CYC-CREDIT
     */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCurrCycCredit = BigDecimal.ZERO;

    /**
     * Current cycle debit total (ACCT-CURR-CYC-DEBIT).
     * Updated when DALYTRAN-AMT < 0:   ADD DALYTRAN-AMT TO ACCT-CURR-CYC-DEBIT
     * (DALYTRAN-AMT is negative for debits, so ADD still increases the absolute debit total.)
     */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal acctCurrCycDebit = BigDecimal.ZERO;

    /** Customer ID – FK to customer master */
    @Column(name = "acct_customer_id")
    private Long acctCustomerId;

    /** Group ID */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
