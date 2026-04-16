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
 * JPA entity representing an account record from the VSAM account dataset.
 *
 * <p>Derived from COBOL copybook <b>CVACT01Y</b> (record length 300):
 * <pre>
 *  01  ACCOUNT-RECORD.
 *      05  ACCT-ID                    PIC 9(11)        – primary key
 *      05  ACCT-ACTIVE-STATUS         PIC X(01)        – 'Y'|'N'
 *      05  ACCT-CURR-BAL              PIC S9(10)V99    – COMP-3 → BigDecimal
 *      05  ACCT-CREDIT-LIMIT          PIC S9(10)V99    – COMP-3 → BigDecimal
 *      05  ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99    – COMP-3 → BigDecimal
 *      05  ACCT-OPEN-DATE             PIC X(10)
 *      05  ACCT-EXPIRAION-DATE        PIC X(10)        – COBOL spelling preserved
 *      05  ACCT-REISSUE-DATE          PIC X(10)
 *      05  ACCT-CURR-CYC-CREDIT       PIC S9(10)V99    – COMP-3 → BigDecimal
 *      05  ACCT-CURR-CYC-DEBIT        PIC S9(10)V99    – COMP-3 → BigDecimal
 *      05  ACCT-ADDR-ZIP              PIC X(10)
 *      05  ACCT-GROUP-ID              PIC X(10)
 *      05  FILLER                     PIC X(178)
 * </pre>
 *
 * <p><b>COMP-3 Mapping:</b> All packed-decimal numeric fields (PIC S9(n)V99) are
 * represented as {@link BigDecimal} with scale 2, using
 * {@link java.math.RoundingMode#HALF_UP} – the standard COBOL rounding mode.
 */
@Entity
@Table(name = "account_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {

    /**
     * ACCT-ID PIC 9(11) – account identifier, primary key.
     */
    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    /**
     * ACCT-ACTIVE-STATUS PIC X(01) – 'Y' = active, 'N' = inactive.
     */
    @Column(name = "acct_active_status", length = 1)
    private String acctActiveStatus;

    /**
     * ACCT-CURR-BAL PIC S9(10)V99 COMP-3 – current balance.
     * Stored as DECIMAL(12,2) to match COBOL 10-digit integer + 2 decimal places.
     */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal acctCurrBal;

    /**
     * ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3 – total credit limit.
     */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCreditLimit;

    /**
     * ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3 – cash advance credit limit.
     */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal acctCashCreditLimit;

    /**
     * ACCT-OPEN-DATE PIC X(10) – account opening date (YYYY-MM-DD).
     */
    @Column(name = "acct_open_date", length = 10)
    private String acctOpenDate;

    /**
     * ACCT-EXPIRAION-DATE PIC X(10) – account expiration date (COBOL spelling preserved).
     */
    @Column(name = "acct_expiration_date", length = 10)
    private String acctExpirationDate;

    /**
     * ACCT-REISSUE-DATE PIC X(10) – card reissue date (YYYY-MM-DD).
     */
    @Column(name = "acct_reissue_date", length = 10)
    private String acctReissueDate;

    /**
     * ACCT-CURR-CYC-CREDIT PIC S9(10)V99 COMP-3 – current cycle credit total.
     */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycCredit;

    /**
     * ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3 – current cycle debit total.
     */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal acctCurrCycDebit;

    /**
     * ACCT-ADDR-ZIP PIC X(10) – postal/ZIP code.
     */
    @Column(name = "acct_addr_zip", length = 10)
    private String acctAddrZip;

    /**
     * ACCT-GROUP-ID PIC X(10) – account group identifier.
     */
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;
}
