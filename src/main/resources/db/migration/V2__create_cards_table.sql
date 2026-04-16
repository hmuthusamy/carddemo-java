-- =============================================================================
-- V2__create_cards_table.sql
-- Flyway DDL migration for the CARDS table
-- Source COBOL copybook: CVACT02Y.cpy (CARD-RECORD, RECLN=150)
--
-- COBOL → PostgreSQL type mapping:
--   PIC X(16)         → VARCHAR(16)   (card number, alphanumeric)
--   PIC 9(11)         → BIGINT        (account id, 11-digit numeric)
--   PIC 9(03)         → SMALLINT      (CVV code, 3-digit)
--   PIC X(50)         → VARCHAR(50)   (embossed name)
--   PIC X(10)         → VARCHAR(10)   (expiration date)
--   PIC X(01)         → CHAR(1)       (active status)
-- =============================================================================

CREATE TABLE IF NOT EXISTS cards (
    -- CARD-NUM  PIC X(16)  → primary key (16-character card number)
    card_number             VARCHAR(16)     NOT NULL,

    -- CARD-ACCT-ID  PIC 9(11)  → FK to accounts
    account_id              BIGINT          NOT NULL,

    -- CARD-CVV-CD  PIC 9(03)  → 3-digit CVV
    cvv_code                SMALLINT        NOT NULL,

    -- CARD-EMBOSSED-NAME  PIC X(50)
    embossed_name           VARCHAR(50)     NOT NULL DEFAULT '',

    -- CARD-EXPIRAION-DATE  PIC X(10)  (COBOL typo preserved)
    expiration_date         VARCHAR(10)     NOT NULL DEFAULT '',

    -- CARD-ACTIVE-STATUS  PIC X(01)
    active_status           CHAR(1)         NOT NULL DEFAULT 'Y',

    CONSTRAINT pk_cards PRIMARY KEY (card_number),
    CONSTRAINT fk_cards_account_id FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_cards_active_status CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_cards_cvv_range CHECK (cvv_code BETWEEN 0 AND 999)
);

-- Indexes
CREATE INDEX idx_cards_account_id    ON cards (account_id);
CREATE INDEX idx_cards_active_status ON cards (active_status);

COMMENT ON TABLE  cards IS 'Credit card records migrated from VSAM KSDS CARDDAT (CVACT02Y.cpy)';
COMMENT ON COLUMN cards.card_number      IS 'CARD-NUM PIC X(16) – 16-character card number (primary key)';
COMMENT ON COLUMN cards.account_id       IS 'CARD-ACCT-ID PIC 9(11) – FK to accounts';
COMMENT ON COLUMN cards.cvv_code         IS 'CARD-CVV-CD PIC 9(03) – 3-digit card verification value';
COMMENT ON COLUMN cards.embossed_name    IS 'CARD-EMBOSSED-NAME PIC X(50)';
COMMENT ON COLUMN cards.expiration_date  IS 'CARD-EXPIRAION-DATE PIC X(10) – ISO date string YYYY-MM-DD';
COMMENT ON COLUMN cards.active_status    IS 'CARD-ACTIVE-STATUS PIC X(01) – Y=active, N=inactive';
