package com.carddemo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AccountUpdateRequest – REST request body for PUT /api/accounts/{accountId}.
 *
 * Maps to the BMS screen fields of the COACTUPD CICS program:
 *   ACUPDAIO (BMS map) → each field corresponds to an updatable account attribute.
 *
 * All fields are optional in the DTO; the service layer applies
 * COBOL-equivalent validations (numeric checks, length limits, required fields).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountUpdateRequest {

    /**
     * ACCT-ACTIVE-STATUS  PIC X(1)  – 'Y' (Active) or 'N' (Inactive).
     * Corresponds to CACTUPDI screen field ACSTTUSI.
     */
    private String activeStatus;

    /**
     * ACCT-CREDIT-LIMIT  PIC S9(10)V99
     * Corresponds to screen field ACRDLIMI.
     */
    private BigDecimal creditLimit;

    /**
     * ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99
     * Corresponds to screen field ACSHCRLI.
     */
    private BigDecimal cashCreditLimit;

    /**
     * ACCT-EXPIRAION-DATE  PIC X(10)  (YYYY-MM-DD)
     * Corresponds to screen field AEXPDTI.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    /**
     * ACCT-REISSUE-DATE  PIC X(10)  (YYYY-MM-DD)
     * Corresponds to screen field AREISSDTI.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reissueDate;

    /**
     * ACCT-ADDR-ZIP  PIC X(10)
     * Corresponds to screen field AZIPCDEI.
     */
    private String addrZip;

    /**
     * ACCT-GROUP-ID  PIC X(10)
     * Corresponds to screen field AGRPIDI.
     */
    private String groupId;

    /**
     * ACCT-CURR-CYC-CREDIT  PIC S9(10)V99
     * Corresponds to screen field ACURCRCI.
     */
    private BigDecimal currCycCredit;

    /**
     * ACCT-CURR-CYC-DEBIT  PIC S9(10)V99
     * Corresponds to screen field ACURDBTI.
     */
    private BigDecimal currCycDebit;
}
