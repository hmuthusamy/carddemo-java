-- =============================================================================
-- V1__create_accounts_table.sql
-- Flyway migration: Create 'accounts' table
--
-- Source COBOL copybook: CVACT01Y.cpy  (ACCOUNT-RECORD, RECLN 300)
-- VSAM KSDS primary key: ACCT-ID
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.Account):
--   ACCT-ID                PIC 9(11)     → BIGINT          → Long       accountId
--   ACCT-ACTIVE-STATUS     PIC X(01)     → CHAR(1)         → String     activeStatus
--   ACCT-CURR-BAL          PIC S9(10)V99 → NUMERIC(12,2)   → BigDecimal currentBalance
--   ACCT-CREDIT-LIMIT      PIC S9(10)V99 → NUMERIC(12,2)   → BigDecimal creditLimit
--   ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99 → NUMERIC(12,2)   → BigDecimal cashCreditLimit
--   ACCT-OPEN-DATE         PIC X(10)     → VARCHAR(10)     → String     openDate
--   ACCT-EXPIRAION-DATE    PIC X(10)     → VARCHAR(10)     → String     expirationDate
--   ACCT-REISSUE-DATE      PIC X(10)     → VARCHAR(10)     → String     reissueDate
--   ACCT-CURR-CYC-CREDIT   PIC S9(10)V99 → NUMERIC(12,2)   → BigDecimal currCycleCredit
--   ACCT-CURR-CYC-DEBIT    PIC S9(10)V99 → NUMERIC(12,2)   → BigDecimal currCycleDebit
--   ACCT-ADDR-ZIP          PIC X(10)     → VARCHAR(10)     → String     addrZip
--   ACCT-GROUP-ID          PIC X(10)     → VARCHAR(10)     → String     groupId
-- =============================================================================

CREATE TABLE IF NOT EXISTS accounts (
    -- ACCT-ID  PIC 9(11)  — VSAM KSDS primary key
    -- Java: @Id Long accountId
    account_id              BIGINT          NOT NULL,

    -- ACCT-ACTIVE-STATUS  PIC X(01)
    -- Java: String activeStatus  (@Column length=1)
    active_status           CHAR(1)         NOT NULL DEFAULT 'Y'
                                CONSTRAINT chk_acct_active_status CHECK (active_status IN ('Y', 'N')),

    -- ACCT-CURR-BAL  PIC S9(10)V99  — signed: 10 integral digits + 2 decimal
    -- Java: BigDecimal currentBalance  (@Column precision=12, scale=2)
    current_balance         NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CREDIT-LIMIT  PIC S9(10)V99
    -- Java: BigDecimal creditLimit  (@Column precision=12, scale=2)
    credit_limit            NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CASH-CREDIT-LIMIT  PIC S9(10)V99
    -- Java: BigDecimal cashCreditLimit  (@Column precision=12, scale=2)
    cash_credit_limit       NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-OPEN-DATE  PIC X(10)  — ISO-8601 date string 'YYYY-MM-DD'
    -- Java: String openDate  (@Column length=10)
    open_date               VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-EXPIRAION-DATE  PIC X(10)  — original COBOL spelling preserved
    -- Java: String expirationDate  (@Column length=10)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-REISSUE-DATE  PIC X(10)
    -- Java: String reissueDate  (@Column length=10)
    reissue_date            VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-CURR-CYC-CREDIT  PIC S9(10)V99
    -- Java: BigDecimal currCycleCredit  (@Column name="curr_cycle_credit", precision=12, scale=2)
    curr_cycle_credit       NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-CURR-CYC-DEBIT  PIC S9(10)V99
    -- Java: BigDecimal currCycleDebit  (@Column name="curr_cycle_debit", precision=12, scale=2)
    curr_cycle_debit        NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,

    -- ACCT-ADDR-ZIP  PIC X(10)
    -- Java: String addrZip  (@Column name="addr_zip", length=10)
    addr_zip                VARCHAR(10)     NOT NULL DEFAULT '',

    -- ACCT-GROUP-ID  PIC X(10)
    -- Java: String groupId  (@Column name="group_id", length=10)
    group_id                VARCHAR(10)     NOT NULL DEFAULT '',

    -- Audit columns (non-COBOL; for operational lifecycle tracking)
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = ACCT-ID)
    CONSTRAINT pk_accounts PRIMARY KEY (account_id)
);

-- Index to support fast lookups by group_id
CREATE INDEX IF NOT EXISTS idx_accounts_group_id
    ON accounts (group_id);

-- Index to support lookups by zip code
CREATE INDEX IF NOT EXISTS idx_accounts_addr_zip
    ON accounts (addr_zip);

COMMENT ON TABLE  accounts                   IS 'Credit card account master — migrated from VSAM KSDS ACCTDAT (CVACT01Y)';
COMMENT ON COLUMN accounts.account_id        IS 'ACCT-ID PIC 9(11) — VSAM KSDS primary key; Java: Long accountId';
COMMENT ON COLUMN accounts.active_status     IS 'ACCT-ACTIVE-STATUS PIC X(01) — Y=active, N=inactive; Java: String activeStatus';
COMMENT ON COLUMN accounts.current_balance   IS 'ACCT-CURR-BAL PIC S9(10)V99; Java: BigDecimal currentBalance NUMERIC(12,2)';
COMMENT ON COLUMN accounts.credit_limit      IS 'ACCT-CREDIT-LIMIT PIC S9(10)V99; Java: BigDecimal creditLimit NUMERIC(12,2)';
COMMENT ON COLUMN accounts.cash_credit_limit IS 'ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99; Java: BigDecimal cashCreditLimit NUMERIC(12,2)';
COMMENT ON COLUMN accounts.open_date         IS 'ACCT-OPEN-DATE PIC X(10) — format YYYY-MM-DD; Java: String openDate';
COMMENT ON COLUMN accounts.expiration_date   IS 'ACCT-EXPIRAION-DATE PIC X(10) — format YYYY-MM-DD; Java: String expirationDate';
COMMENT ON COLUMN accounts.reissue_date      IS 'ACCT-REISSUE-DATE PIC X(10) — format YYYY-MM-DD; Java: String reissueDate';
COMMENT ON COLUMN accounts.curr_cycle_credit IS 'ACCT-CURR-CYC-CREDIT PIC S9(10)V99; Java: BigDecimal currCycleCredit NUMERIC(12,2)';
COMMENT ON COLUMN accounts.curr_cycle_debit  IS 'ACCT-CURR-CYC-DEBIT PIC S9(10)V99; Java: BigDecimal currCycleDebit NUMERIC(12,2)';
COMMENT ON COLUMN accounts.addr_zip          IS 'ACCT-ADDR-ZIP PIC X(10); Java: String addrZip';
COMMENT ON COLUMN accounts.group_id          IS 'ACCT-GROUP-ID PIC X(10); Java: String groupId';
