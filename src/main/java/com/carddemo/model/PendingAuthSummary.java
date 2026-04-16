package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Java model for COBOL copybook CIPAUSMY.cpy
 * IMS Segment - Pending Authorization Summary.
 * VSAM / IMS KSDS equivalent - keyed by PA-ACCT-ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pending_auth_summary")
public class PendingAuthSummary {

    /** PA-ACCT-ID PIC S9(11) COMP-3 - Primary key */
    @Id
    @Column(name = "pa_acct_id", precision = 11, nullable = false)
    private BigDecimal paAcctId;

    /** PA-CUST-ID PIC 9(09) */
    @Column(name = "pa_cust_id", precision = 9)
    private Long paCustId;

    /** PA-AUTH-STATUS PIC X(01) */
    @Column(name = "pa_auth_status", length = 1)
    private String paAuthStatus;

    /** PA-ACCOUNT-STATUS OCCURS 5 TIMES PIC X(02) */
    @Column(name = "pa_account_status_1", length = 2)
    private String paAccountStatus1;

    @Column(name = "pa_account_status_2", length = 2)
    private String paAccountStatus2;

    @Column(name = "pa_account_status_3", length = 2)
    private String paAccountStatus3;

    @Column(name = "pa_account_status_4", length = 2)
    private String paAccountStatus4;

    @Column(name = "pa_account_status_5", length = 2)
    private String paAccountStatus5;

    /** PA-CREDIT-LIMIT PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_credit_limit", precision = 11, scale = 2)
    private BigDecimal paCreditLimit;

    /** PA-CASH-LIMIT PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_cash_limit", precision = 11, scale = 2)
    private BigDecimal paCashLimit;

    /** PA-CREDIT-BALANCE PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_credit_balance", precision = 11, scale = 2)
    private BigDecimal paCreditBalance;

    /** PA-CASH-BALANCE PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_cash_balance", precision = 11, scale = 2)
    private BigDecimal paCashBalance;

    /** PA-APPROVED-AUTH-CNT PIC S9(04) COMP */
    @Column(name = "pa_approved_auth_cnt", precision = 4)
    private Integer paApprovedAuthCnt;

    /** PA-DECLINED-AUTH-CNT PIC S9(04) COMP */
    @Column(name = "pa_declined_auth_cnt", precision = 4)
    private Integer paDeclinedAuthCnt;

    /** PA-APPROVED-AUTH-AMT PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_approved_auth_amt", precision = 11, scale = 2)
    private BigDecimal paApprovedAuthAmt;

    /** PA-DECLINED-AUTH-AMT PIC S9(09)V99 COMP-3 */
    @Column(name = "pa_declined_auth_amt", precision = 11, scale = 2)
    private BigDecimal paDeclinedAuthAmt;
}
