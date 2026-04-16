-- =============================================================================
-- V6__create_cross_reference_table.sql
-- Flyway migration: Create 'card_account_xref' table
--
-- Source COBOL copybook: CVACT03Y.cpy  (CARD-XREF-RECORD, RECLN 50)
-- VSAM KSDS primary key: XREF-CARD-NUM
-- Purpose: Links card numbers to their owning customer and account,
--          preserving the VSAM alternate-index relationship in CARDXREF.
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.CardAccountXref):
--   XREF-CARD-NUM  PIC X(16)  → VARCHAR(16)  → String   cardNumber   (PK & FK→cards)
--   XREF-CUST-ID   PIC 9(09)  → INTEGER      → Integer  customerId   (FK→customers)
--   XREF-ACCT-ID   PIC 9(11)  → BIGINT       → Long     accountId    (FK→accounts)
-- =============================================================================

CREATE TABLE IF NOT EXISTS card_account_xref (
    -- XREF-CARD-NUM  PIC X(16)  — VSAM KSDS primary key
    -- Java: @Id String cardNumber  (@Column name="card_number", length=16)
    card_number             VARCHAR(16)     NOT NULL,

    -- XREF-CUST-ID  PIC 9(09)  — customer associated with this card
    -- Java: Integer customerId  (@Column name="customer_id")
    customer_id             INTEGER         NOT NULL,

    -- XREF-ACCT-ID  PIC 9(11)  — account associated with this card
    -- Java: Long accountId  (@Column name="account_id")
    account_id              BIGINT          NOT NULL,

    -- Audit columns
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY  (VSAM KSDS key = XREF-CARD-NUM)
    CONSTRAINT pk_card_account_xref PRIMARY KEY (card_number),

    -- FOREIGN KEY  XREF-CARD-NUM → cards.card_number
    CONSTRAINT fk_xref_card_number
        FOREIGN KEY (card_number)
        REFERENCES cards (card_number)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    -- FOREIGN KEY  XREF-CUST-ID → customers.customer_id
    CONSTRAINT fk_xref_customer_id
        FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    -- FOREIGN KEY  XREF-ACCT-ID → accounts.account_id
    CONSTRAINT fk_xref_account_id
        FOREIGN KEY (account_id)
        REFERENCES accounts (account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Index to support account-based cross-reference lookups
CREATE INDEX IF NOT EXISTS idx_xref_account_id
    ON card_account_xref (account_id);

-- Index to support customer-based cross-reference lookups
CREATE INDEX IF NOT EXISTS idx_xref_customer_id
    ON card_account_xref (customer_id);

-- Composite index for the common three-way join pattern
CREATE INDEX IF NOT EXISTS idx_xref_customer_account
    ON card_account_xref (customer_id, account_id);

COMMENT ON TABLE  card_account_xref             IS 'Card-Customer-Account cross-reference — migrated from VSAM KSDS CARDXREF (CVACT03Y)';
COMMENT ON COLUMN card_account_xref.card_number IS 'XREF-CARD-NUM PIC X(16) — VSAM KSDS PK; FK to cards.card_number; Java: String cardNumber VARCHAR(16)';
COMMENT ON COLUMN card_account_xref.customer_id IS 'XREF-CUST-ID PIC 9(09) — FK to customers.customer_id; Java: Integer customerId';
COMMENT ON COLUMN card_account_xref.account_id  IS 'XREF-ACCT-ID PIC 9(11) — FK to accounts.account_id; Java: Long accountId';
