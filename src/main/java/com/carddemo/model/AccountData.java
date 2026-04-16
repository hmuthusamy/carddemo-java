package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity representing the ACCOUNT-RECORD structure from COBOL copybook CVACT01Y.
 *
 * COBOL mapping:
 *   ACCT-ID                 PIC 9(11)           → Long
 *   ACCT-ACTIVE-STATUS      PIC X(01)           → String
 *   ACCT-CURR-BAL           PIC S9(10)V99       → BigDecimal  (COMP-3 equivalent)
 *   ACCT-CREDIT-LIMIT       PIC S9(10)V99       → BigDecimal
 *   ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99       → BigDecimal
 *   ACCT-OPEN-DATE          PIC X(10)           → LocalDate
 *   ACCT-EXPIRAION-DATE     PIC X(10)           → LocalDate
 *   ACCT-REISSUE-DATE       PIC X(10)           → LocalDate
 *   ACCT-CURR-CYC-CREDIT    PIC S9(10)V99       → BigDecimal
 *   ACCT-CURR-CYC-DEBIT     PIC S9(10)V99       → BigDecimal
 *   ACCT-ADDR-ZIP           PIC X(10)           → String
 *   ACCT-GROUP-ID           PIC X(10)           → String
 */
@Entity
@Table(name = "account_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountData {

    /** ACCT-ID PIC 9(11) */
    @Id
    @Column(name = "acct_id", nullable = false, precision = 11)
    private Long acctId;

    /** ACCT-ACTIVE-STATUS PIC X(01) – 'Y' = active, 'N' = inactive */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /**
     * ACCT-CURR-BAL PIC S9(10)V99 — current balance.
     * Stored as COMP-3 (packed decimal) in COBOL; represented as BigDecimal(12,2) here.
     * Updated by the interest calculation batch (CBACT03C / CBACT04C).
     */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /** ACCT-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /** ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /** ACCT-OPEN-DATE PIC X(10) */
    @Column(name = "acct_open_date")
    private LocalDate acctOpenDate;

    /** ACCT-EXPIRAION-DATE PIC X(10) — note: original COBOL typo preserved */
    @Column(name = "acct_expiration_date")
    private LocalDate acctExpirationDate;

    /** ACCT-REISSUE-DATE PIC X(10) */
    @Column(name = "acct_reissue_date")
    private LocalDate acctReissueDate;

    /**
     * ACCT-CURR-CYC-CREDIT PIC S9(10)V99 — total credits in current cycle.
     * Reset to 0 after interest posting (1050-UPDATE-ACCOUNT in CBACT04C).
     */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    /**
     * ACCT-CURR-CYC-DEBIT PIC S9(10)V99 — total debits in current cycle.
     * Reset to 0 after interest posting.
     */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    /** ACCT-ADDR-ZIP PIC X(10) */
    @Column(name = "acct_addr_zip", length = 10)
    private String acctAddrZip;

    /**
     * ACCT-GROUP-ID PIC X(10) — used to look up interest rate in disclosure group table.
     * Key component: FD-DIS-ACCT-GROUP-ID in DISCGRP-FILE lookup.
     */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
