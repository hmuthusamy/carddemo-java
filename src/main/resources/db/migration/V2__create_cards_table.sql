-- =============================================================================
-- V2__create_cards_table.sql
-- Flyway migration: Create 'cards' table
--
-- Source COBOL copybook: CVACT02Y.cpy  (CARD-RECORD, RECLN 150)
-- VSAM KSDS primary key: CARD-NUM
-- Foreign key link: CARD-ACCT-ID → accounts.account_id
--
-- COBOL → PostgreSQL type mapping:
--   PIC X(16)    → VARCHAR(16) / CHAR(16)  (card_number — PK)
--   PIC 9(11)    → BIGINT                  (card_acct_id — FK to accounts)
--   PIC 9(03)    → SMALLINT                (cvv code — 3-digit integer)
--   PIC X(50)    → VARCHAR(50)             (embossed name)
--   PIC X(10)    → VARCHAR(10)             (expiration date string)
--   PIC X(01)    → CHAR(1)                 (active status flag)
--
-- Java entity: com.carddemo.model.Card
--   String      ← VARCHAR/CHAR
--   Long        ← BIGINT
--   Integer     ← SMALLINT
-- =============================================================================

CREATE TABLE IF NOT EXISTS cards (
    -- CARD-NUM  PIC X(16)  — VSAM KSDS primary key (16-digit card number)
    card_number             CHAR(16)        NOT NULL,

    -- CARD-ACCT-ID  PIC 9(11)  — foreign key to accounts
    account_id              BIGINT          NOT NULL,

    -- CARD-CVV-CD  PIC 9(03)  — 3-digit card verification value
    cvv_code                SMALLINT        NOT NULL
                                CONSTRAINT chk_cards_cvv CHECK (cvv_code BETWEEN 0 AND 999),

    -- CARD-EMBOSSED-NAME  PIC X(50)
    embossed_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- CARD-EXPIRAION-DATE  PIC X(10)  (original COBOL spelling preserved)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- CARD-ACTIVE-STATUS  PIC X(01)
    active_status           CHAR(1)         NOT NULL DEFAULT 'Y'
                                CONSTRAINT chk_cards_active_status CHECK (active_status IN ('Y', 'N')),

    -- Audit columns
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = CARD-NUM)
    CONSTRAINT pk_cards PRIMARY KEY (card_number),

    -- FOREIGN KEY  CARD-ACCT-ID → accounts.account_id
    CONSTRAINT fk_cards_account_id
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Index for fast account-based card lookups
CREATE INDEX IF NOT EXISTS idx_cards_account_id
    ON cards (account_id);

-- Index for expiration date-based queries (e.g., renewal processing)
CREATE INDEX IF NOT EXISTS idx_cards_expiration_date
    ON cards (expiration_date);

COMMENT ON TABLE  cards                  IS 'Credit card master — migrated from VSAM KSDS CARDDAT (CVACT02Y)';
COMMENT ON COLUMN cards.card_number      IS 'CARD-NUM PIC X(16) — VSAM KSDS primary key; 16-digit card number';
COMMENT ON COLUMN cards.account_id       IS 'CARD-ACCT-ID PIC 9(11) — FK to accounts.account_id';
COMMENT ON COLUMN cards.cvv_code         IS 'CARD-CVV-CD PIC 9(03) — 3-digit card verification value';
COMMENT ON COLUMN cards.embossed_name    IS 'CARD-EMBOSSED-NAME PIC X(50) — name printed on card';
COMMENT ON COLUMN cards.expiration_date  IS 'CARD-EXPIRAION-DATE PIC X(10) — format YYYY-MM-DD';
COMMENT ON COLUMN cards.active_status    IS 'CARD-ACTIVE-STATUS PIC X(01) — Y=active, N=inactive';
