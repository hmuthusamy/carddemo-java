package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Input DTO representing one account update transaction record read by the
 * CBACT02C flat-file {@link org.springframework.batch.item.file.FlatFileItemReader}.
 *
 * <p>Each field maps to a fixed-column position in the input file,
 * mirroring the COBOL record layout defined in copybook <b>CVACT01Y</b>.
 *
 * <p>Fixed-width column layout (92 characters per record):
 * <pre>
 *  Cols  1-11 : acctId               (ACCT-ID               PIC 9(11))
 *  Col   12   : acctActiveStatus     (ACCT-ACTIVE-STATUS     PIC X(01))
 *  Cols 13-25 : acctCurrBal          (ACCT-CURR-BAL          PIC S9(10)V99 → 13 chars with sign+decimal)
 *  Cols 26-38 : acctCreditLimit      (ACCT-CREDIT-LIMIT      PIC S9(10)V99)
 *  Cols 39-51 : acctCashCreditLimit  (ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99)
 *  Cols 52-61 : acctOpenDate         (ACCT-OPEN-DATE         PIC X(10))
 *  Cols 62-71 : acctExpirationDate   (ACCT-EXPIRAION-DATE    PIC X(10))
 *  Cols 72-81 : acctReissueDate      (ACCT-REISSUE-DATE      PIC X(10))
 *  Cols 82-86 : acctAddrZip          (ACCT-ADDR-ZIP          PIC X(10), first 5)
 *  Cols 87-96 : acctGroupId          (ACCT-GROUP-ID          PIC X(10))
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {

    /** ACCT-ID PIC 9(11) – account identifier. */
    private Long acctId;

    /** ACCT-ACTIVE-STATUS PIC X(01) – 'Y' or 'N'. */
    private String acctActiveStatus;

    /** ACCT-CURR-BAL PIC S9(10)V99 COMP-3 – current balance as string for BigDecimal parsing. */
    private String acctCurrBal;

    /** ACCT-CREDIT-LIMIT PIC S9(10)V99 COMP-3 – credit limit as string. */
    private String acctCreditLimit;

    /** ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 COMP-3 – cash credit limit as string. */
    private String acctCashCreditLimit;

    /** ACCT-OPEN-DATE PIC X(10) – account open date. */
    private String acctOpenDate;

    /** ACCT-EXPIRAION-DATE PIC X(10) – account expiration date. */
    private String acctExpirationDate;

    /** ACCT-REISSUE-DATE PIC X(10) – card reissue date. */
    private String acctReissueDate;

    /** ACCT-ADDR-ZIP PIC X(10) – ZIP/postal code. */
    private String acctAddrZip;

    /** ACCT-GROUP-ID PIC X(10) – account group. */
    private String acctGroupId;
}
