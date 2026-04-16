-- =============================================================================
-- V4__create_transactions_table.sql
-- Flyway migration: Create 'transactions' table
--
-- Source COBOL copybook: CVTRA05Y.cpy  (TRAN-RECORD, RECLN 350)
-- VSAM KSDS primary key: TRAN-ID
-- Foreign key: TRAN-CARD-NUM → cards.card_number
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.Transaction):
--   TRAN-ID          PIC X(16)      → VARCHAR(16)   → String     transactionId       (PK)
--   TRAN-TYPE-CD     PIC X(02)      → CHAR(2)       → String     transactionTypeCode
--   TRAN-CAT-CD      PIC 9(04)      → SMALLINT      → Short      transactionCatCode
--   TRAN-SOURCE      PIC X(10)      → VARCHAR(10)   → String     transactionSource
--   TRAN-DESC        PIC X(100)     → VARCHAR(100)  → String     transactionDesc
--   TRAN-AMT         PIC S9(09)V99  → NUMERIC(11,2) → BigDecimal transactionAmount
--   TRAN-MERCHANT-ID PIC 9(09)      → INTEGER       → Integer    merchantId
--   TRAN-MERCHANT-NM PIC X(50)      → VARCHAR(50)   → String     merchantName
--   TRAN-MERCHANT-CT PIC X(50)      → VARCHAR(50)   → String     merchantCity
--   TRAN-MERCHANT-ZP PIC X(10)      → VARCHAR(10)   → String     merchantZip
--   TRAN-CARD-NUM    PIC X(16)      → VARCHAR(16)   → String     cardNumber          (FK)
--   TRAN-ORIG-TS     PIC X(26)      → VARCHAR(26)   → String     origTimestamp
--   TRAN-PROC-TS     PIC X(26)      → VARCHAR(26)   → String     procTimestamp
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    -- TRAN-ID  PIC X(16)  — VSAM KSDS primary key
    -- Java: @Id String transactionId  (@Column length=16)
    transaction_id          VARCHAR(16)     NOT NULL,

    -- TRAN-TYPE-CD  PIC X(02)  — e.g., 'PR'=purchase, 'CR'=credit, 'PM'=payment
    -- Java: String transactionTypeCode  (@Column name="transaction_type_code", length=2)
    transaction_type_code   CHAR(2)         NOT NULL DEFAULT '',

    -- TRAN-CAT-CD  PIC 9(04)  — 4-digit numeric category code; Java Short max = 32767 > 9999
    -- Java: Short transactionCatCode  (@Column name="transaction_cat_code")
    transaction_cat_code    SMALLINT        NOT NULL DEFAULT 0
                                CONSTRAINT chk_tran_cat_code CHECK (transaction_cat_code BETWEEN 0 AND 9999),

    -- TRAN-SOURCE  PIC X(10)
    -- Java: String transactionSource  (@Column name="transaction_source", length=10)
    transaction_source      VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-DESC  PIC X(100)
    -- Java: String transactionDesc  (@Column name="transaction_desc", length=100)
    transaction_desc        VARCHAR(100)    NOT NULL DEFAULT '',

    -- TRAN-AMT  PIC S9(09)V99  — signed: 9 integral digits + 2 decimal
    -- Java: BigDecimal transactionAmount  (@Column name="transaction_amount", precision=11, scale=2)
    transaction_amount      NUMERIC(11, 2)  NOT NULL DEFAULT 0.00,

    -- TRAN-MERCHANT-ID  PIC 9(09)  — up to 9-digit merchant identifier
    -- Java: Integer merchantId  (@Column name="merchant_id")
    merchant_id             INTEGER         NOT NULL DEFAULT 0,

    -- TRAN-MERCHANT-NAME  PIC X(50)
    -- Java: String merchantName  (@Column name="merchant_name", length=50)
    merchant_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-CITY  PIC X(50)
    -- Java: String merchantCity  (@Column name="merchant_city", length=50)
    merchant_city           VARCHAR(50)     NOT NULL DEFAULT '',

    -- TRAN-MERCHANT-ZIP  PIC X(10)
    -- Java: String merchantZip  (@Column name="merchant_zip", length=10)
    merchant_zip            VARCHAR(10)     NOT NULL DEFAULT '',

    -- TRAN-CARD-NUM  PIC X(16)  — FK to cards.card_number
    -- Java: String cardNumber  (@Column name="card_number", length=16)
    card_number             VARCHAR(16)     NOT NULL,

    -- TRAN-ORIG-TS  PIC X(26)  — origination timestamp (ISO-8601 with microseconds)
    -- Java: String origTimestamp  (@Column name="orig_timestamp", length=26)
    orig_timestamp          VARCHAR(26)     NOT NULL DEFAULT '',

    -- TRAN-PROC-TS  PIC X(26)  — processing timestamp
    -- Java: String procTimestamp  (@Column name="proc_timestamp", length=26)
    proc_timestamp          VARCHAR(26)     NOT NULL DEFAULT '',

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

