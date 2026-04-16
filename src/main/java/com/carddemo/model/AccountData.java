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

/**
 * AccountData – JPA entity mapped from COBOL copybook CVACT01Y.
 *
 * COBOL layout (CVACT01Y):
 *   01 ACCOUNT-RECORD.
 *      05 ACCT-ID                        PIC 9(11).
 *      05 ACCT-ACTIVE-STATUS             PIC X(01).
 *      05 ACCT-CURR-BAL                  PIC S9(10)V99 COMP-3.
 *      05 ACCT-CREDIT-LIMIT              PIC S9(10)V99 COMP-3.
 *      05 ACCT-CASH-CREDIT-LIMIT         PIC S9(10)V99 COMP-3.
 *      05 ACCT-OPEN-DATE                 PIC X(10).
 *      05 ACCT-EXPIRAION-DATE            PIC X(10).
 *      05 ACCT-REISSUE-DATE              PIC X(10).
 *      05 ACCT-CURR-CYC-CREDIT           PIC S9(10)V99 COMP-3.
 *      05 ACCT-CURR-CYC-DEBIT            PIC S9(10)V99 COMP-3.
 *      05 ACCT-GROUP-ID                  PIC X(10).
 */
@Entity
@Table(name = "account_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {

    /** Maps to ACCT-ID PIC 9(11) – primary key */
    @Id
    @Column(name = "acct_id", nullable = false, length = 11)
    private Long acctId;

    /** Maps to ACCT-ACTIVE-STATUS PIC X(01) */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /** Maps to ACCT-CURR-BAL PIC S9(10)V99 */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /** Maps to ACCT-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /** Maps to ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /** Maps to ACCT-OPEN-DATE PIC X(10) */
    @Column(name = "acct_open_date", length = 10)
    private String acctOpenDate;

    /** Maps to ACCT-EXPIRAION-DATE PIC X(10) – note original COBOL typo preserved */
    @Column(name = "acct_expiraion_date", length = 10)
    private String acctExpiraionDate;

    /** Maps to ACCT-REISSUE-DATE PIC X(10) */
    @Column(name = "acct_reissue_date", length = 10)
    private String acctReissueDate;

    /** Maps to ACCT-CURR-CYC-CREDIT PIC S9(10)V99 */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    /** Maps to ACCT-CURR-CYC-DEBIT PIC S9(10)V99 */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    /** Maps to ACCT-GROUP-ID PIC X(10) */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
