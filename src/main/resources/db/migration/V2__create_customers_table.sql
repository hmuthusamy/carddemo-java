-- V2__create_customers_table.sql
CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT PRIMARY KEY DEFAULT nextval('customer_seq'),
    first_name    VARCHAR(50) NOT NULL,
    last_name     VARCHAR(50) NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    phone         VARCHAR(20),
    address       VARCHAR(255),
    city          VARCHAR(50),
    state_code    VARCHAR(2),
    zip_code      VARCHAR(10),
    credit_score  INTEGER,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);
