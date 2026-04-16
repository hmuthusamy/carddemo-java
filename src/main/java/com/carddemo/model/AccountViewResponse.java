package com.carddemo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AccountViewResponse – REST DTO that replaces the COACTVWC BMS screen output map (CACTVWAO).
 *
 * Field names mirror the BMS symbolic fields from copybook COACTVW so that the
 * mapping logic in AccountViewService is easy to trace back to COBOL.
 *
 * COBOL → Java field correspondence (CACTVWAO → AccountViewResponse):
 *   ACCTSIDO  → accountId
 *   ACSTTUSO  → activeStatus
 *   ACURBALO  → currentBalance
 *   ACRDLIMO  → creditLimit
 *   ACSHLIMO  → cashCreditLimit
 *   ACRCYCRO  → currCycCredit
 *   ACRCYDBO  → currCycDebit
 *   ADTOPENO  → openDate
 *   AEXPDTO   → expirationDate
 *   AREISDTO  → reissueDate
 *   AADDGRPO  → groupId
 *   ACSTNUMO  → customerId
 *   ACSTSSNO  → ssn             (formatted NNN-NN-NNNN as in COBOL STRING statement)
 *   ACSTFCOO  → ficoCreditScore
 *   ACSTDOBO  → dateOfBirth
 *   ACSFNAMO  → firstName
 *   ACSMNAMO  → middleName
 *   ACSLNAMO  → lastName
 *   ACSADL1O  → addressLine1
 *   ACSADL2O  → addressLine2
 *   ACSCITYO  → city
 *   ACSSTTEO  → state
 *   ACSZIPCO  → zipCode
 *   ACSCTRYO  → country
 *   ACSPHN1O  → phoneNumber1
 *   ACSPHN2O  → phoneNumber2
 *   ACSGOVTO  → govtIssuedId
 *   ACSEFTCO  → eftAccountId
 *   ACSPFLGO  → primaryCardHolderFlag
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountViewResponse {

    // ── Account fields ──────────────────────────────────────────────────────
    private Long          accountId;
    private String        activeStatus;
    private BigDecimal    currentBalance;
    private BigDecimal    creditLimit;
    private BigDecimal    cashCreditLimit;
    private BigDecimal    currCycCredit;
    private BigDecimal    currCycDebit;
    private String        openDate;
    private String        expirationDate;
    private String        reissueDate;
    private String        groupId;

    // ── Customer fields ──────────────────────────────────────────────────────
    private Long          customerId;
    /** SSN formatted as NNN-NN-NNNN (COBOL STRING logic preserved) */
    private String        ssn;
    private Integer       ficoCreditScore;
    private String        dateOfBirth;
    private String        firstName;
    private String        middleName;
    private String        lastName;
    private String        addressLine1;
    private String        addressLine2;
    private String        city;
    private String        state;
    private String        zipCode;
    private String        country;
    private String        phoneNumber1;
    private String        phoneNumber2;
    private String        govtIssuedId;
    private String        eftAccountId;
    private String        primaryCardHolderFlag;
}
