-- V6__create_statements_table.sql
CREATE SEQUENCE IF NOT EXISTS statement_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS statements (
    id               BIGINT PRIMARY KEY DEFAULT nextval('statement_seq'),
    account_id       BIGINT NOT NULL REFERENCES accounts(id),
    statement_date   DATE NOT NULL,
    opening_balance  DECIMAL(12,2),
    closing_balance  DECIMAL(12,2),
    total_charges    DECIMAL(12,2) DEFAULT 0.00,
    total_credits    DECIMAL(12,2) DEFAULT 0.00,
    minimum_payment  DECIMAL(12,2),
    due_date         DATE,
    status           VARCHAR(20) DEFAULT 'GENERATED',
    created_at       TIMESTAMP
);

CREATE INDEX idx_statements_account_id ON statements(account_id);
CREATE INDEX idx_statements_date ON statements(statement_date);
