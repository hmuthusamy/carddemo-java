package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CVACT01Y.cpy
 * Data-structure for account entity (RECLN 300)
 * VSAM KSDS - keyed by ACCT-ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account")
public class AccountData {

    /** ACCT-ID PIC 9(11) - Primary key */
    @Id
    @Column(name = "acct_id", precision = 11, nullable = false)
    private Long acctId;

    /** ACCT-ACTIVE-STATUS PIC X(01) */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /** ACCT-CURR-BAL PIC S9(10)V99 - signed 10-digit integer + 2 decimal places */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /** ACCT-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /** ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /** ACCT-OPEN-DATE PIC X(10) */
    @Column(name = "acct_open_date", length = 10)
    private String acctOpenDate;

    /** ACCT-EXPIRAION-DATE PIC X(10) */
    @Column(name = "acct_expiration_date", length = 10)
    private String acctExpirationDate;

    /** ACCT-REISSUE-DATE PIC X(10) */
    @Column(name = "acct_reissue_date", length = 10)
    private String acctReissueDate;

    /** ACCT-CURR-CYC-CREDIT PIC S9(10)V99 */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    /** ACCT-CURR-CYC-DEBIT PIC S9(10)V99 */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    /** ACCT-ADDR-ZIP PIC X(10) */
    @Column(name = "acct_addr_zip", length = 10)
    private String acctAddrZip;

    /** ACCT-GROUP-ID PIC X(10) */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
