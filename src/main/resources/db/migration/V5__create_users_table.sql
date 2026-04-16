-- =============================================================================
-- V5__create_users_table.sql
-- Flyway migration: Create 'users' table
--
-- Source: CardDemo application security model (COSGN00C sign-on program)
-- No direct VSAM KSDS copybook — derived from application security requirements.
--
-- COBOL → PostgreSQL → Java entity type mapping (com.carddemo.model.User):
--   user_id       (auto-generated) → BIGSERIAL    → Long    userId     (PK, @GeneratedValue IDENTITY)
--   username      VARCHAR(50)      → VARCHAR(50)  → String  username   (UNIQUE)
--   password_hash VARCHAR(256)     → VARCHAR(256) → String  passwordHash
--   role          VARCHAR(20)      → VARCHAR(20)  → String  role
--   status        CHAR(1)          → CHAR(1)      → String  status
--   first_name    VARCHAR(25)      → VARCHAR(25)  → String  firstName  (matches CUST-FIRST-NAME PIC X(25))
--   last_name     VARCHAR(25)      → VARCHAR(25)  → String  lastName   (matches CUST-LAST-NAME PIC X(25))
--
-- Note: user_id is a BIGSERIAL surrogate PK (auto-generated identity), not from COBOL.
--       password_hash is VARCHAR(256) to accommodate BCrypt (60 chars) and Argon2 hashes.
--       role is VARCHAR(20) to hold Spring Security role strings (e.g., 'ADMIN', 'USER').
--       status A=active, I=inactive, L=locked (maps to COBOL USRTYP status codes).
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    -- Auto-generated surrogate PK (no corresponding COBOL KSDS key)
    -- Java: @Id @GeneratedValue(strategy=IDENTITY) Long userId
    user_id                 BIGSERIAL       NOT NULL,

    -- Unique application login name
    -- Java: String username  (@Column unique=true, length=50)
    username                VARCHAR(50)     NOT NULL,

    -- BCrypt or Argon2 password hash (BCrypt = 60 chars; Argon2 up to ~100 chars)
    -- Java: String passwordHash  (@Column name="password_hash", length=256)
    password_hash           VARCHAR(256)    NOT NULL,

    -- Spring Security role string — e.g., 'ADMIN', 'USER'
    -- Java: String role  (@Column name="role", length=20)
    role                    VARCHAR(20)     NOT NULL DEFAULT 'USER'
                                CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER')),

    -- Account status: A=active, I=inactive, L=locked
    -- Java: String status  (@Column name="status", length=1)
    status                  CHAR(1)         NOT NULL DEFAULT 'A'
                                CONSTRAINT chk_users_status CHECK (status IN ('A', 'I', 'L')),

    -- First name — VARCHAR(25) matching CUST-FIRST-NAME PIC X(25) from CVCUS01Y
    -- Java: String firstName  (@Column name="first_name", length=25)
    first_name              VARCHAR(25)     NOT NULL DEFAULT '',

    -- Last name — VARCHAR(25) matching CUST-LAST-NAME PIC X(25) from CVCUS01Y
    -- Java: String lastName  (@Column name="last_name", length=25)
    last_name               VARCHAR(25)     NOT NULL DEFAULT '',

    -- Audit columns
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- PRIMARY KEY
    CONSTRAINT pk_users PRIMARY KEY (user_id),

    -- username must be unique
    CONSTRAINT uq_users_username UNIQUE (username)
);

-- Index for role-based queries
CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

-- Index for status-based filtering (e.g., active users only)
CREATE INDEX IF NOT EXISTS idx_users_status
    ON users (status);

COMMENT ON TABLE  users               IS 'Application user accounts — CardDemo authentication and authorisation (COSGN00C)';
COMMENT ON COLUMN users.user_id       IS 'Auto-generated surrogate PK (BIGSERIAL); Java: Long userId @GeneratedValue(IDENTITY)';
COMMENT ON COLUMN users.username      IS 'Unique login name; Java: String username VARCHAR(50)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt/Argon2 encoded password; Java: String passwordHash VARCHAR(256)';
COMMENT ON COLUMN users.role          IS 'Spring Security role: ADMIN or USER; Java: String role VARCHAR(20)';
COMMENT ON COLUMN users.status        IS 'Account status: A=active, I=inactive, L=locked; Java: String status CHAR(1)';
COMMENT ON COLUMN users.first_name    IS 'User first name — matches CUST-FIRST-NAME PIC X(25); Java: String firstName VARCHAR(25)';
COMMENT ON COLUMN users.last_name     IS 'User last name — matches CUST-LAST-NAME PIC X(25); Java: String lastName VARCHAR(25)';
