-- =============================================================================
-- V1__create_accounts_table.sql
-- Flyway migration: Create 'accounts' table
--
-- Source COBOL copybook: CVACT01Y.cpy  (ACCOUNT-RECORD, RECLN 300)
-- VSAM KSDS primary key: ACCT-ID
--
-- COBOL → PostgreSQL type mapping:
--   PIC 9(11)          → BIGINT            (account_id — PK)
--   PIC X(01)          → CHAR(1)           (active status flag)
--   PIC S9(10)V99      → NUMERIC(12,2)     (signed 10-digit integer + 2 decimal)
--   PIC X(10)          → VARCHAR(10)       (date strings & zip)
--
-- Java entity: com.carddemo.model.Account
--   Long              ← BIGINT
--   String            ← CHAR/VARCHAR
--   BigDecimal        ← NUMERIC(12,2)
-- =============================================================================

CREATE TABLE IF NOT EXISTS accounts (
    -- ACCT-ID  PIC 9(11)  — VSAM KSDS primary key
    account_id              BIGINT          NOT NULL,

    -- ACCT-ACTIVE-STATUS  PIC X(01)
    active_status           CHAR(1)         NOT NULL DEFAULT 'Y'
                                CONSTRAINT chk_acct_active_status CHECK (active_status IN ('Y', 'N')),

    -- ACCT-CURR-BAL  PIC S9(10)V99  — signed 10 integral digits, 2 decimal
    current_balance         NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CREDIT-LIMIT  PIC S9(10)V99
    credit_limit            NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99
    cash_credit_limit       NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-OPEN-DATE  PIC X(10)  — stored as ISO-8601 string 'YYYY-MM-DD'
    open_date               VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-EXPIRAION-DATE  PIC X(10)  (original COBOL spelling preserved)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-REISSUE-DATE  PIC X(10)
    reissue_date            VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-CURR-CYC-CREDIT  PIC S9(10)V99
    current_cycle_credit    NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CURR-CYC-DEBIT  PIC S9(10)V99
    current_cycle_debit     NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-ADDR-ZIP  PIC X(10)
    address_zip             VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-GROUP-ID  PIC X(10)
    group_id                VARCHAR(10)     NOT NULL DEFAULT '',

    -- Audit columns (not in copybook FILLER; added for operational use)
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = ACCT-ID)
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

-- Index to support fast lookups by group_id (common query pattern)
CREATE INDEX IF NOT EXISTS idx_accounts_group_id
    ON accounts (group_id);

-- Index to support lookups by zip code
CREATE INDEX IF NOT EXISTS idx_accounts_address_zip
    ON accounts (address_zip);

COMMENT ON TABLE  accounts                      IS 'Credit card account master — migrated from VSAM KSDS ACCTDAT (CVACT01Y)';
COMMENT ON COLUMN accounts.account_id           IS 'ACCT-ID PIC 9(11) — VSAM KSDS primary key';
COMMENT ON COLUMN accounts.active_status        IS 'ACCT-ACTIVE-STATUS PIC X(01) — Y=active, N=inactive';
COMMENT ON COLUMN accounts.current_balance      IS 'ACCT-CURR-BAL PIC S9(10)V99';
COMMENT ON COLUMN accounts.credit_limit         IS 'ACCT-CREDIT-LIMIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.cash_credit_limit    IS 'ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.open_date            IS 'ACCT-OPEN-DATE PIC X(10) — format YYYY-MM-DD';
COMMENT ON COLUMN accounts.expiration_date      IS 'ACCT-EXPIRAION-DATE PIC X(10) — format YYYY-MM-DD';
COMMENT ON COLUMN accounts.reissue_date         IS 'ACCT-REISSUE-DATE PIC X(10) — format YYYY-MM-DD';
COMMENT ON COLUMN accounts.current_cycle_credit IS 'ACCT-CURR-CYC-CREDIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.current_cycle_debit  IS 'ACCT-CURR-CYC-DEBIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.address_zip          IS 'ACCT-ADDR-ZIP PIC X(10)';
COMMENT ON COLUMN accounts.group_id             IS 'ACCT-GROUP-ID PIC X(10)';
