-- =============================================================================
-- V1__create_accounts_table.sql
-- Flyway DDL migration for the ACCOUNTS table
-- Source COBOL copybook: CVACT01Y.cpy (ACCOUNT-RECORD, RECLN=300)
--
-- COBOL → PostgreSQL type mapping:
--   PIC 9(11)         → BIGINT           (account_id, 11-digit numeric)
--   PIC X(01)         → CHAR(1)          (active status flag)
--   PIC S9(10)V99     → NUMERIC(12,2)    (signed 10-integer + 2-decimal)
--   PIC X(10)         → VARCHAR(10)      (date strings / zip / group id)
-- =============================================================================

CREATE TABLE IF NOT EXISTS accounts (
    -- ACCT-ID  PIC 9(11)  → primary key
    account_id              BIGINT          NOT NULL,

    -- ACCT-ACTIVE-STATUS  PIC X(01)  → 'Y'/'N' flag
    active_status           CHAR(1)         NOT NULL DEFAULT 'Y',

    -- ACCT-CURR-BAL  PIC S9(10)V99  → signed 12-digit, 2 decimal places
    current_balance         NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CREDIT-LIMIT  PIC S9(10)V99
    credit_limit            NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99
    cash_credit_limit       NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-OPEN-DATE  PIC X(10)  → stored as 'YYYY-MM-DD' string
    open_date               VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-EXPIRAION-DATE  PIC X(10)  (note: COBOL typo preserved in naming)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-REISSUE-DATE  PIC X(10)
    reissue_date            VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-CURR-CYC-CREDIT  PIC S9(10)V99
    curr_cycle_credit       NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CURR-CYC-DEBIT  PIC S9(10)V99
    curr_cycle_debit        NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-ADDR-ZIP  PIC X(10)
    addr_zip                VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-GROUP-ID  PIC X(10)
    group_id                VARCHAR(10)     NOT NULL DEFAULT '',

    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT chk_accounts_active_status CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_accounts_credit_limit_positive CHECK (credit_limit >= 0),
    CONSTRAINT chk_accounts_cash_credit_limit_positive CHECK (cash_credit_limit >= 0)
);

-- Indexes supporting common query patterns
CREATE INDEX idx_accounts_active_status ON accounts (active_status);
CREATE INDEX idx_accounts_group_id      ON accounts (group_id);
CREATE INDEX idx_accounts_addr_zip      ON accounts (addr_zip);

COMMENT ON TABLE  accounts IS 'Card account master records migrated from VSAM KSDS ACCTDAT (CVACT01Y.cpy)';
COMMENT ON COLUMN accounts.account_id         IS 'ACCT-ID PIC 9(11) – unique account identifier';
COMMENT ON COLUMN accounts.active_status       IS 'ACCT-ACTIVE-STATUS PIC X(01) – Y=active, N=inactive';
COMMENT ON COLUMN accounts.current_balance     IS 'ACCT-CURR-BAL PIC S9(10)V99';
COMMENT ON COLUMN accounts.credit_limit        IS 'ACCT-CREDIT-LIMIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.cash_credit_limit   IS 'ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.open_date           IS 'ACCT-OPEN-DATE PIC X(10) – ISO date string YYYY-MM-DD';
COMMENT ON COLUMN accounts.expiration_date     IS 'ACCT-EXPIRAION-DATE PIC X(10) – ISO date string YYYY-MM-DD';
COMMENT ON COLUMN accounts.reissue_date        IS 'ACCT-REISSUE-DATE PIC X(10) – ISO date string YYYY-MM-DD';
COMMENT ON COLUMN accounts.curr_cycle_credit   IS 'ACCT-CURR-CYC-CREDIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.curr_cycle_debit    IS 'ACCT-CURR-CYC-DEBIT PIC S9(10)V99';
COMMENT ON COLUMN accounts.addr_zip            IS 'ACCT-ADDR-ZIP PIC X(10)';
COMMENT ON COLUMN accounts.group_id            IS 'ACCT-GROUP-ID PIC X(10)';
