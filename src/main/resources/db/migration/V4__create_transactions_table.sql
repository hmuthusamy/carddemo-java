-- =============================================================================
-- V4__create_transactions_table.sql
-- Flyway DDL migration for the TRANSACTIONS table
-- Source COBOL copybook: CVTRA05Y.cpy (TRAN-RECORD, RECLN=350)
--
-- COBOL → PostgreSQL type mapping:
--   PIC X(16)         → VARCHAR(16)    (transaction id, card number)
--   PIC X(02)         → CHAR(2)        (transaction type code)
--   PIC 9(04)         → SMALLINT       (transaction category code)
--   PIC X(10)         → VARCHAR(10)    (transaction source)
--   PIC X(100)        → VARCHAR(100)   (transaction description)
--   PIC S9(09)V99     → NUMERIC(11,2)  (signed 9-integer + 2-decimal amount)
--   PIC 9(09)         → INTEGER        (merchant id)
--   PIC X(50)         → VARCHAR(50)    (merchant name, city)
--   PIC X(26)         → VARCHAR(26)    (timestamps YYYY-MM-DD HH:MM:SS.ffffff)
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    -- TRAN-ID  PIC X(16)  → primary key
    transaction_id          VARCHAR(16)     NOT NULL,

    -- TRAN-TYPE-CD  PIC X(02)  → e.g. 'PU'=purchase, 'CR'=credit
    transaction_type_code   CHAR(2)         NOT NULL,

    -- TRAN-CAT-CD  PIC 9(04)  → category/MCC code 0000-9999
    transaction_cat_code    SMALLINT        NOT NULL DEFAULT 0,

    -- TRAN-SOURCE  PIC X(10)
    transaction_source      VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-DESC  PIC X(100)
    transaction_desc        VARCHAR(100)    NOT NULL DEFAULT '',

    -- TRAN-AMT  PIC S9(09)V99  → signed 11-digit, 2 decimal places
    transaction_amount      NUMERIC(11, 2)  NOT NULL DEFAULT 0.00,

    -- TRAN-MERCHANT-ID  PIC 9(09)
    merchant_id             INTEGER         NOT NULL DEFAULT 0,

    -- TRAN-MERCHANT-NAME  PIC X(50)
    merchant_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-CITY  PIC X(50)
    merchant_city           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-ZIP  PIC X(10)
    merchant_zip            VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-CARD-NUM  PIC X(16)  → FK to cards
    card_number             VARCHAR(16)     NOT NULL,

    -- TRAN-ORIG-TS  PIC X(26)  → 'YYYY-MM-DD HH:MM:SS.ffffff'
    orig_timestamp          VARCHAR(26)     NOT NULL DEFAULT '',

    -- TRAN-PROC-TS  PIC X(26)
    proc_timestamp          VARCHAR(26)     NOT NULL DEFAULT '',

    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_card_number FOREIGN KEY (card_number)
        REFERENCES cards (card_number)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Indexes
CREATE INDEX idx_transactions_card_number  ON transactions (card_number);
CREATE INDEX idx_transactions_type_code    ON transactions (transaction_type_code);
CREATE INDEX idx_transactions_orig_ts      ON transactions (orig_timestamp);
CREATE INDEX idx_transactions_merchant_id  ON transactions (merchant_id);

COMMENT ON TABLE  transactions IS 'Transaction records migrated from VSAM KSDS TRANSACT (CVTRA05Y.cpy)';
COMMENT ON COLUMN transactions.transaction_id        IS 'TRAN-ID PIC X(16) – unique transaction identifier';
COMMENT ON COLUMN transactions.transaction_type_code IS 'TRAN-TYPE-CD PIC X(02) – transaction type e.g. PU, CR';
COMMENT ON COLUMN transactions.transaction_cat_code  IS 'TRAN-CAT-CD PIC 9(04) – MCC or category code 0-9999';
COMMENT ON COLUMN transactions.transaction_source    IS 'TRAN-SOURCE PIC X(10)';
COMMENT ON COLUMN transactions.transaction_desc      IS 'TRAN-DESC PIC X(100)';
COMMENT ON COLUMN transactions.transaction_amount    IS 'TRAN-AMT PIC S9(09)V99 – signed amount with 2 decimals';
COMMENT ON COLUMN transactions.merchant_id           IS 'TRAN-MERCHANT-ID PIC 9(09)';
COMMENT ON COLUMN transactions.merchant_name         IS 'TRAN-MERCHANT-NAME PIC X(50)';
COMMENT ON COLUMN transactions.merchant_city         IS 'TRAN-MERCHANT-CITY PIC X(50)';
COMMENT ON COLUMN transactions.merchant_zip          IS 'TRAN-MERCHANT-ZIP PIC X(10)';
COMMENT ON COLUMN transactions.card_number           IS 'TRAN-CARD-NUM PIC X(16) – FK to cards';
COMMENT ON COLUMN transactions.orig_timestamp        IS 'TRAN-ORIG-TS PIC X(26) – origination timestamp';
COMMENT ON COLUMN transactions.proc_timestamp        IS 'TRAN-PROC-TS PIC X(26) – processing timestamp';
