package com.carddemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CustomerData entity - mapped from COBOL copybook CVCUS01Y.cpy
 *
 * COBOL CUSTOMER-RECORD (500 bytes):
 *   CUST-ID                 PIC 9(09)
 *   CUST-FIRST-NAME         PIC X(25)
 *   CUST-MIDDLE-NAME        PIC X(25)
 *   CUST-LAST-NAME          PIC X(25)
 *   CUST-ADDR-LINE-1        PIC X(50)
 *   CUST-ADDR-LINE-2        PIC X(50)
 *   CUST-ADDR-LINE-3        PIC X(50)
 *   CUST-ADDR-STATE-CD      PIC X(02)
 *   CUST-ADDR-COUNTRY-CD    PIC X(03)
 *   CUST-ADDR-ZIP           PIC X(10)
 *   CUST-PHONE-NUM-1        PIC X(15)
 *   CUST-PHONE-NUM-2        PIC X(15)
 *   CUST-SSN                PIC 9(09)
 *   CUST-GOVT-ISSUED-ID     PIC X(20)
 *   CUST-DOB-YYYY-MM-DD     PIC X(10)
 *   CUST-EFT-ACCOUNT-ID     PIC X(10)
 *   CUST-PRI-CARD-HOLDER-IND PIC X(01)
 *   CUST-FICO-CREDIT-SCORE  PIC 9(03)
 *
 * Migrated from COUCUS00 CICS customer management program.
 */
@Entity
@Table(name = "customer_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerData {

    /** CUST-ID PIC 9(09) – primary key */
    @Id
    @Column(name = "cust_id", nullable = false)
    private Long customerId;

    /** CUST-FIRST-NAME PIC X(25) */
    @Column(name = "cust_first_name", length = 25)
    private String firstName;

    /** CUST-MIDDLE-NAME PIC X(25) */
    @Column(name = "cust_middle_name", length = 25)
    private String middleName;

    /** CUST-LAST-NAME PIC X(25) */
    @Column(name = "cust_last_name", length = 25)
    private String lastName;

    /** CUST-ADDR-LINE-1 PIC X(50) */
    @Column(name = "cust_addr_line_1", length = 50)
    private String addrLine1;

    /** CUST-ADDR-LINE-2 PIC X(50) */
    @Column(name = "cust_addr_line_2", length = 50)
    private String addrLine2;

    /** CUST-ADDR-LINE-3 PIC X(50) */
    @Column(name = "cust_addr_line_3", length = 50)
    private String addrLine3;

    /** CUST-ADDR-STATE-CD PIC X(02) */
    @Column(name = "cust_addr_state_cd", length = 2)
    private String addrStateCd;

    /** CUST-ADDR-COUNTRY-CD PIC X(03) */
    @Column(name = "cust_addr_country_cd", length = 3)
    private String addrCountryCd;

    /** CUST-ADDR-ZIP PIC X(10) */
    @Column(name = "cust_addr_zip", length = 10)
    private String addrZip;

    /** CUST-PHONE-NUM-1 PIC X(15) */
    @Column(name = "cust_phone_num_1", length = 15)
    private String phoneNum1;

    /** CUST-PHONE-NUM-2 PIC X(15) */
    @Column(name = "cust_phone_num_2", length = 15)
    private String phoneNum2;

    /** CUST-SSN PIC 9(09) */
    @Column(name = "cust_ssn")
    private Long ssn;

    /** CUST-GOVT-ISSUED-ID PIC X(20) */
    @Column(name = "cust_govt_issued_id", length = 20)
    private String govtIssuedId;

    /** CUST-DOB-YYYY-MM-DD PIC X(10) */
    @Column(name = "cust_dob_yyyy_mm_dd", length = 10)
    private String dateOfBirth;

    /** CUST-EFT-ACCOUNT-ID PIC X(10) */
    @Column(name = "cust_eft_account_id", length = 10)
    private String eftAccountId;

    /** CUST-PRI-CARD-HOLDER-IND PIC X(01) – 'Y' or 'N' */
    @Column(name = "cust_pri_card_holder_ind", length = 1)
    private String primaryCardHolderInd;

    /** CUST-FICO-CREDIT-SCORE PIC 9(03) */
    @Column(name = "cust_fico_credit_score")
    private Integer ficoCreditScore;
}
