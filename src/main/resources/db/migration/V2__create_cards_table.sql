-- =============================================================================
-- V2__create_cards_table.sql
-- Flyway migration: Create 'cards' table
--
-- Source COBOL copybook: CVACT02Y.cpy  (CARD-RECORD, RECLN 150)
-- VSAM KSDS primary key: CARD-NUM
-- Foreign key: CARD-ACCT-ID → accounts.account_id
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.Card):
--   CARD-NUM            PIC X(16)  → VARCHAR(16)  → String  cardNumber   (PK)
--   CARD-ACCT-ID        PIC 9(11)  → BIGINT       → Long    accountId    (FK)
--   CARD-CVV-CD         PIC 9(03)  → SMALLINT     → Short   cvvCode
--   CARD-EMBOSSED-NAME  PIC X(50)  → VARCHAR(50)  → String  embossedName
--   CARD-EXPIRAION-DATE PIC X(10)  → VARCHAR(10)  → String  expirationDate
--   CARD-ACTIVE-STATUS  PIC X(01)  → CHAR(1)      → String  activeStatus
-- =============================================================================

CREATE TABLE IF NOT EXISTS cards (
    -- CARD-NUM  PIC X(16)  — VSAM KSDS primary key (16-character card number)
    -- Java: @Id String cardNumber  (@Column length=16)
    card_number             VARCHAR(16)     NOT NULL,

    -- CARD-ACCT-ID  PIC 9(11)  — foreign key to accounts
    -- Java: Long accountId  (@Column name="account_id")
    account_id              BIGINT          NOT NULL,

    -- CARD-CVV-CD  PIC 9(03)  — 3-digit card verification value (0–999)
    -- Java: Short cvvCode  (@Column name="cvv_code")
    cvv_code                SMALLINT        NOT NULL
                                CONSTRAINT chk_cards_cvv CHECK (cvv_code BETWEEN 0 AND 999),

    -- CARD-EMBOSSED-NAME  PIC X(50)
    -- Java: String embossedName  (@Column name="embossed_name", length=50)
    embossed_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- CARD-EXPIRAION-DATE  PIC X(10)  — original COBOL spelling preserved
    -- Java: String expirationDate  (@Column name="expiration_date", length=10)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- CARD-ACTIVE-STATUS  PIC X(01)
    -- Java: String activeStatus  (@Column name="active_status", length=1)
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

COMMENT ON TABLE  cards                 IS 'Credit card master — migrated from VSAM KSDS CARDDAT (CVACT02Y)';
COMMENT ON COLUMN cards.card_number     IS 'CARD-NUM PIC X(16) — VSAM KSDS primary key; Java: String cardNumber VARCHAR(16)';
COMMENT ON COLUMN cards.account_id      IS 'CARD-ACCT-ID PIC 9(11) — FK to accounts.account_id; Java: Long accountId';
COMMENT ON COLUMN cards.cvv_code        IS 'CARD-CVV-CD PIC 9(03) — 3-digit CVV; Java: Short cvvCode SMALLINT';
COMMENT ON COLUMN cards.embossed_name   IS 'CARD-EMBOSSED-NAME PIC X(50); Java: String embossedName VARCHAR(50)';
COMMENT ON COLUMN cards.expiration_date IS 'CARD-EXPIRAION-DATE PIC X(10) — format YYYY-MM-DD; Java: String expirationDate';
COMMENT ON COLUMN cards.active_status   IS 'CARD-ACTIVE-STATUS PIC X(01) — Y=active, N=inactive; Java: String activeStatus';
