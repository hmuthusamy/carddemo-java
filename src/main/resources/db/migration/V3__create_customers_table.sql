-- =============================================================================
-- V3__create_customers_table.sql
-- Flyway migration: Create 'customers' table
--
-- Source COBOL copybook: CVCUS01Y.cpy  (CUSTOMER-RECORD, RECLN 500)
-- VSAM KSDS primary key: CUST-ID
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.Customer):
--   CUST-ID                  PIC 9(09)  → INTEGER     → Integer  customerId        (PK)
--   CUST-FIRST-NAME          PIC X(25)  → VARCHAR(25) → String   firstName
--   CUST-MIDDLE-NAME         PIC X(25)  → VARCHAR(25) → String   middleName
--   CUST-LAST-NAME           PIC X(25)  → VARCHAR(25) → String   lastName
--   CUST-ADDR-LINE-1         PIC X(50)  → VARCHAR(50) → String   addrLine1
--   CUST-ADDR-LINE-2         PIC X(50)  → VARCHAR(50) → String   addrLine2
--   CUST-ADDR-LINE-3         PIC X(50)  → VARCHAR(50) → String   addrLine3
--   CUST-ADDR-STATE-CD       PIC X(02)  → CHAR(2)     → String   addrStateCode
--   CUST-ADDR-COUNTRY-CD     PIC X(03)  → CHAR(3)     → String   addrCountryCode
--   CUST-ADDR-ZIP            PIC X(10)  → VARCHAR(10) → String   addrZip
--   CUST-PHONE-NUM-1         PIC X(15)  → VARCHAR(15) → String   phoneNum1
--   CUST-PHONE-NUM-2         PIC X(15)  → VARCHAR(15) → String   phoneNum2
--   CUST-SSN                 PIC 9(09)  → INTEGER     → Integer  ssn
--   CUST-GOVT-ISSUED-ID      PIC X(20)  → VARCHAR(20) → String   govtIssuedId
--   CUST-DOB-YYYY-MM-DD      PIC X(10)  → VARCHAR(10) → String   dateOfBirth
--   CUST-EFT-ACCOUNT-ID      PIC X(10)  → VARCHAR(10) → String   eftAccountId
--   CUST-PRI-CARD-HOLDER-IND PIC X(01)  → CHAR(1)     → String   primaryCardHolderInd
--   CUST-FICO-CREDIT-SCORE   PIC 9(03)  → SMALLINT    → Short    ficoCreditScore
-- =============================================================================

CREATE TABLE IF NOT EXISTS customers (
    -- CUST-ID  PIC 9(09)  — VSAM KSDS primary key
    -- Java: @Id Integer customerId
    customer_id                 INTEGER         NOT NULL,

    -- CUST-FIRST-NAME  PIC X(25)
    -- Java: String firstName  (@Column name="first_name", length=25)
    first_name                  VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-MIDDLE-NAME  PIC X(25)
    -- Java: String middleName  (@Column name="middle_name", length=25)
    middle_name                 VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-LAST-NAME  PIC X(25)
    -- Java: String lastName  (@Column name="last_name", length=25)
    last_name                   VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-1  PIC X(50)
    -- Java: String addrLine1  (@Column name="addr_line_1", length=50)
    addr_line_1                 VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-2  PIC X(50)
    -- Java: String addrLine2  (@Column name="addr_line_2", length=50)
    addr_line_2                 VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-3  PIC X(50)
    -- Java: String addrLine3  (@Column name="addr_line_3", length=50)
    addr_line_3                 VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-STATE-CD  PIC X(02)
    -- Java: String addrStateCode  (@Column name="addr_state_code", length=2)
    addr_state_code             CHAR(2)         NOT NULL DEFAULT '',

    -- CUST-ADDR-COUNTRY-CD  PIC X(03)
    -- Java: String addrCountryCode  (@Column name="addr_country_code", length=3)
    addr_country_code           CHAR(3)         NOT NULL DEFAULT '',

    -- CUST-ADDR-ZIP  PIC X(10)
    -- Java: String addrZip  (@Column name="addr_zip", length=10)
    addr_zip                    VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-1  PIC X(15)
    -- Java: String phoneNum1  (@Column name="phone_num_1", length=15)
    phone_num_1                 VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-2  PIC X(15)
    -- Java: String phoneNum2  (@Column name="phone_num_2", length=15)
    phone_num_2                 VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-SSN  PIC 9(09)  — Social Security Number stored as integer
    -- Java: Integer ssn  (@Column name="ssn")
    ssn                         INTEGER         NOT NULL DEFAULT 0,

    -- CUST-GOVT-ISSUED-ID  PIC X(20)
    -- Java: String govtIssuedId  (@Column name="govt_issued_id", length=20)
    govt_issued_id              VARCHAR(20)     NOT NULL DEFAULT '',

    -- CUST-DOB-YYYY-MM-DD  PIC X(10)  — date of birth as ISO-8601 string
    -- Java: String dateOfBirth  (@Column name="date_of_birth", length=10)
    date_of_birth               VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-EFT-ACCOUNT-ID  PIC X(10)
    -- Java: String eftAccountId  (@Column name="eft_account_id", length=10)
    eft_account_id              VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PRI-CARD-HOLDER-IND  PIC X(01)
    -- Java: String primaryCardHolderInd  (@Column name="primary_card_holder_ind", length=1)
    primary_card_holder_ind     CHAR(1)         NOT NULL DEFAULT 'Y'
                                    CONSTRAINT chk_cust_pri_holder CHECK (primary_card_holder_ind IN ('Y', 'N')),

    -- CUST-FICO-CREDIT-SCORE  PIC 9(03)  — FICO score range 300-850
    -- Java: Short ficoCreditScore  (@Column name="fico_credit_score")
    fico_credit_score           SMALLINT        NOT NULL DEFAULT 0
                                    CONSTRAINT chk_cust_fico CHECK (fico_credit_score BETWEEN 0 AND 999),

    -- Audit columns
    created_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = CUST-ID)
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
);

