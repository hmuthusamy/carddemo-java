package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing a credit-card account record.
 *
 * <p>Migrated from COBOL copybook CVACT01Y (ACCOUNT-FILE in CBTRN01C).
 * CBTRN01C reads this file to confirm the account exists before posting
 * a transaction against it.
 *
 * <p>Layout in COBOL:
 * <pre>
 *   FD-ACCT-ID     PIC 9(11)   — record key
 *   FD-ACCT-DATA   PIC X(289)  — balance, status, limit, dates, etc.
 * </pre>
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /** 11-digit account ID (FD-ACCT-ID / ACCT-ID). */
    @Id
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** Account status – e.g. "1" active, "0" closed. */
    @Column(name = "active_status", length = 1)
    private String activeStatus;

    /**
     * Current balance on the account (ACCT-BALANCE).
     * COBOL PIC S9(10)V99 COMP-3 → BigDecimal(12,2).
     */
    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance;

    /**
     * Credit limit (ACCT-CREDIT-LIMIT).
     * COBOL PIC S9(10)V99 COMP-3 → BigDecimal(12,2).
     */
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /**
     * Cash-advance credit limit (ACCT-CASH-CREDIT-LIMIT).
     */
    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    /** Account open date (ACCT-OPEN-DATE). */
    @Column(name = "open_date")
    private LocalDate openDate;

    /** Account expiry date (ACCT-EXPIRAION-DATE). */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Linked customer ID (ACCT-CUSTOMER-ID). */
    @Column(name = "customer_id")
    private Long customerId;
}
