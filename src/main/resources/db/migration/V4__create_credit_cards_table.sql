-- V4__create_credit_cards_table.sql
CREATE SEQUENCE IF NOT EXISTS card_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS credit_cards (
    id           BIGINT PRIMARY KEY DEFAULT nextval('card_seq'),
    card_number  VARCHAR(16) UNIQUE NOT NULL,
    account_id   BIGINT NOT NULL REFERENCES accounts(id),
    card_type    VARCHAR(20),
    expiry_date  VARCHAR(7),
    cvv          VARCHAR(4),
    status       VARCHAR(20) DEFAULT 'ACTIVE',
    issued_date  TIMESTAMP,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE INDEX idx_credit_cards_account_id ON credit_cards(account_id);
