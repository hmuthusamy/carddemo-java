-- =============================================================================
-- V6__create_cross_reference_table.sql
-- Flyway DDL migration for the CARD_ACCOUNT_XREF table
-- Source COBOL copybook: CVACT03Y.cpy (CARD-XREF-RECORD, RECLN=50)
--
-- COBOL → PostgreSQL type mapping:
--   PIC X(16)         → VARCHAR(16)   (xref_card_num – card number)
--   PIC 9(09)         → INTEGER       (xref_cust_id  – customer id)
--   PIC 9(11)         → BIGINT        (xref_acct_id  – account id)
--
-- Purpose: VSAM cross-reference file linking card numbers to customer and
--          account identifiers.  Separate from the cards table so that the
--          KSDS key structure (XREF-CARD-NUM as the primary VSAM key) is
--          preserved explicitly in the relational model.
-- =============================================================================

CREATE TABLE IF NOT EXISTS card_account_xref (
    -- XREF-CARD-NUM  PIC X(16)  → primary key (VSAM KSDS primary key)
    card_number             VARCHAR(16)     NOT NULL,

    -- XREF-CUST-ID  PIC 9(09)  → FK to customers
    customer_id             INTEGER         NOT NULL,

    -- XREF-ACCT-ID  PIC 9(11)  → FK to accounts
    account_id              BIGINT          NOT NULL,

    CONSTRAINT pk_card_account_xref PRIMARY KEY (card_number),
    CONSTRAINT fk_xref_card_number FOREIGN KEY (card_number)
        REFERENCES cards (card_number)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_xref_customer_id FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_xref_account_id FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Indexes
CREATE INDEX idx_xref_customer_id ON card_account_xref (customer_id);
CREATE INDEX idx_xref_account_id  ON card_account_xref (account_id);

COMMENT ON TABLE  card_account_xref IS 'Card–customer–account cross-reference migrated from VSAM KSDS CARDXREF (CVACT03Y.cpy)';
COMMENT ON COLUMN card_account_xref.card_number   IS 'XREF-CARD-NUM PIC X(16) – VSAM primary key / FK to cards';
COMMENT ON COLUMN card_account_xref.customer_id   IS 'XREF-CUST-ID PIC 9(09) – FK to customers';
COMMENT ON COLUMN card_account_xref.account_id    IS 'XREF-ACCT-ID PIC 9(11) – FK to accounts';
