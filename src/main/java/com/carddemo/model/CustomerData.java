package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java model for COBOL copybook CVCUS01Y.cpy
 * Data-structure for Customer entity (RECLN 500)
 * VSAM KSDS - keyed by CUST-ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class CustomerData {

    /** CUST-ID PIC 9(09) - Primary key */
    @Id
    @Column(name = "cust_id", precision = 9, nullable = false)
    private Long custId;

    /** CUST-FIRST-NAME PIC X(25) */
    @Column(name = "cust_first_name", length = 25)
    private String custFirstName;

    /** CUST-MIDDLE-NAME PIC X(25) */
    @Column(name = "cust_middle_name", length = 25)
    private String custMiddleName;

    /** CUST-LAST-NAME PIC X(25) */
    @Column(name = "cust_last_name", length = 25)
    private String custLastName;

    /** CUST-ADDR-LINE-1 PIC X(50) */
    @Column(name = "cust_addr_line_1", length = 50)
    private String custAddrLine1;

    /** CUST-ADDR-LINE-2 PIC X(50) */
    @Column(name = "cust_addr_line_2", length = 50)
    private String custAddrLine2;

    /** CUST-ADDR-LINE-3 PIC X(50) */
    @Column(name = "cust_addr_line_3", length = 50)
    private String custAddrLine3;

    /** CUST-ADDR-STATE-CD PIC X(02) */
    @Column(name = "cust_addr_state_cd", length = 2)
    private String custAddrStateCd;

    /** CUST-ADDR-COUNTRY-CD PIC X(03) */
    @Column(name = "cust_addr_country_cd", length = 3)
    private String custAddrCountryCd;

    /** CUST-ADDR-ZIP PIC X(10) */
    @Column(name = "cust_addr_zip", length = 10)
    private String custAddrZip;

    /** CUST-PHONE-NUM-1 PIC X(15) */
    @Column(name = "cust_phone_num_1", length = 15)
    private String custPhoneNum1;

    /** CUST-PHONE-NUM-2 PIC X(15) */
    @Column(name = "cust_phone_num_2", length = 15)
    private String custPhoneNum2;

    /** CUST-SSN PIC 9(09) */
    @Column(name = "cust_ssn", precision = 9)
    private Long custSsn;

    /** CUST-GOVT-ISSUED-ID PIC X(20) */
    @Column(name = "cust_govt_issued_id", length = 20)
    private String custGovtIssuedId;

    /** CUST-DOB-YYYY-MM-DD PIC X(10) */
    @Column(name = "cust_dob_yyyy_mm_dd", length = 10)
    private String custDobYyyyMmDd;

    /** CUST-EFT-ACCOUNT-ID PIC X(10) */
    @Column(name = "cust_eft_account_id", length = 10)
    private String custEftAccountId;

    /** CUST-PRI-CARD-HOLDER-IND PIC X(01) */
    @Column(name = "cust_pri_card_holder_ind", length = 1)
    private String custPriCardHolderInd;

    /** CUST-FICO-CREDIT-SCORE PIC 9(03) */
    @Column(name = "cust_fico_credit_score", precision = 3)
    private Integer custFicoCreditScore;
}
