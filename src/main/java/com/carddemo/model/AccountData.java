package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AccountData – maps to the ACCOUNT-RECORD copybook (CVACT01Y).
 *
 * COBOL FD:  ACCOUNT-FILE, RECORD KEY IS FD-ACCT-ID (PIC 9(11))
 * Fields derived from CVACT01Y copybook layout used in CBACT04C.
 */
@Entity
@Table(name = "account_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {

    /** ACCT-ID  PIC 9(11) — primary key */
    @Id
    @Column(name = "acct_id", nullable = false, length = 11)
    private Long acctId;

    /** ACCT-ACTIVE-STATUS  PIC X(01) */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /** ACCT-CURR-BAL  PIC S9(10)V99 COMP-3 */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /** ACCT-CREDIT-LIMIT  PIC S9(10)V99 COMP-3 */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /** ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99 COMP-3 */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /** ACCT-OPEN-DATE  PIC X(10) */
    @Column(name = "acct_open_date")
    private LocalDate acctOpenDate;

    /** ACCT-EXPIRATION-DATE  PIC X(10) */
    @Column(name = "acct_expiration_date")
    private LocalDate acctExpirationDate;

    /** ACCT-REISSUE-DATE  PIC X(10) */
    @Column(name = "acct_reissue_date")
    private LocalDate acctReissueDate;

    /** ACCT-CURR-CYC-CREDIT  PIC S9(10)V99 COMP-3 */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    /** ACCT-CURR-CYC-DEBIT  PIC S9(10)V99 COMP-3 */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    /** ACCT-GROUP-ID  PIC X(10) — used for disclosure group lookup */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
