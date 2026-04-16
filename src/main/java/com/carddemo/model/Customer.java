package com.carddemo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for the CUSTOMERS table.
 *
 * <p>Migrated from COBOL copybook CVCUS01Y.cpy (CUSTOMER-RECORD, RECLN=500).
 *
 * <pre>
 * Column type mapping:
 *   customer_id             INTEGER      ← PIC 9(09)
 *   first_name              VARCHAR(25)  ← PIC X(25)
 *   middle_name             VARCHAR(25)  ← PIC X(25)
 *   last_name               VARCHAR(25)  ← PIC X(25)
 *   addr_line_1             VARCHAR(50)  ← PIC X(50)
 *   addr_line_2             VARCHAR(50)  ← PIC X(50)
 *   addr_line_3             VARCHAR(50)  ← PIC X(50)
 *   addr_state_code         CHAR(2)      ← PIC X(02)
 *   addr_country_code       CHAR(3)      ← PIC X(03)
 *   addr_zip                VARCHAR(10)  ← PIC X(10)
 *   phone_num_1             VARCHAR(15)  ← PIC X(15)
 *   phone_num_2             VARCHAR(15)  ← PIC X(15)
 *   ssn                     INTEGER      ← PIC 9(09)
 *   govt_issued_id          VARCHAR(20)  ← PIC X(20)
 *   date_of_birth           VARCHAR(10)  ← PIC X(10)
 *   eft_account_id          VARCHAR(10)  ← PIC X(10)
 *   primary_card_holder_ind CHAR(1)      ← PIC X(01)
 *   fico_credit_score       SMALLINT     ← PIC 9(03)
 * </pre>
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /** CUST-ID PIC 9(09) – primary key. Maps to DDL: INTEGER NOT NULL */
    @Id
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    /** CUST-FIRST-NAME PIC X(25). Maps to DDL: VARCHAR(25) NOT NULL */
    @Column(name = "first_name", nullable = false, length = 25)
    private String firstName;

    /** CUST-MIDDLE-NAME PIC X(25). Maps to DDL: VARCHAR(25) NOT NULL */
    @Column(name = "middle_name", nullable = false, length = 25)
    private String middleName;

    /** CUST-LAST-NAME PIC X(25). Maps to DDL: VARCHAR(25) NOT NULL */
    @Column(name = "last_name", nullable = false, length = 25)
    private String lastName;

    /** CUST-ADDR-LINE-1 PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "addr_line_1", nullable = false, length = 50)
    private String addrLine1;

    /** CUST-ADDR-LINE-2 PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "addr_line_2", nullable = false, length = 50)
    private String addrLine2;

    /** CUST-ADDR-LINE-3 PIC X(50). Maps to DDL: VARCHAR(50) NOT NULL */
    @Column(name = "addr_line_3", nullable = false, length = 50)
    private String addrLine3;

    /** CUST-ADDR-STATE-CD PIC X(02). Maps to DDL: CHAR(2) NOT NULL */
    @Column(name = "addr_state_code", nullable = false, length = 2)
    private String addrStateCode;

    /** CUST-ADDR-COUNTRY-CD PIC X(03). Maps to DDL: CHAR(3) NOT NULL */
    @Column(name = "addr_country_code", nullable = false, length = 3)
    private String addrCountryCode;

    /** CUST-ADDR-ZIP PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "addr_zip", nullable = false, length = 10)
    private String addrZip;

    /** CUST-PHONE-NUM-1 PIC X(15). Maps to DDL: VARCHAR(15) NOT NULL */
    @Column(name = "phone_num_1", nullable = false, length = 15)
    private String phoneNum1;

    /** CUST-PHONE-NUM-2 PIC X(15). Maps to DDL: VARCHAR(15) NOT NULL */
    @Column(name = "phone_num_2", nullable = false, length = 15)
    private String phoneNum2;

    /** CUST-SSN PIC 9(09). Maps to DDL: INTEGER NOT NULL */
    @Column(name = "ssn", nullable = false)
    private Integer ssn;

    /** CUST-GOVT-ISSUED-ID PIC X(20). Maps to DDL: VARCHAR(20) NOT NULL */
    @Column(name = "govt_issued_id", nullable = false, length = 20)
    private String govtIssuedId;

    /** CUST-DOB-YYYY-MM-DD PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "date_of_birth", nullable = false, length = 10)
    private String dateOfBirth;

    /** CUST-EFT-ACCOUNT-ID PIC X(10). Maps to DDL: VARCHAR(10) NOT NULL */
    @Column(name = "eft_account_id", nullable = false, length = 10)
    private String eftAccountId;

    /** CUST-PRI-CARD-HOLDER-IND PIC X(01). Maps to DDL: CHAR(1) NOT NULL */
    @Column(name = "primary_card_holder_ind", nullable = false, length = 1)
    private String primaryCardHolderInd;

    /** CUST-FICO-CREDIT-SCORE PIC 9(03). Maps to DDL: SMALLINT NOT NULL */
    @Column(name = "fico_credit_score", nullable = false)
    private Short ficoCreditScore;
}
