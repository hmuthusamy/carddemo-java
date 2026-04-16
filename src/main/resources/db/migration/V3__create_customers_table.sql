-- =============================================================================
-- V3__create_customers_table.sql
-- Flyway DDL migration for the CUSTOMERS table
-- Source COBOL copybook: CVCUS01Y.cpy (CUSTOMER-RECORD, RECLN=500)
--
-- COBOL → PostgreSQL type mapping:
--   PIC 9(09)         → INTEGER        (customer id, SSN, FICO score)
--   PIC X(25)         → VARCHAR(25)    (name fields)
--   PIC X(50)         → VARCHAR(50)    (address lines)
--   PIC X(02)         → CHAR(2)        (state code)
--   PIC X(03)         → CHAR(3)        (country code)
--   PIC X(10)         → VARCHAR(10)    (zip, EFT acct, DOB)
--   PIC X(15)         → VARCHAR(15)    (phone numbers)
--   PIC X(20)         → VARCHAR(20)    (govt issued ID)
--   PIC X(01)         → CHAR(1)        (primary card holder indicator)
--   PIC 9(03)         → SMALLINT       (FICO credit score 000-999)
-- =============================================================================

CREATE TABLE IF NOT EXISTS customers (
    -- CUST-ID  PIC 9(09)  → primary key
    customer_id             INTEGER         NOT NULL,

    -- CUST-FIRST-NAME  PIC X(25)
    first_name              VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-MIDDLE-NAME  PIC X(25)
    middle_name             VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-LAST-NAME  PIC X(25)
    last_name               VARCHAR(25)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-1  PIC X(50)
    addr_line_1             VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-2  PIC X(50)
    addr_line_2             VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-LINE-3  PIC X(50)
    addr_line_3             VARCHAR(50)     NOT NULL DEFAULT '',

    -- CUST-ADDR-STATE-CD  PIC X(02)
    addr_state_code         CHAR(2)         NOT NULL DEFAULT '',

    -- CUST-ADDR-COUNTRY-CD  PIC X(03)
    addr_country_code       CHAR(3)         NOT NULL DEFAULT '',

    -- CUST-ADDR-ZIP  PIC X(10)
    addr_zip                VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-1  PIC X(15)
    phone_num_1             VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-PHONE-NUM-2  PIC X(15)
    phone_num_2             VARCHAR(15)     NOT NULL DEFAULT '',

    -- CUST-SSN  PIC 9(09)  → stored as integer (no leading zeros required)
    ssn                     INTEGER         NOT NULL DEFAULT 0,

    -- CUST-GOVT-ISSUED-ID  PIC X(20)
    govt_issued_id          VARCHAR(20)     NOT NULL DEFAULT '',

    -- CUST-DOB-YYYY-MM-DD  PIC X(10)
    date_of_birth           VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-EFT-ACCOUNT-ID  PIC X(10)
    eft_account_id          VARCHAR(10)     NOT NULL DEFAULT '',

    -- CUST-PRI-CARD-HOLDER-IND  PIC X(01)  → 'Y'/'N'
    primary_card_holder_ind CHAR(1)         NOT NULL DEFAULT 'Y',

    -- CUST-FICO-CREDIT-SCORE  PIC 9(03)  → 0–999
    fico_credit_score       SMALLINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT chk_customers_primary_holder CHECK (primary_card_holder_ind IN ('Y', 'N')),
    CONSTRAINT chk_customers_fico_range CHECK (fico_credit_score BETWEEN 0 AND 999)
);

-- Indexes
CREATE INDEX idx_customers_last_name  ON customers (last_name);
CREATE INDEX idx_customers_ssn        ON customers (ssn);
CREATE INDEX idx_customers_addr_zip   ON customers (addr_zip);

COMMENT ON TABLE  customers IS 'Customer master records migrated from VSAM KSDS CUSTDAT (CVCUS01Y.cpy)';
COMMENT ON COLUMN customers.customer_id             IS 'CUST-ID PIC 9(09) – unique customer identifier';
COMMENT ON COLUMN customers.first_name              IS 'CUST-FIRST-NAME PIC X(25)';
COMMENT ON COLUMN customers.middle_name             IS 'CUST-MIDDLE-NAME PIC X(25)';
COMMENT ON COLUMN customers.last_name               IS 'CUST-LAST-NAME PIC X(25)';
COMMENT ON COLUMN customers.addr_line_1             IS 'CUST-ADDR-LINE-1 PIC X(50)';
COMMENT ON COLUMN customers.addr_line_2             IS 'CUST-ADDR-LINE-2 PIC X(50)';
COMMENT ON COLUMN customers.addr_line_3             IS 'CUST-ADDR-LINE-3 PIC X(50)';
COMMENT ON COLUMN customers.addr_state_code         IS 'CUST-ADDR-STATE-CD PIC X(02)';
COMMENT ON COLUMN customers.addr_country_code       IS 'CUST-ADDR-COUNTRY-CD PIC X(03)';
COMMENT ON COLUMN customers.addr_zip                IS 'CUST-ADDR-ZIP PIC X(10)';
COMMENT ON COLUMN customers.phone_num_1             IS 'CUST-PHONE-NUM-1 PIC X(15)';
COMMENT ON COLUMN customers.phone_num_2             IS 'CUST-PHONE-NUM-2 PIC X(15)';
COMMENT ON COLUMN customers.ssn                     IS 'CUST-SSN PIC 9(09) – social security number';
COMMENT ON COLUMN customers.govt_issued_id          IS 'CUST-GOVT-ISSUED-ID PIC X(20)';
COMMENT ON COLUMN customers.date_of_birth           IS 'CUST-DOB-YYYY-MM-DD PIC X(10) – ISO date string';
COMMENT ON COLUMN customers.eft_account_id          IS 'CUST-EFT-ACCOUNT-ID PIC X(10)';
COMMENT ON COLUMN customers.primary_card_holder_ind IS 'CUST-PRI-CARD-HOLDER-IND PIC X(01) – Y=primary';
COMMENT ON COLUMN customers.fico_credit_score       IS 'CUST-FICO-CREDIT-SCORE PIC 9(03) – 0-999';
