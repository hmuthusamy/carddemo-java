-- ============================================================
-- Flyway Migration: V1__create_account_data.sql
--
-- Migrated from COBOL copybook ACCTDATA.CPY / VSAM file ACCTDAT
-- used by CICS program COACTUPD (Account Update).
--
-- COBOL field → Column mapping:
--   ACCT-ID               PIC 9(11)         → acct_id      VARCHAR(11) PK
--   ACCT-ACTIVE-STATUS    PIC X(1)          → acct_active_status  CHAR(1)
--   ACCT-CURR-BAL         PIC S9(10)V99     → acct_curr_bal       NUMERIC(12,2)
--   ACCT-CREDIT-LIMIT     PIC S9(10)V99     → acct_credit_limit   NUMERIC(12,2)
--   ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99   → acct_cash_credit_limit NUMERIC(12,2)
--   ACCT-OPEN-DATE        PIC X(10)         → acct_open_date      DATE
--   ACCT-EXPIRAION-DATE   PIC X(10)         → acct_expiration_date DATE
--   ACCT-REISSUE-DATE     PIC X(10)         → acct_reissue_date   DATE
--   ACCT-CURR-CYC-CREDIT  PIC S9(10)V99     → acct_curr_cyc_credit NUMERIC(12,2)
--   ACCT-CURR-CYC-DEBIT   PIC S9(10)V99     → acct_curr_cyc_debit  NUMERIC(12,2)
--   ACCT-ADDR-ZIP         PIC X(10)         → acct_addr_zip       VARCHAR(10)
--   ACCT-GROUP-ID         PIC X(10)         → acct_group_id       VARCHAR(10)
-- ============================================================

CREATE TABLE IF NOT EXISTS account_data (
    acct_id                 VARCHAR(11)     NOT NULL,
    acct_active_status      CHAR(1)         DEFAULT 'Y',
    acct_curr_bal           NUMERIC(12, 2)  DEFAULT 0.00,
    acct_credit_limit       NUMERIC(12, 2)  DEFAULT 0.00,
    acct_cash_credit_limit  NUMERIC(12, 2)  DEFAULT 0.00,
    acct_open_date          DATE,
    acct_expiration_date    DATE,
    acct_reissue_date       DATE,
    acct_curr_cyc_credit    NUMERIC(12, 2)  DEFAULT 0.00,
    acct_curr_cyc_debit     NUMERIC(12, 2)  DEFAULT 0.00,
    acct_addr_zip           VARCHAR(10),
    acct_group_id           VARCHAR(10),
    CONSTRAINT pk_account_data PRIMARY KEY (acct_id),
    CONSTRAINT chk_active_status CHECK (acct_active_status IN ('Y', 'N'))
);

-- Seed data for integration / smoke tests
INSERT INTO account_data (acct_id, acct_active_status, acct_curr_bal,
                          acct_credit_limit, acct_cash_credit_limit,
                          acct_open_date, acct_expiration_date, acct_reissue_date,
                          acct_curr_cyc_credit, acct_curr_cyc_debit,
                          acct_addr_zip, acct_group_id)
VALUES ('00000001234', 'Y', 1500.00, 5000.00, 1000.00,
        '2020-01-01', '2026-12-31', '2024-01-01',
        200.00, 150.00, '10001', 'GOLD');
