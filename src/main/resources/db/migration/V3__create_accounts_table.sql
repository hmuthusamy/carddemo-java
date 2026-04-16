-- V3__create_accounts_table.sql
CREATE SEQUENCE IF NOT EXISTS account_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS accounts (
    id               BIGINT PRIMARY KEY DEFAULT nextval('account_seq'),
    account_number   VARCHAR(20) UNIQUE NOT NULL,
    customer_id      BIGINT NOT NULL REFERENCES customers(id),
    credit_limit     DECIMAL(12,2),
    current_balance  DECIMAL(12,2) DEFAULT 0.00,
    available_credit DECIMAL(12,2),
    account_type     VARCHAR(20),
    status           VARCHAR(20) DEFAULT 'ACTIVE',
    open_date        TIMESTAMP,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
