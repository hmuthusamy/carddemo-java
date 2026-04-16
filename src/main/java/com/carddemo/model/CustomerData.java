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
 * CustomerData – JPA entity mapped from COBOL copybook CVCUS01Y.
 *
 * COBOL layout (CVCUS01Y):
 *   01 CUSTOMER-RECORD.
 *      05 CUST-ID                        PIC 9(09).
 *      05 CUST-FIRST-NAME                PIC X(25).
 *      05 CUST-MIDDLE-NAME               PIC X(25).
 *      05 CUST-LAST-NAME                 PIC X(25).
 *      05 CUST-ADDR-LINE-1               PIC X(50).
 *      05 CUST-ADDR-LINE-2               PIC X(50).
 *      05 CUST-ADDR-LINE-3               PIC X(50).   (city)
 *      05 CUST-ADDR-STATE-CD             PIC X(02).
 *      05 CUST-ADDR-ZIP                  PIC X(10).
 *      05 CUST-ADDR-COUNTRY-CD           PIC X(03).
 *      05 CUST-PHONE-NUM-1               PIC X(15).
 *      05 CUST-PHONE-NUM-2               PIC X(15).
 *      05 CUST-SSN                       PIC X(09).
 *      05 CUST-GOVT-ISSUED-ID            PIC X(20).
 *      05 CUST-DOB-YYYY-MM-DD            PIC X(10).
 *      05 CUST-EFT-ACCOUNT-ID            PIC X(10).
 *      05 CUST-PRI-CARD-HOLDER-IND       PIC X(01).
 *      05 CUST-FICO-CREDIT-SCORE         PIC 9(03).
 */
@Entity
@Table(name = "customer_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerData {

    @Id
    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "cust_first_name", length = 25)
    private String custFirstName;

    @Column(name = "cust_middle_name", length = 25)
    private String custMiddleName;

    @Column(name = "cust_last_name", length = 25)
    private String custLastName;

    @Column(name = "cust_addr_line_1", length = 50)
    private String custAddrLine1;

    @Column(name = "cust_addr_line_2", length = 50)
    private String custAddrLine2;

    /** Maps to CUST-ADDR-LINE-3 (used as city) PIC X(50) */
    @Column(name = "cust_addr_line_3", length = 50)
    private String custAddrLine3;

    @Column(name = "cust_addr_state_cd", length = 2)
    private String custAddrStateCd;

    @Column(name = "cust_addr_zip", length = 10)
    private String custAddrZip;

    @Column(name = "cust_addr_country_cd", length = 3)
    private String custAddrCountryCd;

    @Column(name = "cust_phone_num_1", length = 15)
    private String custPhoneNum1;

    @Column(name = "cust_phone_num_2", length = 15)
    private String custPhoneNum2;

    /** Maps to CUST-SSN PIC X(09) */
    @Column(name = "cust_ssn", length = 9)
    private String custSsn;

    @Column(name = "cust_govt_issued_id", length = 20)
    private String custGovtIssuedId;

    @Column(name = "cust_dob_yyyy_mm_dd", length = 10)
    private String custDobYyyyMmDd;

    @Column(name = "cust_eft_account_id", length = 10)
    private String custEftAccountId;

    @Column(name = "cust_pri_card_holder_ind", length = 1)
    private String custPriCardHolderInd;

    @Column(name = "cust_fico_credit_score")
    private Integer custFicoCreditScore;
}