-- Index to support name-based customer lookups
CREATE INDEX IF NOT EXISTS idx_customers_last_name
    ON customers (last_name);

-- Partial unique index on SSN (excluding default 0 value)
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_ssn
    ON customers (ssn)
    WHERE ssn <> 0;

COMMENT ON TABLE  customers                            IS 'Customer master — migrated from VSAM KSDS CUSTDAT (CVCUS01Y)';
COMMENT ON COLUMN customers.customer_id                IS 'CUST-ID PIC 9(09) — VSAM KSDS primary key; Java: Integer customerId';
COMMENT ON COLUMN customers.first_name                 IS 'CUST-FIRST-NAME PIC X(25); Java: String firstName VARCHAR(25)';
COMMENT ON COLUMN customers.middle_name                IS 'CUST-MIDDLE-NAME PIC X(25); Java: String middleName VARCHAR(25)';
COMMENT ON COLUMN customers.last_name                  IS 'CUST-LAST-NAME PIC X(25); Java: String lastName VARCHAR(25)';
COMMENT ON COLUMN customers.addr_line_1                IS 'CUST-ADDR-LINE-1 PIC X(50); Java: String addrLine1 VARCHAR(50)';
COMMENT ON COLUMN customers.addr_line_2                IS 'CUST-ADDR-LINE-2 PIC X(50); Java: String addrLine2 VARCHAR(50)';
COMMENT ON COLUMN customers.addr_line_3                IS 'CUST-ADDR-LINE-3 PIC X(50); Java: String addrLine3 VARCHAR(50)';
COMMENT ON COLUMN customers.addr_state_code            IS 'CUST-ADDR-STATE-CD PIC X(02); Java: String addrStateCode CHAR(2)';
COMMENT ON COLUMN customers.addr_country_code          IS 'CUST-ADDR-COUNTRY-CD PIC X(03); Java: String addrCountryCode CHAR(3)';
COMMENT ON COLUMN customers.addr_zip                   IS 'CUST-ADDR-ZIP PIC X(10); Java: String addrZip VARCHAR(10)';
COMMENT ON COLUMN customers.phone_num_1                IS 'CUST-PHONE-NUM-1 PIC X(15); Java: String phoneNum1 VARCHAR(15)';
COMMENT ON COLUMN customers.phone_num_2                IS 'CUST-PHONE-NUM-2 PIC X(15); Java: String phoneNum2 VARCHAR(15)';
COMMENT ON COLUMN customers.ssn                        IS 'CUST-SSN PIC 9(09); Java: Integer ssn';
COMMENT ON COLUMN customers.govt_issued_id             IS 'CUST-GOVT-ISSUED-ID PIC X(20); Java: String govtIssuedId VARCHAR(20)';
COMMENT ON COLUMN customers.date_of_birth              IS 'CUST-DOB-YYYY-MM-DD PIC X(10) — format YYYY-MM-DD; Java: String dateOfBirth';
COMMENT ON COLUMN customers.eft_account_id             IS 'CUST-EFT-ACCOUNT-ID PIC X(10); Java: String eftAccountId VARCHAR(10)';
COMMENT ON COLUMN customers.primary_card_holder_ind    IS 'CUST-PRI-CARD-HOLDER-IND PIC X(01) — Y=primary, N=secondary; Java: String primaryCardHolderInd';
COMMENT ON COLUMN customers.fico_credit_score          IS 'CUST-FICO-CREDIT-SCORE PIC 9(03) — FICO score 300-850; Java: Short ficoCreditScore SMALLINT';
