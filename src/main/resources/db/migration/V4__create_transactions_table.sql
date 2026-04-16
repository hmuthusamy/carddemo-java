-- =============================================================================
-- V4__create_transactions_table.sql
-- Flyway migration: Create 'transactions' table
--
-- Source COBOL copybook: CVTRA05Y.cpy  (TRAN-RECORD, RECLN 350)
-- VSAM KSDS primary key: TRAN-ID
-- Foreign keys:
--   TRAN-CARD-NUM → cards.card_number
--   (account_id resolved through card_account_xref)
--
-- COBOL → PostgreSQL type mapping:
--   PIC X(16)       → CHAR(16)        (tran_id — PK; also card number)
--   PIC X(02)       → CHAR(2)         (transaction type code)
--   PIC 9(04)       → INTEGER         (transaction category code)
--   PIC X(10)       → VARCHAR(10)     (tran source)
--   PIC X(100)      → VARCHAR(100)    (description)
--   PIC S9(09)V99   → NUMERIC(11,2)   (signed 9-digit integer + 2 decimal)
--   PIC 9(09)       → INTEGER         (merchant_id)
--   PIC X(50)       → VARCHAR(50)     (merchant name, city)
--   PIC X(26)       → VARCHAR(26)     (timestamp strings)
--
-- Java entity: com.carddemo.model.Transaction
--   String      ← CHAR/VARCHAR
--   Integer     ← INTEGER
--   BigDecimal  ← NUMERIC(11,2)
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    -- TRAN-ID  PIC X(16)  — VSAM KSDS primary key
    transaction_id          CHAR(16)        NOT NULL,

    -- TRAN-TYPE-CD  PIC X(02)  — e.g., 'PR'=purchase, 'CR'=credit, 'PM'=payment
    transaction_type_code   CHAR(2)         NOT NULL DEFAULT '',

    -- TRAN-CAT-CD  PIC 9(04)  — numeric category code (0000–9999)
    transaction_category_cd INTEGER         NOT NULL DEFAULT 0
                                CONSTRAINT chk_tran_cat_cd CHECK (transaction_category_cd BETWEEN 0 AND 9999),

    -- TRAN-SOURCE  PIC X(10)
    transaction_source      VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-DESC  PIC X(100)
    transaction_description VARCHAR(100)    NOT NULL DEFAULT '',

    -- TRAN-AMT  PIC S9(09)V99  — signed amount: 9 integral + 2 decimal digits
    transaction_amount      NUMERIC(11, 2)  NOT NULL DEFAULT 0.00,

    -- TRAN-MERCHANT-ID  PIC 9(09)  — up to 9-digit merchant identifier
    merchant_id             INTEGER         NOT NULL DEFAULT 0,

    -- TRAN-MERCHANT-NAME  PIC X(50)
    merchant_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-CITY  PIC X(50)
    merchant_city           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-ZIP  PIC X(10)
    merchant_zip            VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-CARD-NUM  PIC X(16)  — FK to cards.card_number
    card_number             CHAR(16)        NOT NULL,

    -- TRAN-ORIG-TS  PIC X(26)  — origination timestamp (ISO-8601 with micros)
    original_timestamp      VARCHAR(26)     NOT NULL DEFAULT '',

    -- TRAN-PROC-TS  PIC X(26)  — processing timestamp
    processed_timestamp     VARCHAR(26)     NOT NULL DEFAULT '',

    -- Audit columns
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = TRAN-ID)
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),

    -- FOREIGN KEY  TRAN-CARD-NUM → cards.card_number
    CONSTRAINT fk_transactions_card_number
        FOREIGN KEY (card_number)
        REFERENCES cards (card_number)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Index for card-number-based transaction history queries
CREATE INDEX IF NOT EXISTS idx_transactions_card_number
    ON transactions (card_number);

-- Index for type-code filtering (e.g., all purchases)
CREATE INDEX IF NOT EXISTS idx_transactions_type_code
    ON transactions (transaction_type_code);

-- Index for merchant-id-based analytics
CREATE INDEX IF NOT EXISTS idx_transactions_merchant_id
    ON transactions (merchant_id);

-- Index for date-range queries using original_timestamp prefix (YYYY-MM-DD)
CREATE INDEX IF NOT EXISTS idx_transactions_orig_ts
    ON transactions (original_timestamp);

COMMENT ON TABLE  transactions                          IS 'Transaction ledger — migrated from VSAM KSDS TRANSACT (CVTRA05Y)';
COMMENT ON COLUMN transactions.transaction_id           IS 'TRAN-ID PIC X(16) — VSAM KSDS primary key';
COMMENT ON COLUMN transactions.transaction_type_code    IS 'TRAN-TYPE-CD PIC X(02) — e.g. PR=purchase, CR=credit, PM=payment';
COMMENT ON COLUMN transactions.transaction_category_cd  IS 'TRAN-CAT-CD PIC 9(04) — numeric category code';
COMMENT ON COLUMN transactions.transaction_source       IS 'TRAN-SOURCE PIC X(10)';
COMMENT ON COLUMN transactions.transaction_description  IS 'TRAN-DESC PIC X(100)';
COMMENT ON COLUMN transactions.transaction_amount       IS 'TRAN-AMT PIC S9(09)V99 — signed transaction amount';
COMMENT ON COLUMN transactions.merchant_id              IS 'TRAN-MERCHANT-ID PIC 9(09)';
COMMENT ON COLUMN transactions.merchant_name            IS 'TRAN-MERCHANT-NAME PIC X(50)';
COMMENT ON COLUMN transactions.merchant_city            IS 'TRAN-MERCHANT-CITY PIC X(50)';
COMMENT ON COLUMN transactions.merchant_zip             IS 'TRAN-MERCHANT-ZIP PIC X(10)';
COMMENT ON COLUMN transactions.card_number              IS 'TRAN-CARD-NUM PIC X(16) — FK to cards.card_number';
COMMENT ON COLUMN transactions.original_timestamp       IS 'TRAN-ORIG-TS PIC X(26) — origination timestamp';
COMMENT ON COLUMN transactions.processed_timestamp      IS 'TRAN-PROC-TS PIC X(26) — processing timestamp';
