-- =============================================================================
-- V5__create_users_table.sql
-- Flyway migration: Create 'users' table
--
-- Source: Application security model (COBOL USRSEC / sign-on program COSGN00C)
-- No direct VSAM KSDS copybook — derived from CardDemo security requirements
-- and standard Spring Security UserDetails contract.
--
-- COBOL → PostgreSQL type mapping:
--   User ID (PIC X(08) in COBOL sign-on screens) → VARCHAR(8)  (user_id — PK)
--   Password (hashed, no length from COBOL)       → VARCHAR(60) (BCrypt hash)
--   Role (admin/regular user)                      → VARCHAR(10)
--   Status (active/inactive)                        → CHAR(1)
--   Name fields                                     → VARCHAR(25) (matching CVCUS01Y)
--
-- Java entity: com.carddemo.model.User  (Spring Security UserDetails)
--   String  ← VARCHAR/CHAR
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    -- User login identifier (matches COBOL sign-on screen field USER-ID PIC X(08))
    user_id                 VARCHAR(8)      NOT NULL,

    -- Application username / login name (may differ from user_id)
    username                VARCHAR(50)     NOT NULL,

    -- BCrypt-hashed password (BCrypt hash is always 60 characters)
    password_hash           VARCHAR(60)     NOT NULL,

    -- Role: 'ADMIN' or 'USER' (maps to COBOL USRTYP-CD)
    role                    VARCHAR(10)     NOT NULL DEFAULT 'USER'
                                CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER')),

    -- Account status flag: Y=active, N=inactive/locked
    status                  CHAR(1)         NOT NULL DEFAULT 'Y'
                                CONSTRAINT chk_users_status CHECK (status IN ('Y', 'N')),

    -- First name — VARCHAR(25) matching CUST-FIRST-NAME PIC X(25) in CVCUS01Y
    first_name              VARCHAR(25)     NOT NULL DEFAULT '',

    -- Last name — VARCHAR(25) matching CUST-LAST-NAME PIC X(25) in CVCUS01Y
    last_name               VARCHAR(25)     NOT NULL DEFAULT '',

    -- Audit columns
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY
    CONSTRAINT pk_users PRIMARY KEY (user_id),

    -- username must be unique (login uniqueness)
    CONSTRAINT uq_users_username UNIQUE (username)
);

-- Index to support role-based queries (e.g., list all admins)
CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

-- Index to support status filtering (e.g., active users only)
CREATE INDEX IF NOT EXISTS idx_users_status
    ON users (status);

COMMENT ON TABLE  users               IS 'Application user accounts for CardDemo authentication and authorisation';
COMMENT ON COLUMN users.user_id       IS 'User login identifier — matches COBOL USER-ID PIC X(08)';
COMMENT ON COLUMN users.username      IS 'Display username — unique login name';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password (60-character hash)';
COMMENT ON COLUMN users.role          IS 'Application role: ADMIN or USER — maps to COBOL USRTYP-CD';
COMMENT ON COLUMN users.status        IS 'Account status: Y=active, N=inactive/locked';
COMMENT ON COLUMN users.first_name    IS 'First name — VARCHAR(25) matching CUST-FIRST-NAME PIC X(25)';
COMMENT ON COLUMN users.last_name     IS 'Last name — VARCHAR(25) matching CUST-LAST-NAME PIC X(25)';
