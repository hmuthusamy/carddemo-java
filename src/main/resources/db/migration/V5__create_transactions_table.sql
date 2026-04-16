-- V5__create_transactions_table.sql
CREATE SEQUENCE IF NOT EXISTS transaction_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS transactions (
    id                  BIGINT PRIMARY KEY DEFAULT nextval('transaction_seq'),
    transaction_id      VARCHAR(36) UNIQUE NOT NULL,
    account_id          BIGINT NOT NULL REFERENCES accounts(id),
    amount              DECIMAL(12,2) NOT NULL,
    transaction_type    VARCHAR(20) NOT NULL,
    description         VARCHAR(255),
    merchant_name       VARCHAR(100),
    merchant_category   VARCHAR(50),
    status              VARCHAR(20) DEFAULT 'COMPLETED',
    transaction_date    TIMESTAMP,
    created_at          TIMESTAMP
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
