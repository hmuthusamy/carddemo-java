-- V1__create_users_table.sql
CREATE SEQUENCE IF NOT EXISTS user_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT PRIMARY KEY DEFAULT nextval('user_seq'),
    username      VARCHAR(50) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,
    first_name    VARCHAR(50),
    last_name     VARCHAR(50),
    role          VARCHAR(20) NOT NULL DEFAULT 'USER',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

-- Insert default admin user (password: admin123)
-- Hash generated with BCryptPasswordEncoder.encode("admin123")
INSERT INTO users (username, password, first_name, last_name, role, active, created_at, updated_at)
VALUES ('admin', '$2a$10$7QdtHqJlDBKKa/7SzgZ1.OlP.fZEZTPG2F/ZonS1aWaznodshswsa',
        'Admin', 'User', 'ADMIN', TRUE, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;
