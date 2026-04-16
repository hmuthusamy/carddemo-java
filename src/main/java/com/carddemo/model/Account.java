package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA entity for the ACCOUNTS table.
 *
 * <p>Migrated from COBOL copybook CVACT01Y.cpy (ACCOUNT-RECORD, RECLN=300).
 *
 * <pre>
 * Column type mapping:
 *   account_id          BIGINT           ← PIC 9(11)
 *   active_status       CHAR(1)          ← PIC X(01)
 *   current_balance     NUMERIC(12,2)    ← PIC S9(10)V99
 *   credit_limit        NUMERIC(12,2)    ← PIC S9(10)V99
 *   cash_credit_limit   NUMERIC(12,2)    ← PIC S9(10)V99
 *   open_date           VARCHAR(10)      ← PIC X(10)
 *   expiration_date     VARCHAR(10)      ← PIC X(10)
 *   reissue_date        VARCHAR(10)      ← PIC X(10)
 *   curr_cycle_credit   NUMERIC(12,2)    ← PIC S9(10)V99
 *   curr_cycle_debit    NUMERIC(12,2)    ← PIC S9(10)V99
 *   addr_zip            VARCHAR(10)      ← PIC X(10)
 *   group_id            VARCHAR(10)      ← PIC X(10)
 * </pre>
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /** ACCT-ID PIC 9(11) – primary key. Maps to DDL: BIGINT NOT NULL */
    @Id
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** ACCT-ACTIVE-STATUS PIC X(01). Maps to DDL: CHAR(1) NOT NULL DEFAULT 'Y' */
    @Column(name = "active_status", nullable = false, length = 1)
    private String activeStatus;

    /** ACCT-CURR-BAL PIC S9(10)V99. Maps to DDL: NUMERIC(12,2) NOT NULL */
    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentBalance;

    /** ACCT-CREDIT-LIMIT PIC S9(10)V99. Maps to DDL: NUMERIC(12,2) NOT NULL */
    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit;

    /** ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99. Maps to DDL: NUMERIC(12,2) NOT NULL */
    @Column(name = "cash_credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    /** ACCT-OPEN-DATE PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "open_date", nullable = false, length = 10)
    private String openDate;

    /** ACCT-EXPIRAION-DATE PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "expiration_date", nullable = false, length = 10)
    private String expirationDate;

    /** ACCT-REISSUE-DATE PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "reissue_date", nullable = false, length = 10)
    private String reissueDate;

    /** ACCT-CURR-CYC-CREDIT PIC S9(10)V99. Maps to DDL: NUMERIC(12,2) NOT NULL */
    @Column(name = "curr_cycle_credit", nullable = false, precision = 12, scale = 2)
    private BigDecimal currCycleCredit;

    /** ACCT-CURR-CYC-DEBIT PIC S9(10)V99. Maps to DDL: NUMERIC(12,2) NOT NULL */
    @Column(name = "curr_cycle_debit", nullable = false, precision = 12, scale = 2)
    private BigDecimal currCycleDebit;

    /** ACCT-ADDR-ZIP PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "addr_zip", nullable = false, length = 10)
    private String addrZip;

    /** ACCT-GROUP-ID PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "group_id", nullable = false, length = 10)
    private String groupId;
}