-- Index for type-code filtering
CREATE INDEX IF NOT EXISTS idx_transactions_type_code
    ON transactions (transaction_type_code);

-- Index for merchant analytics
CREATE INDEX IF NOT EXISTS idx_transactions_merchant_id
    ON transactions (merchant_id);

-- Index for date-range queries using orig_timestamp prefix
CREATE INDEX IF NOT EXISTS idx_transactions_orig_ts
    ON transactions (orig_timestamp);

COMMENT ON TABLE  transactions                        IS 'Transaction ledger — migrated from VSAM KSDS TRANSACT (CVTRA05Y)';
COMMENT ON COLUMN transactions.transaction_id         IS 'TRAN-ID PIC X(16) — VSAM KSDS primary key; Java: String transactionId VARCHAR(16)';
COMMENT ON COLUMN transactions.transaction_type_code  IS 'TRAN-TYPE-CD PIC X(02); Java: String transactionTypeCode CHAR(2)';
COMMENT ON COLUMN transactions.transaction_cat_code   IS 'TRAN-CAT-CD PIC 9(04); Java: Short transactionCatCode SMALLINT';
COMMENT ON COLUMN transactions.transaction_source     IS 'TRAN-SOURCE PIC X(10); Java: String transactionSource VARCHAR(10)';
COMMENT ON COLUMN transactions.transaction_desc       IS 'TRAN-DESC PIC X(100); Java: String transactionDesc VARCHAR(100)';
COMMENT ON COLUMN transactions.transaction_amount     IS 'TRAN-AMT PIC S9(09)V99; Java: BigDecimal transactionAmount NUMERIC(11,2)';
COMMENT ON COLUMN transactions.merchant_id            IS 'TRAN-MERCHANT-ID PIC 9(09); Java: Integer merchantId';
COMMENT ON COLUMN transactions.merchant_name          IS 'TRAN-MERCHANT-NAME PIC X(50); Java: String merchantName VARCHAR(50)';
COMMENT ON COLUMN transactions.merchant_city          IS 'TRAN-MERCHANT-CITY PIC X(50); Java: String merchantCity VARCHAR(50)';
COMMENT ON COLUMN transactions.merchant_zip           IS 'TRAN-MERCHANT-ZIP PIC X(10); Java: String merchantZip VARCHAR(10)';
COMMENT ON COLUMN transactions.card_number            IS 'TRAN-CARD-NUM PIC X(16) — FK to cards.card_number; Java: String cardNumber VARCHAR(16)';
COMMENT ON COLUMN transactions.orig_timestamp         IS 'TRAN-ORIG-TS PIC X(26) — origination timestamp; Java: String origTimestamp VARCHAR(26)';
COMMENT ON COLUMN transactions.proc_timestamp         IS 'TRAN-PROC-TS PIC X(26) — processing timestamp; Java: String procTimestamp VARCHAR(26)';
