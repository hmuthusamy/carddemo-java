package com.carddemo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AccountData – JPA entity migrated from COBOL copybook ACCTDATA.CPY / COACTUPD.cbl.
 *
 * COBOL original layout (ACCOUNT-RECORD):
 *   ACCT-ID               PIC 9(11)
 *   ACCT-ACTIVE-STATUS    PIC X(1)
 *   ACCT-CURR-BAL         PIC S9(10)V99
 *   ACCT-CREDIT-LIMIT     PIC S9(10)V99
 *   ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
 *   ACCT-OPEN-DATE        PIC X(10)
 *   ACCT-EXPIRAION-DATE   PIC X(10)
 *   ACCT-REISSUE-DATE     PIC X(10)
 *   ACCT-CURR-CYC-CREDIT  PIC S9(10)V99
 *   ACCT-CURR-CYC-DEBIT   PIC S9(10)V99
 *   ACCT-ADDR-ZIP         PIC X(10)
 *   ACCT-GROUP-ID         PIC X(10)
 */
@Entity
@Table(name = "account_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountData {

    /** PIC 9(11) – Account ID (primary key). */
    @Id
    @Column(name = "acct_id", length = 11, nullable = false)
    private String accountId;

    /** PIC X(1) – 'Y' = Active, 'N' = Inactive. */
    @Column(name = "acct_active_status", length = 1)
    private String activeStatus;

    /** PIC S9(10)V99 – Current balance. */
    @Column(name = "acct_curr_bal", precision = 12, scale = 2)
    private BigDecimal currentBalance;

    /** PIC S9(10)V99 – Credit limit. */
    @Column(name = "acct_credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /** PIC S9(10)V99 – Cash credit limit. */
    @Column(name = "acct_cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    /** PIC X(10) – Open date (YYYY-MM-DD). */
    @Column(name = "acct_open_date")
    private LocalDate openDate;

    /** PIC X(10) – Expiration date (YYYY-MM-DD). */
    @Column(name = "acct_expiration_date")
    private LocalDate expirationDate;

    /** PIC X(10) – Reissue date (YYYY-MM-DD). */
    @Column(name = "acct_reissue_date")
    private LocalDate reissueDate;

    /** PIC S9(10)V99 – Current cycle credits. */
    @Column(name = "acct_curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal currCycCredit;

    /** PIC S9(10)V99 – Current cycle debits. */
    @Column(name = "acct_curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal currCycDebit;

    /** PIC X(10) – ZIP code (address). */
    @Column(name = "acct_addr_zip", length = 10)
    private String addrZip;

    /** PIC X(10) – Group ID. */
    @Column(name = "acct_group_id", length = 10)
    private String groupId;
}
