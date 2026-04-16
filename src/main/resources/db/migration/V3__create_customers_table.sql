-- =============================================================================
-- V3__create_customers_table.sql
-- Flyway migration: Create 'customers' table
--
-- Source COBOL copybook: CVCUS01Y.cpy  (CUSTOMER-RECORD, RECLN 500)
-- VSAM KSDS primary key: CUST-ID
--
-- COBOL → PostgreSQL type mapping:
--   PIC 9(09)    → INTEGER / BIGINT   (customer_id — PK)
--   PIC X(25)    → VARCHAR(25)        (name fields)
--   PIC X(50)    → VARCHAR(50)        (address lines)
--   PIC X(02)    → CHAR(2)            (state code)
--   PIC X(03)    → CHAR(3)            (country code)
--   PIC X(10)    → VARCHAR(10)        (zip, date-of-birth)
--   PIC X(15)    → VARCHAR(15)        (phone numbers)
--   PIC X(20)    → VARCHAR(20)        (government-issued ID)
--   PIC X(01)    → CHAR(1)            (primary card-holder indicator)
--   PIC 9(03)    → SMALLINT           (FICO credit score, 300–850)
--
-- Java entity: com.carddemo.model.Customer
--   Long/Integer ← BIGINT/INTEGER
--   String       ← VARCHAR/CHAR
-- =============================================================================

CREATE TABLE IF NOT EXISTS customers (
    -- CUST-ID  PIC 9(09)  — VSAM KSDS primary key
    customer_id                 INTEGER         NOT NULL,

    -- CUST-FIRST-NAME  PIC X(25)
    first_name                  VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-MIDDLE-NAME  PIC X(25)
    middle_name                 VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-LAST-NAME  PIC X(25)
    last_name                   VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-1  PIC X(50)
    address_line_1              VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-2  PIC X(50)
    address_line_2              VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-3  PIC X(50)
    address_line_3              VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-STATE-CD  PIC X(02)
    address_state_code          CHAR(2)         NOT NULL DEFAULT '',

    -- CUST-ADDR-COUNTRY-CD  PIC X(03)
    address_country_code        CHAR(3)         NOT NULL DEFAULT '',

    -- CUST-ADDR-ZIP  PIC X(10)
    address_zip                 VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-1  PIC X(15)
    phone_number_1              VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-2  PIC X(15)
    phone_number_2              VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-SSN  PIC 9(09)  — stored as integer (no punctuation in COBOL)
    ssn                         INTEGER         NOT NULL DEFAULT 0,

    -- CUST-GOVT-ISSUED-ID  PIC X(20)
    govt_issued_id              VARCHAR(20)     NOT NULL DEFAULT '',

    -- CUST-DOB-YYYY-MM-DD  PIC X(10)  — stored as ISO-8601 string
    date_of_birth               VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-EFT-ACCOUNT-ID  PIC X(10)
    eft_account_id              VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PRI-CARD-HOLDER-IND  PIC X(01)
    primary_card_holder_ind     CHAR(1)         NOT NULL DEFAULT 'Y'
                                    CONSTRAINT chk_cust_pri_holder CHECK (primary_card_holder_ind IN ('Y', 'N')),

    -- CUST-FICO-CREDIT-SCORE  PIC 9(03)  — FICO range 300-850
    fico_credit_score           SMALLINT        NOT NULL DEFAULT 0
                                    CONSTRAINT chk_cust_fico CHECK (fico_credit_score BETWEEN 0 AND 999),

    -- Audit columns
    created_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = CUST-ID)
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
);

-- Index to support name-based searches
CREATE INDEX IF NOT EXISTS idx_customers_last_name
    ON customers (last_name);

-- Index to support SSN lookups (unique business identifier)
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_ssn
    ON customers (ssn)
    WHERE ssn <> 0;

COMMENT ON TABLE  customers                         IS 'Customer master — migrated from VSAM KSDS CUSTDAT (CVCUS01Y)';
COMMENT ON COLUMN customers.customer_id             IS 'CUST-ID PIC 9(09) — VSAM KSDS primary key';
COMMENT ON COLUMN customers.first_name              IS 'CUST-FIRST-NAME PIC X(25)';
COMMENT ON COLUMN customers.middle_name             IS 'CUST-MIDDLE-NAME PIC X(25)';
COMMENT ON COLUMN customers.last_name               IS 'CUST-LAST-NAME PIC X(25)';
COMMENT ON COLUMN customers.address_line_1          IS 'CUST-ADDR-LINE-1 PIC X(50)';
COMMENT ON COLUMN customers.address_line_2          IS 'CUST-ADDR-LINE-2 PIC X(50)';
COMMENT ON COLUMN customers.address_line_3          IS 'CUST-ADDR-LINE-3 PIC X(50)';
COMMENT ON COLUMN customers.address_state_code      IS 'CUST-ADDR-STATE-CD PIC X(02)';
COMMENT ON COLUMN customers.address_country_code    IS 'CUST-ADDR-COUNTRY-CD PIC X(03)';
COMMENT ON COLUMN customers.address_zip             IS 'CUST-ADDR-ZIP PIC X(10)';
COMMENT ON COLUMN customers.phone_number_1          IS 'CUST-PHONE-NUM-1 PIC X(15)';
COMMENT ON COLUMN customers.phone_number_2          IS 'CUST-PHONE-NUM-2 PIC X(15)';
COMMENT ON COLUMN customers.ssn                     IS 'CUST-SSN PIC 9(09) — Social Security Number';
COMMENT ON COLUMN customers.govt_issued_id          IS 'CUST-GOVT-ISSUED-ID PIC X(20)';
COMMENT ON COLUMN customers.date_of_birth           IS 'CUST-DOB-YYYY-MM-DD PIC X(10) — format YYYY-MM-DD';
COMMENT ON COLUMN customers.eft_account_id          IS 'CUST-EFT-ACCOUNT-ID PIC X(10)';
COMMENT ON COLUMN customers.primary_card_holder_ind IS 'CUST-PRI-CARD-HOLDER-IND PIC X(01) — Y=primary, N=secondary';
COMMENT ON COLUMN customers.fico_credit_score       IS 'CUST-FICO-CREDIT-SCORE PIC 9(03) — FICO score 300-850';
