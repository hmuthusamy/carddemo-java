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
 * AccountData entity – direct Java mapping of the COBOL CVACT01Y copybook
 * (ACCOUNT-RECORD, RECLN 300).
 *
 * COBOL field mapping:
 *   ACCT-ID                PIC 9(11)       -> Long  acctId
 *   ACCT-ACTIVE-STATUS     PIC X(01)       -> String activeStatus
 *   ACCT-CURR-BAL          PIC S9(10)V99   -> BigDecimal currBal
 *   ACCT-CREDIT-LIMIT      PIC S9(10)V99   -> BigDecimal creditLimit
 *   ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99   -> BigDecimal cashCreditLimit
 *   ACCT-OPEN-DATE         PIC X(10)       -> String openDate
 *   ACCT-EXPIRAION-DATE    PIC X(10)       -> String expirationDate   (sic – COBOL typo preserved)
 *   ACCT-REISSUE-DATE      PIC X(10)       -> String reissueDate
 *   ACCT-CURR-CYC-CREDIT   PIC S9(10)V99   -> BigDecimal currCycCredit
 *   ACCT-CURR-CYC-DEBIT    PIC S9(10)V99   -> BigDecimal currCycDebit  (COMP-3 in output record)
 *   ACCT-ADDR-ZIP          PIC X(10)       -> String addrZip
 *   ACCT-GROUP-ID          PIC X(10)       -> String groupId
 */
@Entity
@Table(name = "account_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountData {

    /** ACCT-ID PIC 9(11) – primary key */
    @Id
    @Column(name = "acct_id", nullable = false, precision = 11, scale = 0)
    private Long acctId;

    /** ACCT-ACTIVE-STATUS PIC X(01) – 'Y' active, 'N' inactive */
    @Column(name = "acct_active_status", length = 1)
    private String activeStatus;

    /** ACCT-CURR-BAL PIC S9(10)V99 – current balance (COMP-3 semantics → BigDecimal) */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal currBal;

    /** ACCT-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /** ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    /** ACCT-OPEN-DATE PIC X(10) – formatted date string (e.g. YYYY-MM-DD) */
    @Column(name = "acct_open_date", length = 10)
    private String openDate;

    /** ACCT-EXPIRAION-DATE PIC X(10) – intentional typo from COBOL source preserved */
    @Column(name = "acct_expiraion_date", length = 10)
    private String expirationDate;

    /** ACCT-REISSUE-DATE PIC X(10) */
    @Column(name = "acct_reissue_date", length = 10)
    private String reissueDate;

    /** ACCT-CURR-CYC-CREDIT PIC S9(10)V99 */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal currCycCredit;

    /**
     * ACCT-CURR-CYC-DEBIT PIC S9(10)V99 USAGE IS COMP-3.
     * Stored as BigDecimal; COMP-3 (packed-decimal) arithmetic is preserved
     * via BigDecimal.ROUND_HALF_UP in the service layer.
     */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal currCycDebit;

    /** ACCT-ADDR-ZIP PIC X(10) */
    @Column(name = "acct_addr_zip", length = 10)
    private String addrZip;

    /** ACCT-GROUP-ID PIC X(10) */
    @Column(name = "acct_group_id", length = 10)
    private String groupId;
}
