-- =============================================================================
-- V5__create_users_table.sql
-- Flyway DDL migration for the USERS table
-- Source: COBOL security/user file (no direct copybook; modelled for Spring
--         Security integration in the modernised Java application)
--
-- Column type rationale:
--   user_id         BIGINT          – surrogate PK (auto-increment)
--   username        VARCHAR(50)     – login name, unique, not null
--   password_hash   VARCHAR(256)    – bcrypt/argon2 hash (min 60 chars)
--   role            VARCHAR(20)     – e.g. ROLE_USER, ROLE_ADMIN
--   status          CHAR(1)         – 'A'=active, 'I'=inactive, 'L'=locked
--   first_name      VARCHAR(25)     – matches PIC X(25) in CVCUS01Y
--   last_name       VARCHAR(25)     – matches PIC X(25) in CVCUS01Y
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id             BIGSERIAL       NOT NULL,
    username            VARCHAR(50)     NOT NULL,
    password_hash       VARCHAR(256)    NOT NULL,
    role                VARCHAR(20)     NOT NULL DEFAULT 'ROLE_USER',
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    first_name          VARCHAR(25)     NOT NULL DEFAULT '',
    last_name           VARCHAR(25)     NOT NULL DEFAULT '',

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_status CHECK (status IN ('A', 'I', 'L')),
    CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MANAGER'))
);

-- Indexes
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_status   ON users (status);
CREATE INDEX idx_users_role     ON users (role);

COMMENT ON TABLE  users IS 'Application users table for Spring Security authentication';
COMMENT ON COLUMN users.user_id        IS 'Auto-generated surrogate primary key';
COMMENT ON COLUMN users.username       IS 'Unique login name (max 50 chars)';
COMMENT ON COLUMN users.password_hash  IS 'Bcrypt/Argon2 encoded password hash';
COMMENT ON COLUMN users.role           IS 'Spring Security role: ROLE_USER, ROLE_ADMIN, ROLE_MANAGER';
COMMENT ON COLUMN users.status         IS 'A=active, I=inactive, L=locked';
COMMENT ON COLUMN users.first_name     IS 'User first name (PIC X(25) width, matches CVCUS01Y)';
COMMENT ON COLUMN users.last_name      IS 'User last name (PIC X(25) width, matches CVCUS01Y)';
